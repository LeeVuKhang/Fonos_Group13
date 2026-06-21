package com.example.fonos_group13.data;

import android.content.Context;

import com.example.fonos_group13.model.AudiobookGenerationStatus;
import com.example.fonos_group13.model.CreateAudiobookDraftInput;
import com.example.fonos_group13.model.UserGeneratedAudiobook;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreatorAudiobookRepository {
    private static final String COLLECTION_BOOKS = "books";
    private static final String COLLECTION_CHAPTERS = "chapters";
    private static final String DEFAULT_CHAPTER_ID = "chapter_1";

    private final boolean configured;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    public CreatorAudiobookRepository(Context context) {
        configured = FirebaseConfig.isConfigured(context);
        if (configured) {
            auth = FirebaseAuth.getInstance();
            firestore = FirebaseFirestore.getInstance();
        }
    }

    public void createDraft(CreateAudiobookDraftInput input, RepositoryCallback<String> callback) {
        FirebaseUser user = currentUser();
        if (!canWrite(user, callback)) {
            return;
        }
        if (!isValidInput(input, callback)) {
            return;
        }

        String bookId = firestore.collection(COLLECTION_BOOKS).document().getId();
        saveDraft(bookId, user, input, AudiobookGenerationStatus.DRAFT, callback);
    }

    public void createDraftAndRequestGeneration(CreateAudiobookDraftInput input, RepositoryCallback<String> callback) {
        FirebaseUser user = currentUser();
        if (!canWrite(user, callback)) {
            return;
        }
        if (!isValidInput(input, callback)) {
            return;
        }

        String bookId = firestore.collection(COLLECTION_BOOKS).document().getId();
        saveDraft(bookId, user, input, AudiobookGenerationStatus.PENDING_GENERATION, callback);
    }

    public void getMyUploads(RepositoryCallback<List<UserGeneratedAudiobook>> callback) {
        FirebaseUser user = currentUser();
        if (!configured || firestore == null) {
            callback.onError(FirebaseConfig.missingConfigException());
            return;
        }
        if (user == null) {
            callback.onError(new IllegalStateException("Please sign in to view your uploads."));
            return;
        }

        firestore.collection(COLLECTION_BOOKS)
                .whereEqualTo("creatorUid", user.getUid())
                .whereEqualTo("createdByUser", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<UserGeneratedAudiobook> uploads = new ArrayList<>();
                    querySnapshot.getDocuments().forEach(document -> uploads.add(UserGeneratedAudiobook.fromDocument(document)));
                    Collections.sort(uploads, (left, right) -> Long.compare(
                            right.getSortTimestampMillis(),
                            left.getSortTimestampMillis()
                    ));
                    callback.onSuccess(uploads);
                })
                .addOnFailureListener(callback::onError);
    }

    public void requestGeneration(String bookId, RepositoryCallback<Void> callback) {
        FirebaseUser user = currentUser();
        if (!canWrite(user, callback)) {
            return;
        }
        String safeBookId = trimToNull(bookId);
        if (safeBookId == null) {
            callback.onError(new IllegalArgumentException("Missing audiobook id."));
            return;
        }

        firestore.collection(COLLECTION_BOOKS)
                .document(safeBookId)
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        callback.onError(new IllegalArgumentException("Audiobook not found."));
                        return;
                    }
                    String creatorUid = document.getString("creatorUid");
                    if (!user.getUid().equals(creatorUid)) {
                        callback.onError(new IllegalStateException("You can only update your own uploads."));
                        return;
                    }
                    AudiobookGenerationStatus status = AudiobookGenerationStatus.fromValue(document.getString("generationStatus"));
                    if (status != AudiobookGenerationStatus.DRAFT && status != AudiobookGenerationStatus.FAILED) {
                        callback.onError(new IllegalStateException("This audiobook is not ready for generation."));
                        return;
                    }
                    markPendingGeneration(safeBookId, callback);
                })
                .addOnFailureListener(callback::onError);
    }

    private void saveDraft(
            String bookId,
            FirebaseUser user,
            CreateAudiobookDraftInput input,
            AudiobookGenerationStatus status,
            RepositoryCallback<String> callback
    ) {
        Map<String, Object> bookData = new HashMap<>();
        bookData.put("creatorUid", user.getUid());
        bookData.put("createdByUser", true);
        bookData.put("sourceType", "user_text");
        bookData.put("generationStatus", status.getValue());
        bookData.put("reviewStatus", "pending");
        bookData.put("published", false);
        bookData.put("title", input.getTitle());
        bookData.put("author", input.getAuthor());
        bookData.put("coverUrl", input.getCoverUrl());
        bookData.put("chapterTitle", input.getChapterTitle());
        bookData.put("contentSample", contentSample(input.getChapterText()));
        bookData.put("languageCode", input.getLanguageCode());
        bookData.put("voiceGender", input.getVoiceOption().getGender());
        bookData.put("pollyVoiceId", input.getVoiceOption().getVoiceId());
        bookData.put("updatedAt", FieldValue.serverTimestamp());
        bookData.put("createdAt", FieldValue.serverTimestamp());

        Map<String, Object> chapterData = new HashMap<>();
        chapterData.put("title", input.getChapterTitle());
        chapterData.put("chapterTitle", input.getChapterTitle());
        chapterData.put("sourceText", input.getChapterText());
        chapterData.put("contentSample", contentSample(input.getChapterText()));
        chapterData.put("order", 0);
        chapterData.put("published", false);
        chapterData.put("generationStatus", status.getValue());
        chapterData.put("pollyVoiceId", input.getVoiceOption().getVoiceId());
        chapterData.put("voiceGender", input.getVoiceOption().getGender());
        chapterData.put("updatedAt", FieldValue.serverTimestamp());
        chapterData.put("createdAt", FieldValue.serverTimestamp());

        WriteBatch batch = firestore.batch();
        batch.set(firestore.collection(COLLECTION_BOOKS).document(bookId), bookData, SetOptions.merge());
        batch.set(
                firestore.collection(COLLECTION_BOOKS).document(bookId)
                        .collection(COLLECTION_CHAPTERS)
                        .document(DEFAULT_CHAPTER_ID),
                chapterData,
                SetOptions.merge()
        );
        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess(bookId))
                .addOnFailureListener(callback::onError);
    }

    private void markPendingGeneration(String bookId, RepositoryCallback<Void> callback) {
        Map<String, Object> bookData = new HashMap<>();
        bookData.put("generationStatus", AudiobookGenerationStatus.PENDING_GENERATION.getValue());
        bookData.put("reviewStatus", "pending");
        bookData.put("generationError", null);
        bookData.put("updatedAt", FieldValue.serverTimestamp());

        Map<String, Object> chapterData = new HashMap<>();
        chapterData.put("generationStatus", AudiobookGenerationStatus.PENDING_GENERATION.getValue());
        chapterData.put("generationError", null);
        chapterData.put("updatedAt", FieldValue.serverTimestamp());

        WriteBatch batch = firestore.batch();
        batch.set(firestore.collection(COLLECTION_BOOKS).document(bookId), bookData, SetOptions.merge());
        batch.set(
                firestore.collection(COLLECTION_BOOKS).document(bookId)
                        .collection(COLLECTION_CHAPTERS)
                        .document(DEFAULT_CHAPTER_ID),
                chapterData,
                SetOptions.merge()
        );
        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    private boolean canWrite(FirebaseUser user, RepositoryCallback<?> callback) {
        if (!configured || firestore == null) {
            callback.onError(FirebaseConfig.missingConfigException());
            return false;
        }
        if (user == null) {
            callback.onError(new IllegalStateException("Please sign in to create audiobooks."));
            return false;
        }
        return true;
    }

    private boolean isValidInput(CreateAudiobookDraftInput input, RepositoryCallback<String> callback) {
        if (input == null) {
            callback.onError(new IllegalArgumentException("Missing audiobook draft."));
            return false;
        }
        if (isBlank(input.getTitle())) {
            callback.onError(new IllegalArgumentException("Title is required."));
            return false;
        }
        if (isBlank(input.getAuthor())) {
            callback.onError(new IllegalArgumentException("Author is required."));
            return false;
        }
        if (isBlank(input.getChapterText())) {
            callback.onError(new IllegalArgumentException("Chapter text is required."));
            return false;
        }
        return true;
    }

    private FirebaseUser currentUser() {
        return auth == null ? null : auth.getCurrentUser();
    }

    private String contentSample(String sourceText) {
        String trimmed = trimToNull(sourceText);
        if (trimmed == null) {
            return "";
        }
        return trimmed.length() <= 180 ? trimmed : trimmed.substring(0, 180);
    }

    private boolean isBlank(String value) {
        return trimToNull(value) == null;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
