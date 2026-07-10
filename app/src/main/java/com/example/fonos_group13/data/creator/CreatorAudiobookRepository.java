package com.example.fonos_group13.data.creator;

import android.content.Context;

import com.example.fonos_group13.BuildConfig;
import com.example.fonos_group13.data.core.FirebaseConfig;
import com.example.fonos_group13.data.core.RepositoryCallback;
import com.example.fonos_group13.model.CreateAudiobookDraftInput;
import com.example.fonos_group13.model.CreateChapterDraftInput;
import com.example.fonos_group13.model.EditableAudiobookDraft;
import com.example.fonos_group13.model.EditableChapterDraft;
import com.example.fonos_group13.model.UserGeneratedAudiobook;
import com.example.fonos_group13.model.UserGeneratedChapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CreatorAudiobookRepository {
    private static final String COLLECTION_BOOKS = "books";
    private static final String COLLECTION_CHAPTERS = "chapters";

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

    public void getDraftForEdit(String bookId, RepositoryCallback<EditableAudiobookDraft> callback) {
        if (!canUseBackend(callback, "Please sign in to edit audiobooks.")) {
            return;
        }
        String safeBookId = trimToNull(bookId);
        if (safeBookId == null) {
            callback.onError(new IllegalArgumentException("Missing audiobook id."));
            return;
        }
        backendApi.getDraftForEdit(safeBookId, callback);
    }

    public void updateDraft(String bookId, CreateAudiobookDraftInput input, RepositoryCallback<String> callback) {
        if (!canWrite(callback)) {
            return;
        }
        String safeBookId = trimToNull(bookId);
        if (safeBookId == null) {
            callback.onError(new IllegalArgumentException("Missing audiobook id."));
            return;
        }
        if (!isValidInput(input, callback)) {
            return;
        }
        backendApi.updateDraft(safeBookId, input, callback);
    }

    public void updateDraftAndRequestGeneration(String bookId, CreateAudiobookDraftInput input, RepositoryCallback<String> callback) {
        if (!canWrite(callback)) {
            return;
        }
        String safeBookId = trimToNull(bookId);
        if (safeBookId == null) {
            callback.onError(new IllegalArgumentException("Missing audiobook id."));
            return;
        }
        if (!isValidInput(input, callback)) {
            return;
        }
        backendApi.updateDraft(safeBookId, input, new RepositoryCallback<String>() {
            @Override
            public void onSuccess(String updatedBookId) {
                backendApi.requestGeneration(safeBookId, new RepositoryCallback<Void>() {
                    @Override
                    public void onSuccess(Void data) {
                        callback.onSuccess(updatedBookId);
                    }

                    @Override
                    public void onError(Exception exception) {
                        callback.onError(new DraftSavedGenerationRequestException(safeBookId, exception));
                    }
                });
            }

            @Override
            public void onError(Exception exception) {
                callback.onError(exception);
            }
        });
    }

    public void createChapterDraft(String bookId, CreateChapterDraftInput input, RepositoryCallback<String> callback) {
        if (!canWrite(callback)) {
            return;
        }
        String safeBookId = trimToNull(bookId);
        if (safeBookId == null) {
            callback.onError(new IllegalArgumentException("Missing audiobook id."));
            return;
        }
        if (!isValidChapterInput(input, callback)) {
            return;
        }
        backendApi.createChapterDraft(safeBookId, input, callback);
    }

    public void createChapterDraftAndRequestGeneration(String bookId, CreateChapterDraftInput input, RepositoryCallback<String> callback) {
        if (!canWrite(callback)) {
            return;
        }
        String safeBookId = trimToNull(bookId);
        if (safeBookId == null) {
            callback.onError(new IllegalArgumentException("Missing audiobook id."));
            return;
        }
        if (!isValidChapterInput(input, callback)) {
            return;
        }
        backendApi.createChapterDraft(safeBookId, input, new RepositoryCallback<String>() {
            @Override
            public void onSuccess(String chapterId) {
                backendApi.requestChapterGeneration(safeBookId, chapterId, new RepositoryCallback<Void>() {
                    @Override
                    public void onSuccess(Void data) {
                        callback.onSuccess(chapterId);
                    }

                    @Override
                    public void onError(Exception exception) {
                        callback.onError(new DraftSavedGenerationRequestException(safeBookId, exception));
                    }
                });
            }

            @Override
            public void onError(Exception exception) {
                callback.onError(exception);
            }
        });
    }

    public void getChapterDraftForEdit(
            String bookId,
            String chapterId,
            RepositoryCallback<EditableChapterDraft> callback
    ) {
        if (!canUseBackend(callback, "Please sign in to edit audiobooks.")) {
            return;
        }
        String safeBookId = trimToNull(bookId);
        String safeChapterId = trimToNull(chapterId);
        if (safeBookId == null || safeChapterId == null) {
            callback.onError(new IllegalArgumentException("Missing chapter id."));
            return;
        }
        backendApi.getChapterDraftForEdit(safeBookId, safeChapterId, callback);
    }

    public void updateChapterDraft(
            String bookId,
            String chapterId,
            CreateChapterDraftInput input,
            RepositoryCallback<String> callback
    ) {
        if (!canWrite(callback)) {
            return;
        }
        String safeBookId = trimToNull(bookId);
        String safeChapterId = trimToNull(chapterId);
        if (safeBookId == null || safeChapterId == null) {
            callback.onError(new IllegalArgumentException("Missing chapter id."));
            return;
        }
        if (!isValidChapterInput(input, callback)) {
            return;
        }
        backendApi.updateChapterDraft(safeBookId, safeChapterId, input, callback);
    }

    public void updateChapterDraftAndRequestGeneration(
            String bookId,
            String chapterId,
            CreateChapterDraftInput input,
            RepositoryCallback<String> callback
    ) {
        if (!canWrite(callback)) {
            return;
        }
        String safeBookId = trimToNull(bookId);
        String safeChapterId = trimToNull(chapterId);
        if (safeBookId == null || safeChapterId == null) {
            callback.onError(new IllegalArgumentException("Missing chapter id."));
            return;
        }
        if (!isValidChapterInput(input, callback)) {
            return;
        }
        backendApi.updateChapterDraft(safeBookId, safeChapterId, input, new RepositoryCallback<String>() {
            @Override
            public void onSuccess(String updatedChapterId) {
                backendApi.requestChapterGeneration(safeBookId, safeChapterId, new RepositoryCallback<Void>() {
                    @Override
                    public void onSuccess(Void data) {
                        callback.onSuccess(updatedChapterId);
                    }

                    @Override
                    public void onError(Exception exception) {
                        callback.onError(new DraftSavedGenerationRequestException(safeBookId, exception));
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
                    callback.onSuccess(mapUploads(querySnapshot));
                })
                .addOnFailureListener(callback::onError);
    }

    public ListenerRegistration observeMyUploads(RepositoryCallback<List<UserGeneratedAudiobook>> callback) {
        String uid = currentUserUid();
        if (!configured || firestore == null) {
            callback.onError(FirebaseConfig.missingConfigException());
            return null;
        }
        if (uid == null) {
            callback.onError(new IllegalStateException("Please sign in to view your uploads."));
            return null;
        }

        return firestore.collection(COLLECTION_BOOKS)
                .whereEqualTo("creatorUid", uid)
                .whereEqualTo("createdByUser", true)
                .addSnapshotListener((querySnapshot, exception) -> {
                    if (exception != null) {
                        callback.onError(exception);
                        return;
                    }
                    callback.onSuccess(mapUploads(querySnapshot));
                });
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

    public void requestChapterGeneration(String bookId, String chapterId, RepositoryCallback<Void> callback) {
        if (!canWrite(callback)) {
            return;
        }
        String safeBookId = trimToNull(bookId);
        String safeChapterId = trimToNull(chapterId);
        if (safeBookId == null || safeChapterId == null) {
            callback.onError(new IllegalArgumentException("Missing chapter id."));
            return;
        }
        backendApi.requestChapterGeneration(safeBookId, safeChapterId, callback);
    }

    public void publishAudiobook(String bookId, RepositoryCallback<Void> callback) {
        if (!canUseBackend(callback, "Please sign in to publish audiobooks.")) {
            return;
        }
        String safeBookId = trimToNull(bookId);
        if (safeBookId == null) {
            callback.onError(new IllegalArgumentException("Missing audiobook id."));
            return;
        }
        backendApi.publishAudiobook(safeBookId, callback);
    }

    public void setAudiobookVisibility(String bookId, boolean hiddenByCreator, RepositoryCallback<Void> callback) {
        if (!canUseBackend(callback, "Please sign in to manage audiobook visibility.")) {
            return;
        }
        String safeBookId = trimToNull(bookId);
        if (safeBookId == null) {
            callback.onError(new IllegalArgumentException("Missing audiobook id."));
            return;
        }
        backendApi.setAudiobookVisibility(safeBookId, hiddenByCreator, callback);
    }

    public void deleteChapter(String bookId, String chapterId, RepositoryCallback<Void> callback) {
        if (!canUseBackend(callback, "Please sign in to manage chapters.")) {
            return;
        }
        String safeBookId = trimToNull(bookId);
        String safeChapterId = trimToNull(chapterId);
        if (safeBookId == null || safeChapterId == null) {
            callback.onError(new IllegalArgumentException("Missing chapter id."));
            return;
        }
        backendApi.deleteChapter(safeBookId, safeChapterId, callback);
    }

    public ListenerRegistration observeUploadChapters(
            String bookId,
            RepositoryCallback<List<UserGeneratedChapter>> callback
    ) {
        String uid = currentUserUid();
        if (!configured || firestore == null) {
            callback.onError(FirebaseConfig.missingConfigException());
            return null;
        }
        if (uid == null) {
            callback.onError(new IllegalStateException("Please sign in to view chapters."));
            return null;
        }
        String safeBookId = trimToNull(bookId);
        if (safeBookId == null) {
            callback.onError(new IllegalArgumentException("Missing audiobook id."));
            return null;
        }

        return firestore.collection(COLLECTION_BOOKS)
                .document(safeBookId)
                .collection(COLLECTION_CHAPTERS)
                .addSnapshotListener((querySnapshot, exception) -> {
                    if (exception != null) {
                        callback.onError(exception);
                        return;
                    }
                    callback.onSuccess(mapUploadChapters(safeBookId, querySnapshot));
                });
    }

    private List<UserGeneratedAudiobook> mapUploads(QuerySnapshot querySnapshot) {
        List<UserGeneratedAudiobook> uploads = new ArrayList<>();
        if (querySnapshot != null) {
            querySnapshot.getDocuments().forEach(document -> uploads.add(UserGeneratedAudiobook.fromDocument(document)));
        }
        Collections.sort(uploads, (left, right) -> Long.compare(
                right.getSortTimestampMillis(),
                left.getSortTimestampMillis()
        ));
        return uploads;
    }

    private List<UserGeneratedChapter> mapUploadChapters(String bookId, QuerySnapshot querySnapshot) {
        List<UserGeneratedChapter> chapters = new ArrayList<>();
        if (querySnapshot != null) {
            querySnapshot.getDocuments().forEach(document -> {
                if (!UserGeneratedChapter.isDeletedDocument(document)) {
                    chapters.add(UserGeneratedChapter.fromDocument(bookId, document));
                }
            });
        }
        Collections.sort(chapters, (left, right) -> {
            int orderCompare = Integer.compare(left.getOrder(), right.getOrder());
            if (orderCompare != 0) {
                return orderCompare;
            }
            return left.getTitle().compareToIgnoreCase(right.getTitle());
        });
        return chapters;
    }

    private boolean canWrite(RepositoryCallback<?> callback) {
        return canUseBackend(callback, "Please sign in to create audiobooks.");
    }

    private boolean canUseBackend(RepositoryCallback<?> callback, String signedOutMessage) {
        if (!configured || backendApi == null) {
            callback.onError(FirebaseConfig.missingConfigException());
            return false;
        }
        if (currentUserUid() == null) {
            callback.onError(new IllegalStateException(signedOutMessage));
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
        if (CreateAudiobookDraftInput.countWords(input.getChapterText()) > CreateAudiobookDraftInput.MAX_CHAPTER_TEXT_WORDS) {
            callback.onError(new IllegalArgumentException("Chapter text must be 3500 words or fewer."));
            return false;
        }
        return true;
    }

    private boolean isValidChapterInput(CreateChapterDraftInput input, RepositoryCallback<?> callback) {
        if (input == null) {
            callback.onError(new IllegalArgumentException("Missing chapter draft."));
            return false;
        }
        if (isBlank(input.getChapterTitle())) {
            callback.onError(new IllegalArgumentException("Chapter title is required."));
            return false;
        }
        if (isBlank(input.getChapterText())) {
            callback.onError(new IllegalArgumentException("Chapter text is required."));
            return false;
        }
        if (CreateChapterDraftInput.countWords(input.getChapterText()) > CreateChapterDraftInput.MAX_CHAPTER_TEXT_WORDS) {
            callback.onError(new IllegalArgumentException("Chapter text must be 3500 words or fewer."));
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
