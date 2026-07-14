package com.example.fonos_group13.data.ai;

import com.example.fonos_group13.data.core.RepositoryCallback;
import com.example.fonos_group13.model.AiChatMessage;
import com.example.fonos_group13.model.AiResponse;
import com.example.fonos_group13.model.AiScope;

import java.util.List;

interface AiBackendDataSource {
    void requestResponse(
            String bookId,
            String mode,
            AiScope scope,
            String question,
            String locale,
            List<AiChatMessage> history,
            RepositoryCallback<AiResponse> callback
    );

    void cancelPendingRequests();
}
