package com.example.fonos_group13.controller.ai;

import com.example.fonos_group13.controller.core.RequestGate;
import com.example.fonos_group13.data.core.RepositoryCallback;
import com.example.fonos_group13.data.repository.AiChatRepository;
import com.example.fonos_group13.model.AiChatMessage;
import com.example.fonos_group13.model.AiResponse;
import com.example.fonos_group13.model.AiScope;

import java.util.List;

public final class AiChatController {
    private final AiChatRepository repository;
    private final RequestGate gate = new RequestGate();
    private long generation;

    public AiChatController(AiChatRepository repository) {
        this.repository = repository;
    }

    public void start() { generation = gate.open(); }

    public void stop() {
        gate.invalidate();
        repository.cancelPendingRequests();
    }

    public void requestResponse(String bookId, String mode, AiScope scope, String question, String locale,
                                List<AiChatMessage> history, RepositoryCallback<AiResponse> callback) {
        long requestGeneration = generation;
        repository.requestResponse(bookId, mode, scope, question, locale, history,
                new RepositoryCallback<AiResponse>() {
                    @Override public void onSuccess(AiResponse data) {
                        if (gate.isCurrent(requestGeneration)) callback.onSuccess(data);
                    }

                    @Override public void onError(Exception exception) {
                        if (gate.isCurrent(requestGeneration)) callback.onError(exception);
                    }
                });
    }
}
