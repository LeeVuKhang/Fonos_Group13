package com.example.fonos_group13.data.library;

import android.content.Context;
import android.util.Log;

import com.example.fonos_group13.data.core.FirebaseConfig;
import com.example.fonos_group13.data.core.RepositoryCallback;
import com.example.fonos_group13.data.firestore.FirestoreValueReader;
import com.example.fonos_group13.data.firestore.UserProgressDocumentMapper;
import com.example.fonos_group13.model.BookChapter;
import com.example.fonos_group13.model.ProgressKey;
import com.example.fonos_group13.model.UserProgress;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class ProgressRepository implements com.example.fonos_group13.data.repository.ProgressRepository {
    private static final String PROGRESS_KEY_SEPARATOR = "__";
    private static final String TAG = "ProgressRepository";

    private final boolean configured;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    public ProgressRepository(Context context) {
        configured = FirebaseConfig.isConfigured(context);
        if (configured) {
            auth = FirebaseAuth.getInstance();
            firestore = FirebaseFirestore.getInstance();
        }
    }

    public void getProgress(String bookId, RepositoryCallback<UserProgress> callback) {
        getProgress(bookId, BookChapter.LEGACY_CHAPTER_ID, new RepositoryCallback<UserProgress>() {
            @Override
            public void onSuccess(UserProgress progress) {
                if (progress.getPositionMs() > 0 || progress.getDurationMs() > 0 || progress.isCompleted()) {
                    callback.onSuccess(progress);
                    return;
                }
                getLegacyProgress(bookId, callback);
            }

            @Override
            public void onError(Exception exception) {
                callback.onError(exception);
            }
        });
    }

    public void getProgress(String bookId, String chapterId, RepositoryCallback<UserProgress> callback) {
        FirebaseUser user = auth == null ? null : auth.getCurrentUser();
        if (!configured || firestore == null || user == null) {
            callback.onSuccess(UserProgress.empty(bookId, chapterId));
            return;
        }

        firestore.collection("users")
                .document(user.getUid())
                .collection("progress")
                .document(progressDocumentId(bookId, chapterId))
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        callback.onSuccess(UserProgressDocumentMapper.fromDocument(bookId, chapterId, document));
                    } else if (BookChapter.LEGACY_CHAPTER_ID.equals(chapterId)) {
                        getLegacyProgress(bookId, callback);
                    } else {
                        callback.onSuccess(UserProgress.empty(bookId, chapterId));
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    @Override
    public void getAllProgress(RepositoryCallback<Map<ProgressKey, UserProgress>> callback) {
        FirebaseUser user = auth == null ? null : auth.getCurrentUser();
        if (!configured || firestore == null || user == null) {
            callback.onSuccess(new HashMap<>());
            return;
        }

        firestore.collection("users")
                .document(user.getUid())
                .collection("progress")
                .get()
                .addOnSuccessListener(snapshot -> {
                    Map<ProgressKey, UserProgress> progressByChapter = new HashMap<>();
                    snapshot.getDocuments().forEach(document -> {
                        String bookId = FirestoreValueReader.string(document, "bookId");
                        String chapterId = FirestoreValueReader.string(document, "chapterId");
                        if (bookId == null) {
                            String documentId = document.getId();
                            int separator = documentId.indexOf(PROGRESS_KEY_SEPARATOR);
                            bookId = separator < 0 ? documentId : documentId.substring(0, separator);
                        }
                        if (chapterId == null) {
                            chapterId = BookChapter.LEGACY_CHAPTER_ID;
                        }
                        UserProgress progress = UserProgressDocumentMapper.fromDocument(
                                bookId,
                                chapterId,
                                document
                        );
                        progressByChapter.put(ProgressKey.from(progress), progress);
                    });
                    callback.onSuccess(progressByChapter);
                })
                .addOnFailureListener(callback::onError);
    }

    public void saveProgress(String bookId, long positionMs, long durationMs) {
        saveProgress(bookId, BookChapter.LEGACY_CHAPTER_ID, positionMs, durationMs);
    }

    public void saveProgress(String bookId, String chapterId, long positionMs, long durationMs) {
        saveProgress(bookId, chapterId, positionMs, durationMs, new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
            }

            @Override
            public void onError(Exception exception) {
                Log.w(TAG, "Could not save playback progress.", exception);
            }
        });
    }

    @Override
    public void saveProgress(
            String bookId,
            String chapterId,
            long positionMs,
            long durationMs,
            RepositoryCallback<Void> callback
    ) {
        FirebaseUser user = auth == null ? null : auth.getCurrentUser();
        if (!configured || firestore == null || user == null || bookId == null || chapterId == null) {
            callback.onError(new IllegalStateException("Progress cannot be saved without a signed-in user."));
            return;
        }

        Map<String, Object> progress = new HashMap<>();
        progress.put("bookId", bookId);
        progress.put("chapterId", chapterId);
        progress.put("positionMs", Math.max(positionMs, 0));
        progress.put("durationMs", Math.max(durationMs, 0));
        progress.put("completed", ProgressCompletionPolicy.isCompleted(positionMs, durationMs));
        progress.put("updatedAt", FieldValue.serverTimestamp());

        firestore.collection("users")
                .document(user.getUid())
                .collection("progress")
                .document(progressDocumentId(bookId, chapterId))
                .set(progress, SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    public static String progressDocumentId(String bookId, String chapterId) {
        return safeKeyPart(bookId) + PROGRESS_KEY_SEPARATOR + safeKeyPart(chapterId);
    }

    private void getLegacyProgress(String bookId, RepositoryCallback<UserProgress> callback) {
        FirebaseUser user = auth == null ? null : auth.getCurrentUser();
        if (!configured || firestore == null || user == null) {
            callback.onSuccess(UserProgress.empty(bookId, BookChapter.LEGACY_CHAPTER_ID));
            return;
        }

        firestore.collection("users")
                .document(user.getUid())
                .collection("progress")
                .document(bookId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        callback.onSuccess(UserProgressDocumentMapper.fromDocument(
                                bookId,
                                BookChapter.LEGACY_CHAPTER_ID,
                                document
                        ));
                    } else {
                        callback.onSuccess(UserProgress.empty(bookId, BookChapter.LEGACY_CHAPTER_ID));
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    private static String safeKeyPart(String value) {
        return value == null ? "" : value.trim();
    }
}
