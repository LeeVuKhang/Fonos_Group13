package com.example.fonos_group13.data.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.example.fonos_group13.data.core.RepositoryCallback;
import com.example.fonos_group13.model.AiChatMessage;
import com.example.fonos_group13.model.AiResponse;
import com.example.fonos_group13.model.AiScope;

import org.junit.Test;

import java.util.List;

public class DefaultAiChatRepositoryTest {
    @Test public void validatesAndDelegatesQuestionRequest() {
        RecordingBackend backend = new RecordingBackend();
        DefaultAiChatRepository repository = new DefaultAiChatRepository(true, backend);

        repository.requestResponse(" book-1 ", "question", AiScope.chapter("chapter_2"),
                " Why? ", "auto", List.of(), new RecordingCallback());

        assertEquals(1, backend.calls);
        assertEquals("book-1", backend.bookId);
        assertEquals("Why?", backend.question);
    }

    @Test public void rejectsMissingOrOversizedQuestionsBeforeNetwork() {
        RecordingBackend backend = new RecordingBackend();
        DefaultAiChatRepository repository = new DefaultAiChatRepository(true, backend);
        RecordingCallback callback = new RecordingCallback();

        repository.requestResponse("book-1", "question", AiScope.book(), " ", "auto", List.of(), callback);

        assertEquals(0, backend.calls);
        assertNotNull(callback.error);
    }

    private static final class RecordingBackend implements AiBackendDataSource {
        int calls;
        String bookId;
        String question;

        @Override public void requestResponse(String bookId, String mode, AiScope scope, String question,
                                              String locale, List<AiChatMessage> history,
                                              RepositoryCallback<AiResponse> callback) {
            calls++;
            this.bookId = bookId;
            this.question = question;
        }

        @Override public void cancelPendingRequests() { }
    }

    private static final class RecordingCallback implements RepositoryCallback<AiResponse> {
        Exception error;
        @Override public void onSuccess(AiResponse data) { }
        @Override public void onError(Exception exception) { error = exception; }
    }
}
