package com.example.fonos_group13.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.fonos_group13.model.CreateAudiobookDraftInput;
import com.example.fonos_group13.model.CreateChapterDraftInput;
import com.example.fonos_group13.model.CreatorVoiceOption;
import com.example.fonos_group13.model.EditableAudiobookDraft;
import com.example.fonos_group13.model.EditableChapterDraft;

import org.junit.Test;

public class CreatorAudiobookRepositoryBackendTest {
    @Test
    public void createDraftDelegatesToBackendApi() {
        FakeBackendApi backendApi = new FakeBackendApi();
        backendApi.createdBookId = "book-1";
        CreatorAudiobookRepository repository = repositoryWith(backendApi, "user-1");
        CapturingCallback<String> callback = new CapturingCallback<>();

        repository.createDraft(validInput(), callback);

        assertEquals("book-1", callback.value);
        assertEquals("Title", backendApi.createdInput.getTitle());
    }

    @Test
    public void createDraftAndRequestGenerationReportsPartialSuccessForRetry() {
        FakeBackendApi backendApi = new FakeBackendApi();
        backendApi.createdBookId = "book-1";
        backendApi.generationError = new BackendApiException(409, "invalid_generation_state", "Try later", null);
        CreatorAudiobookRepository repository = repositoryWith(backendApi, "user-1");
        CapturingCallback<String> callback = new CapturingCallback<>();

        repository.createDraftAndRequestGeneration(validInput(), callback);

        assertTrue(callback.error instanceof DraftSavedGenerationRequestException);
        DraftSavedGenerationRequestException exception = (DraftSavedGenerationRequestException) callback.error;
        assertEquals("book-1", exception.getBookId());
    }

    @Test
    public void requestGenerationDelegatesToBackendApi() {
        FakeBackendApi backendApi = new FakeBackendApi();
        CreatorAudiobookRepository repository = repositoryWith(backendApi, "user-1");
        CapturingCallback<Void> callback = new CapturingCallback<>();

        repository.requestGeneration("book-1", callback);

        assertEquals("book-1", backendApi.requestedBookId);
        assertEquals(null, callback.error);
    }

    @Test
    public void publishAudiobookDelegatesToBackendApi() {
        FakeBackendApi backendApi = new FakeBackendApi();
        CreatorAudiobookRepository repository = repositoryWith(backendApi, "user-1");
        CapturingCallback<Void> callback = new CapturingCallback<>();

        repository.publishAudiobook("book-1", callback);

        assertEquals("book-1", backendApi.publishedBookId);
        assertEquals(null, callback.error);
    }

    @Test
    public void setAudiobookVisibilityDelegatesToBackendApi() {
        FakeBackendApi backendApi = new FakeBackendApi();
        CreatorAudiobookRepository repository = repositoryWith(backendApi, "user-1");
        CapturingCallback<Void> callback = new CapturingCallback<>();

        repository.setAudiobookVisibility("book-1", true, callback);

        assertEquals("book-1", backendApi.visibilityBookId);
        assertEquals(true, backendApi.hiddenByCreator);
        assertEquals(null, callback.error);
    }

    @Test
    public void deleteChapterDelegatesToBackendApi() {
        FakeBackendApi backendApi = new FakeBackendApi();
        CreatorAudiobookRepository repository = repositoryWith(backendApi, "user-1");
        CapturingCallback<Void> callback = new CapturingCallback<>();

        repository.deleteChapter("book-1", "chapter_2", callback);

        assertEquals("book-1", backendApi.deletedChapterBookId);
        assertEquals("chapter_2", backendApi.deletedChapterId);
        assertEquals(null, callback.error);
    }

    @Test
    public void getDraftForEditDelegatesToBackendApi() {
        FakeBackendApi backendApi = new FakeBackendApi();
        CreatorAudiobookRepository repository = repositoryWith(backendApi, "user-1");
        CapturingCallback<EditableAudiobookDraft> callback = new CapturingCallback<>();

        repository.getDraftForEdit("book-1", callback);

        assertEquals("book-1", backendApi.loadedBookId);
        assertEquals("book-1", callback.value.getBookId());
        assertEquals("Title", callback.value.getTitle());
    }

    @Test
    public void updateDraftDelegatesToBackendApi() {
        FakeBackendApi backendApi = new FakeBackendApi();
        CreatorAudiobookRepository repository = repositoryWith(backendApi, "user-1");
        CapturingCallback<String> callback = new CapturingCallback<>();

        repository.updateDraft("book-1", validInput(), callback);

        assertEquals("book-1", backendApi.updatedBookId);
        assertEquals("Title", backendApi.updatedInput.getTitle());
        assertEquals("book-1", callback.value);
    }

    @Test
    public void updateDraftAndRequestGenerationUsesExistingBookId() {
        FakeBackendApi backendApi = new FakeBackendApi();
        CreatorAudiobookRepository repository = repositoryWith(backendApi, "user-1");
        CapturingCallback<String> callback = new CapturingCallback<>();

        repository.updateDraftAndRequestGeneration("book-1", validInput(), callback);

        assertEquals("book-1", backendApi.updatedBookId);
        assertEquals("book-1", backendApi.requestedBookId);
        assertEquals("book-1", callback.value);
    }

    @Test
    public void createChapterDraftDelegatesToBackendApi() {
        FakeBackendApi backendApi = new FakeBackendApi();
        CreatorAudiobookRepository repository = repositoryWith(backendApi, "user-1");
        CapturingCallback<String> callback = new CapturingCallback<>();

        repository.createChapterDraft("book-1", validChapterInput(), callback);

        assertEquals("chapter_2", callback.value);
        assertEquals("book-1", backendApi.createdChapterBookId);
        assertEquals("Chapter 2", backendApi.createdChapterInput.getChapterTitle());
    }

    @Test
    public void updateChapterDraftAndRequestGenerationUsesExistingChapterId() {
        FakeBackendApi backendApi = new FakeBackendApi();
        CreatorAudiobookRepository repository = repositoryWith(backendApi, "user-1");
        CapturingCallback<String> callback = new CapturingCallback<>();

        repository.updateChapterDraftAndRequestGeneration("book-1", "chapter_2", validChapterInput(), callback);

        assertEquals("book-1", backendApi.updatedChapterBookId);
        assertEquals("chapter_2", backendApi.updatedChapterId);
        assertEquals("chapter_2", backendApi.requestedChapterId);
        assertEquals("chapter_2", callback.value);
    }

    @Test
    public void requestChapterGenerationDelegatesToBackendApi() {
        FakeBackendApi backendApi = new FakeBackendApi();
        CreatorAudiobookRepository repository = repositoryWith(backendApi, "user-1");
        CapturingCallback<Void> callback = new CapturingCallback<>();

        repository.requestChapterGeneration("book-1", "chapter_2", callback);

        assertEquals("book-1", backendApi.requestedChapterBookId);
        assertEquals("chapter_2", backendApi.requestedChapterId);
        assertEquals(null, callback.error);
    }

    @Test
    public void writeMethodsRequireSignedInUserBeforeCallingBackend() {
        FakeBackendApi backendApi = new FakeBackendApi();
        CreatorAudiobookRepository repository = repositoryWith(backendApi, null);
        CapturingCallback<String> callback = new CapturingCallback<>();

        repository.createDraft(validInput(), callback);

        assertTrue(callback.error instanceof IllegalStateException);
        assertEquals(null, backendApi.createdInput);
    }

    @Test
    public void updateDraftRequiresSignedInUserBeforeCallingBackend() {
        FakeBackendApi backendApi = new FakeBackendApi();
        CreatorAudiobookRepository repository = repositoryWith(backendApi, null);
        CapturingCallback<String> callback = new CapturingCallback<>();

        repository.updateDraft("book-1", validInput(), callback);

        assertTrue(callback.error instanceof IllegalStateException);
        assertEquals(null, backendApi.updatedInput);
    }

    @Test
    public void publishRequiresSignedInUserBeforeCallingBackend() {
        FakeBackendApi backendApi = new FakeBackendApi();
        CreatorAudiobookRepository repository = repositoryWith(backendApi, null);
        CapturingCallback<Void> callback = new CapturingCallback<>();

        repository.publishAudiobook("book-1", callback);

        assertTrue(callback.error instanceof IllegalStateException);
        assertEquals(null, backendApi.publishedBookId);
    }

    @Test
    public void visibilityRequiresSignedInUserBeforeCallingBackend() {
        FakeBackendApi backendApi = new FakeBackendApi();
        CreatorAudiobookRepository repository = repositoryWith(backendApi, null);
        CapturingCallback<Void> callback = new CapturingCallback<>();

        repository.setAudiobookVisibility("book-1", true, callback);

        assertTrue(callback.error instanceof IllegalStateException);
        assertEquals(null, backendApi.visibilityBookId);
    }

    @Test
    public void deleteChapterRequiresSignedInUserBeforeCallingBackend() {
        FakeBackendApi backendApi = new FakeBackendApi();
        CreatorAudiobookRepository repository = repositoryWith(backendApi, null);
        CapturingCallback<Void> callback = new CapturingCallback<>();

        repository.deleteChapter("book-1", "chapter_2", callback);

        assertTrue(callback.error instanceof IllegalStateException);
        assertEquals(null, backendApi.deletedChapterBookId);
    }

    @Test
    public void visibilityRequiresAudiobookIdBeforeCallingBackend() {
        FakeBackendApi backendApi = new FakeBackendApi();
        CreatorAudiobookRepository repository = repositoryWith(backendApi, "user-1");
        CapturingCallback<Void> callback = new CapturingCallback<>();

        repository.setAudiobookVisibility(" ", true, callback);

        assertTrue(callback.error instanceof IllegalArgumentException);
        assertEquals(null, backendApi.visibilityBookId);
    }

    @Test
    public void deleteChapterRequiresIdsBeforeCallingBackend() {
        FakeBackendApi backendApi = new FakeBackendApi();
        CreatorAudiobookRepository repository = repositoryWith(backendApi, "user-1");
        CapturingCallback<Void> callback = new CapturingCallback<>();

        repository.deleteChapter("book-1", " ", callback);

        assertTrue(callback.error instanceof IllegalArgumentException);
        assertEquals(null, backendApi.deletedChapterBookId);
    }

    @Test
    public void acceptsThreeThousandFiveHundredWordChapterText() {
        FakeBackendApi backendApi = new FakeBackendApi();
        CreatorAudiobookRepository repository = repositoryWith(backendApi, "user-1");
        CapturingCallback<String> callback = new CapturingCallback<>();
        String chapterText = repeatedWords(3500);

        repository.createDraft(inputWithChapterText(chapterText), callback);

        assertEquals(null, callback.error);
        assertEquals(chapterText, backendApi.createdInput.getChapterText());
    }

    @Test
    public void rejectsChapterTextOverThreeThousandFiveHundredWords() {
        FakeBackendApi backendApi = new FakeBackendApi();
        CreatorAudiobookRepository repository = repositoryWith(backendApi, "user-1");
        CapturingCallback<String> callback = new CapturingCallback<>();

        repository.createDraft(inputWithChapterText(repeatedWords(3501)), callback);

        assertTrue(callback.error instanceof IllegalArgumentException);
        assertEquals(null, backendApi.createdInput);
    }

    private CreatorAudiobookRepository repositoryWith(FakeBackendApi backendApi, String uid) {
        return new CreatorAudiobookRepository(
                true,
                null,
                () -> uid,
                backendApi
        );
    }

    private CreateAudiobookDraftInput validInput() {
        return inputWithChapterText("Hello");
    }

    private CreateChapterDraftInput validChapterInput() {
        return new CreateChapterDraftInput(
                "Chapter 2",
                "Hello again",
                "en-US",
                CreatorVoiceOption.RUTH
        );
    }

    private CreateAudiobookDraftInput inputWithChapterText(String chapterText) {
        return new CreateAudiobookDraftInput(
                "Title",
                "Author",
                null,
                "Chapter 1",
                chapterText,
                "en-US",
                CreatorVoiceOption.PATRICK
        );
    }

    private static String repeatedWords(int count) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < count; index++) {
            if (index > 0) {
                builder.append(" ");
            }
            builder.append("word");
        }
        return builder.toString();
    }

    private static class FakeBackendApi implements CreatorBackendDataSource {
        CreateAudiobookDraftInput createdInput;
        CreateAudiobookDraftInput updatedInput;
        CreateChapterDraftInput createdChapterInput;
        CreateChapterDraftInput updatedChapterInput;
        String createdBookId = "book-1";
        String createdChapterId = "chapter_2";
        String loadedBookId;
        String updatedBookId;
        String requestedBookId;
        String publishedBookId;
        String createdChapterBookId;
        String loadedChapterBookId;
        String loadedChapterId;
        String updatedChapterBookId;
        String updatedChapterId;
        String requestedChapterBookId;
        String requestedChapterId;
        String visibilityBookId;
        boolean hiddenByCreator;
        String deletedChapterBookId;
        String deletedChapterId;
        Exception generationError;

        @Override
        public void createDraft(CreateAudiobookDraftInput input, RepositoryCallback<String> callback) {
            createdInput = input;
            callback.onSuccess(createdBookId);
        }

        @Override
        public void getDraftForEdit(String bookId, RepositoryCallback<EditableAudiobookDraft> callback) {
            loadedBookId = bookId;
            callback.onSuccess(new EditableAudiobookDraft(
                    bookId,
                    "Title",
                    "Author",
                    null,
                    "Chapter 1",
                    "Hello",
                    "en-US",
                    CreatorVoiceOption.PATRICK,
                    null
            ));
        }

        @Override
        public void updateDraft(String bookId, CreateAudiobookDraftInput input, RepositoryCallback<String> callback) {
            updatedBookId = bookId;
            updatedInput = input;
            callback.onSuccess(bookId);
        }

        @Override
        public void createChapterDraft(String bookId, CreateChapterDraftInput input, RepositoryCallback<String> callback) {
            createdChapterBookId = bookId;
            createdChapterInput = input;
            callback.onSuccess(createdChapterId);
        }

        @Override
        public void getChapterDraftForEdit(String bookId, String chapterId, RepositoryCallback<EditableChapterDraft> callback) {
            loadedChapterBookId = bookId;
            loadedChapterId = chapterId;
            callback.onSuccess(new EditableChapterDraft(
                    bookId,
                    chapterId,
                    "Title",
                    "Chapter 2",
                    "Hello again",
                    "en-US",
                    CreatorVoiceOption.RUTH,
                    null
            ));
        }

        @Override
        public void updateChapterDraft(
                String bookId,
                String chapterId,
                CreateChapterDraftInput input,
                RepositoryCallback<String> callback
        ) {
            updatedChapterBookId = bookId;
            updatedChapterId = chapterId;
            updatedChapterInput = input;
            callback.onSuccess(chapterId);
        }

        @Override
        public void requestGeneration(String bookId, RepositoryCallback<Void> callback) {
            requestedBookId = bookId;
            if (generationError != null) {
                callback.onError(generationError);
            } else {
                callback.onSuccess(null);
            }
        }

        @Override
        public void requestChapterGeneration(String bookId, String chapterId, RepositoryCallback<Void> callback) {
            requestedChapterBookId = bookId;
            requestedChapterId = chapterId;
            if (generationError != null) {
                callback.onError(generationError);
            } else {
                callback.onSuccess(null);
            }
        }

        @Override
        public void publishAudiobook(String bookId, RepositoryCallback<Void> callback) {
            publishedBookId = bookId;
            callback.onSuccess(null);
        }

        @Override
        public void setAudiobookVisibility(String bookId, boolean hiddenByCreator, RepositoryCallback<Void> callback) {
            visibilityBookId = bookId;
            this.hiddenByCreator = hiddenByCreator;
            callback.onSuccess(null);
        }

        @Override
        public void deleteChapter(String bookId, String chapterId, RepositoryCallback<Void> callback) {
            deletedChapterBookId = bookId;
            deletedChapterId = chapterId;
            callback.onSuccess(null);
        }
    }

    private static class CapturingCallback<T> implements RepositoryCallback<T> {
        T value;
        Exception error;

        @Override
        public void onSuccess(T data) {
            value = data;
        }

        @Override
        public void onError(Exception exception) {
            error = exception;
        }
    }
}
