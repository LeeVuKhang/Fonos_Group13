package com.example.fonos_group13.data.creator;

import android.content.Context;
import android.os.Handler;

import com.example.fonos_group13.BuildConfig;
import com.example.fonos_group13.data.core.FirebaseConfig;
import com.example.fonos_group13.data.core.RepositoryCallback;
import com.example.fonos_group13.model.CreateAudiobookDraftInput;
import com.example.fonos_group13.model.CreateChapterDraftInput;
import com.example.fonos_group13.model.EditableAudiobookDraft;
import com.example.fonos_group13.model.EditableChapterDraft;
import com.google.firebase.auth.FirebaseAuth;

import java.util.concurrent.ExecutorService;

public class CreatorAudiobookRepository implements com.example.fonos_group13.data.repository.CreatorCommandRepository {
    private final boolean configured;
    private final SignedInUserProvider userProvider;
    private final CreatorBackendDataSource backendApi;
    private final CreatorDraftValidator validator = new CreatorDraftValidator();

    public CreatorAudiobookRepository(
            Context context,
            ExecutorService executorService,
            Handler mainHandler
    ) {
        configured = FirebaseConfig.isConfigured(context);
        SignedInUserProvider resolvedUserProvider = () -> null;
        CreatorBackendDataSource resolvedBackendApi = null;
        if (configured) {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            resolvedUserProvider = new FirebaseSignedInUserProvider(auth);
            resolvedBackendApi = new CreatorApiClient(
                    BuildConfig.BACKEND_BASE_URL,
                    auth,
                    executorService,
                    mainHandler
            );
        }
        userProvider = resolvedUserProvider;
        backendApi = resolvedBackendApi;
    }

    CreatorAudiobookRepository(
            boolean configured,
            SignedInUserProvider userProvider,
            CreatorBackendDataSource backendApi
    ) {
        this.configured = configured;
        this.userProvider = userProvider;
        this.backendApi = backendApi;
    }

    @Override
    public void cancelPendingRequests() {
        if (backendApi != null) {
            backendApi.cancelPendingRequests();
        }
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
        Exception validationError = validator.validate(input);
        if (validationError != null) {
            callback.onError(validationError);
            return false;
        }
        return true;
    }

    private boolean isValidChapterInput(CreateChapterDraftInput input, RepositoryCallback<?> callback) {
        Exception validationError = validator.validate(input);
        if (validationError != null) {
            callback.onError(validationError);
            return false;
        }
        return true;
    }

    private String currentUserUid() {
        return userProvider == null ? null : trimToNull(userProvider.currentUid());
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
