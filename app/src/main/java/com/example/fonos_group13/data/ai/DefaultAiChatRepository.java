package com.example.fonos_group13.data.ai;

import android.content.Context;
import android.os.Handler;

import com.example.fonos_group13.BuildConfig;
import com.example.fonos_group13.data.core.FirebaseConfig;
import com.example.fonos_group13.data.core.RepositoryCallback;
import com.example.fonos_group13.data.repository.AiChatRepository;
import com.example.fonos_group13.model.AiChatMessage;
import com.example.fonos_group13.model.AiResponse;
import com.example.fonos_group13.model.AiScope;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

public final class DefaultAiChatRepository implements AiChatRepository {
    private final boolean configured;
    private final AiBackendDataSource backend;

    public DefaultAiChatRepository(Context context, ExecutorService executor, Handler mainHandler) {
        configured = FirebaseConfig.isConfigured(context);
        backend = configured
                ? new AiApiClient(BuildConfig.BACKEND_BASE_URL, FirebaseAuth.getInstance(), executor, mainHandler)
                : null;
    }

    DefaultAiChatRepository(boolean configured, AiBackendDataSource backend) {
        this.configured = configured;
        this.backend = backend;
    }

    @Override
    public void requestResponse(String bookId, String mode, AiScope scope, String question, String locale,
                                List<AiChatMessage> history, RepositoryCallback<AiResponse> callback) {
        String safeBookId = trimToNull(bookId);
        String safeMode = trimToNull(mode);
        String safeQuestion = trimToNull(question);
        if (!configured || backend == null) {
            callback.onError(FirebaseConfig.missingConfigException());
            return;
        }
        if (safeBookId == null) {
            callback.onError(new IllegalArgumentException("Missing audiobook id."));
            return;
        }
        if (!"summary".equals(safeMode) && !"question".equals(safeMode)) {
            callback.onError(new IllegalArgumentException("Unsupported AI request mode."));
            return;
        }
        if (scope == null || (!scope.isBook() && trimToNull(scope.getChapterId()) == null)) {
            callback.onError(new IllegalArgumentException("Choose a book or chapter scope."));
            return;
        }
        if ("question".equals(safeMode) && safeQuestion == null) {
            callback.onError(new IllegalArgumentException("Enter a question."));
            return;
        }
        if (safeQuestion != null && safeQuestion.length() > 1000) {
            callback.onError(new IllegalArgumentException("Questions cannot exceed 1000 characters."));
            return;
        }
        String safeLocale = "en".equals(locale) || "vi".equals(locale) || "auto".equals(locale) ? locale : "auto";
        backend.requestResponse(safeBookId, safeMode, scope, safeQuestion, safeLocale,
                history == null ? Collections.emptyList() : history, callback);
    }

    @Override public void cancelPendingRequests() {
        if (backend != null) backend.cancelPendingRequests();
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String clean = value.trim();
        return clean.isEmpty() ? null : clean;
    }
}
