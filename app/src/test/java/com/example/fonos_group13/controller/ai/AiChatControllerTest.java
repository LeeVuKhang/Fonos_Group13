package com.example.fonos_group13.controller.ai;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.fonos_group13.data.core.RepositoryCallback;
import com.example.fonos_group13.data.repository.AiChatRepository;
import com.example.fonos_group13.model.AiChatMessage;
import com.example.fonos_group13.model.AiResponse;
import com.example.fonos_group13.model.AiScope;

import org.junit.Test;

import java.util.List;

public class AiChatControllerTest {
    @Test public void stopCancelsNetworkAndSuppressesLateCallback() {
        FakeRepository repository = new FakeRepository();
        AiChatController controller = new AiChatController(repository);
        RecordingCallback callback = new RecordingCallback();
        controller.start();
        controller.requestResponse("book-1", "summary", AiScope.book(), null, "en", List.of(), callback);

        controller.stop();
        repository.callback.onSuccess(new AiResponse("summary", false, AiScope.book(), "hash", List.of()));

        assertTrue(repository.cancelled);
        assertNull(callback.response);
    }

    private static final class FakeRepository implements AiChatRepository {
        RepositoryCallback<AiResponse> callback;
        boolean cancelled;
        @Override public void requestResponse(String bookId, String mode, AiScope scope, String question,
                                              String locale, List<AiChatMessage> history,
                                              RepositoryCallback<AiResponse> callback) {
            this.callback = callback;
        }
        @Override public void cancelPendingRequests() { cancelled = true; }
    }

    private static final class RecordingCallback implements RepositoryCallback<AiResponse> {
        AiResponse response;
        @Override public void onSuccess(AiResponse data) { response = data; }
        @Override public void onError(Exception exception) { }
    }
}
