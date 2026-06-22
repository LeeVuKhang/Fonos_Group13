package com.example.fonos_group13.data;

import android.content.Context;

import com.example.fonos_group13.BuildConfig;
import com.example.fonos_group13.model.CreateAudiobookDraftInput;
import com.example.fonos_group13.model.UserGeneratedAudiobook;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CreatorAudiobookRepository {
    private static final String COLLECTION_BOOKS = "books";

    private final boolean configured;
    private final FirebaseFirestore firestore;
    private final SignedInUserProvider userProvider;
    private final CreatorBackendDataSource backendApi;

    public CreatorAudiobookRepository(Context context) {
        configured = FirebaseConfig.isConfigured(context);
        FirebaseFirestore resolvedFirestore = null;
        SignedInUserProvider resolvedUserProvider = () -> null;
        CreatorBackendDataSource resolvedBackendApi = null;
        if (configured) {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            resolvedFirestore = FirebaseFirestore.getInstance();
            resolvedUserProvider = new FirebaseSignedInUserProvider(auth);
            resolvedBackendApi = new CreatorApiClient(BuildConfig.BACKEND_BASE_URL, auth);
        }
        firestore = resolvedFirestore;
        userProvider = resolvedUserProvider;
        backendApi = resolvedBackendApi;
    }

    CreatorAudiobookRepository(
            boolean configured,
            FirebaseFirestore firestore,
            SignedInUserProvider userProvider,
            CreatorBackendDataSource backendApi
    ) {
        this.configured = configured;
        this.firestore = firestore;
        this.userProvider = userProvider;
        this.backendApi = backendApi;
    }

    public void createDraft(CreateAudiobookDraftInput input, RepositoryCallback<String> callback) {
        if (!canWrite(callback)) {
            return;
        }
        if (!isValidInput(input, callback)) {
            return;
        }
        backendApi.createDraft(input, callback);
    }

    public void createDraftAndRequestGeneration(CreateAudiobookDraftInput input, RepositoryCallback<String> callback) {
        if (!canWrite(callback)) {
            return;
        }
        if (!isValidInput(input, callback)) {
            return;
        }
        backendApi.createDraft(input, new RepositoryCallback<String>() {
            @Override
            public void onSuccess(String bookId) {
                backendApi.requestGeneration(bookId, new RepositoryCallback<Void>() {
                    @Override
                    public void onSuccess(Void data) {
                        callback.onSuccess(bookId);
                    }

                    @Override
                    public void onError(Exception exception) {
                        callback.onError(new DraftSavedGenerationRequestException(bookId, exception));
                    }
                });
            }

            @Override
            public void onError(Exception exception) {
                callback.onError(exception);
            }
        });
    }

    public void getMyUploads(RepositoryCallback<List<UserGeneratedAudiobook>> callback) {
        String uid = currentUserUid();
        if (!configured || firestore == null) {
            callback.onError(FirebaseConfig.missingConfigException());
            return;
        }
        if (uid == null) {
            callback.onError(new IllegalStateException("Please sign in to view your uploads."));
            return;
        }

        firestore.collection(COLLECTION_BOOKS)
                .whereEqualTo("creatorUid", uid)
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
        if (!canWrite(callback)) {
            return;
        }
        String safeBookId = trimToNull(bookId);
        if (safeBookId == null) {
            callback.onError(new IllegalArgumentException("Missing audiobook id."));
            return;
        }
        backendApi.requestGeneration(safeBookId, callback);
    }

    private boolean canWrite(RepositoryCallback<?> callback) {
        if (!configured || backendApi == null) {
            callback.onError(FirebaseConfig.missingConfigException());
            return false;
        }
        if (currentUserUid() == null) {
            callback.onError(new IllegalStateException("Please sign in to create audiobooks."));
            return false;
        }
        return true;
    }

    private boolean isValidInput(CreateAudiobookDraftInput input, RepositoryCallback<?> callback) {
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
        if (input.getTitle().length() > CreateAudiobookDraftInput.MAX_TITLE_CHARS) {
            callback.onError(new IllegalArgumentException("Title must be 120 characters or fewer."));
            return false;
        }
        if (input.getAuthor().length() > CreateAudiobookDraftInput.MAX_AUTHOR_CHARS) {
            callback.onError(new IllegalArgumentException("Author must be 120 characters or fewer."));
            return false;
        }
        if (input.getChapterText().length() > CreateAudiobookDraftInput.MAX_CHAPTER_TEXT_CHARS) {
            callback.onError(new IllegalArgumentException("Chapter text must be 4000 characters or fewer."));
            return false;
        }
        return true;
    }

    private String currentUserUid() {
        return userProvider == null ? null : trimToNull(userProvider.currentUid());
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
