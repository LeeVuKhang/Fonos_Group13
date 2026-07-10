package com.example.fonos_group13.data.creator;

import android.content.Context;

import com.example.fonos_group13.data.core.FirebaseConfig;
import com.example.fonos_group13.data.core.RepositoryCallback;
import com.example.fonos_group13.data.core.Subscription;
import com.example.fonos_group13.data.firestore.CreatorUploadDocumentMapper;
import com.example.fonos_group13.data.firestore.FirestoreSubscription;
import com.example.fonos_group13.data.repository.CreatorUploadsRepository;
import com.example.fonos_group13.model.UserGeneratedAudiobook;
import com.example.fonos_group13.model.UserGeneratedChapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FirestoreCreatorUploadsRepository implements CreatorUploadsRepository {
    private static final String COLLECTION_BOOKS = "books";
    private static final String COLLECTION_CHAPTERS = "chapters";

    private final boolean configured;
    private final FirebaseFirestore firestore;
    private final SignedInUserProvider userProvider;

    public FirestoreCreatorUploadsRepository(Context context) {
        configured = FirebaseConfig.isConfigured(context);
        if (configured) {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            firestore = FirebaseFirestore.getInstance();
            userProvider = new FirebaseSignedInUserProvider(auth);
        } else {
            firestore = null;
            userProvider = () -> null;
        }
    }

    @Override
    public void getMyUploads(RepositoryCallback<List<UserGeneratedAudiobook>> callback) {
        String uid = currentUserUid();
        if (!canRead(uid, callback, "Please sign in to view your uploads.")) {
            return;
        }
        firestore.collection(COLLECTION_BOOKS)
                .whereEqualTo("creatorUid", uid)
                .whereEqualTo("createdByUser", true)
                .get()
                .addOnSuccessListener(snapshot -> callback.onSuccess(mapUploads(snapshot)))
                .addOnFailureListener(callback::onError);
    }

    @Override
    public Subscription observeMyUploads(RepositoryCallback<List<UserGeneratedAudiobook>> callback) {
        String uid = currentUserUid();
        if (!canRead(uid, callback, "Please sign in to view your uploads.")) {
            return Subscription.NONE;
        }
        return new FirestoreSubscription(firestore.collection(COLLECTION_BOOKS)
                .whereEqualTo("creatorUid", uid)
                .whereEqualTo("createdByUser", true)
                .addSnapshotListener((snapshot, exception) -> {
                    if (exception != null) {
                        callback.onError(exception);
                    } else {
                        callback.onSuccess(mapUploads(snapshot));
                    }
                }));
    }

    @Override
    public Subscription observeUploadChapters(
            String bookId,
            RepositoryCallback<List<UserGeneratedChapter>> callback
    ) {
        String uid = currentUserUid();
        if (!canRead(uid, callback, "Please sign in to view chapters.")) {
            return Subscription.NONE;
        }
        String safeBookId = trimToNull(bookId);
        if (safeBookId == null) {
            callback.onError(new IllegalArgumentException("Missing audiobook id."));
            return Subscription.NONE;
        }
        return new FirestoreSubscription(firestore.collection(COLLECTION_BOOKS)
                .document(safeBookId)
                .collection(COLLECTION_CHAPTERS)
                .addSnapshotListener((snapshot, exception) -> {
                    if (exception != null) {
                        callback.onError(exception);
                    } else {
                        callback.onSuccess(mapChapters(snapshot));
                    }
                }));
    }

    private boolean canRead(String uid, RepositoryCallback<?> callback, String signedOutMessage) {
        if (!configured || firestore == null) {
            callback.onError(FirebaseConfig.missingConfigException());
            return false;
        }
        if (uid == null) {
            callback.onError(new IllegalStateException(signedOutMessage));
            return false;
        }
        return true;
    }

    private List<UserGeneratedAudiobook> mapUploads(QuerySnapshot snapshot) {
        List<UserGeneratedAudiobook> uploads = new ArrayList<>();
        if (snapshot != null) {
            snapshot.getDocuments().forEach(document -> uploads.add(
                    CreatorUploadDocumentMapper.audiobook(document)
            ));
        }
        Collections.sort(uploads, (left, right) -> Long.compare(
                right.getSortTimestampMillis(),
                left.getSortTimestampMillis()
        ));
        return uploads;
    }

    private List<UserGeneratedChapter> mapChapters(QuerySnapshot snapshot) {
        List<UserGeneratedChapter> chapters = new ArrayList<>();
        if (snapshot != null) {
            snapshot.getDocuments().forEach(document -> {
                if (!CreatorUploadDocumentMapper.isDeletedChapter(document)) {
                    chapters.add(CreatorUploadDocumentMapper.chapter(document));
                }
            });
        }
        Collections.sort(chapters, (left, right) -> {
            int order = Integer.compare(left.getOrder(), right.getOrder());
            return order == 0 ? left.getTitle().compareToIgnoreCase(right.getTitle()) : order;
        });
        return chapters;
    }

    private String currentUserUid() {
        return trimToNull(userProvider.currentUid());
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
