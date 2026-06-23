package com.example.fonos_group13.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.fonos_group13.model.CreateAudiobookDraftInput;
import com.example.fonos_group13.model.CreatorVoiceOption;

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
    public void writeMethodsRequireSignedInUserBeforeCallingBackend() {
        FakeBackendApi backendApi = new FakeBackendApi();
        CreatorAudiobookRepository repository = repositoryWith(backendApi, null);
        CapturingCallback<String> callback = new CapturingCallback<>();

        repository.createDraft(validInput(), callback);

        assertTrue(callback.error instanceof IllegalStateException);
        assertEquals(null, backendApi.createdInput);
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
        String createdBookId = "book-1";
        String requestedBookId;
        Exception generationError;

        @Override
        public void createDraft(CreateAudiobookDraftInput input, RepositoryCallback<String> callback) {
            createdInput = input;
            callback.onSuccess(createdBookId);
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
