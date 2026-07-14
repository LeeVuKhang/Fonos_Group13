package com.example.fonos_group13.data.ai;

import android.os.Handler;

import com.example.fonos_group13.data.core.RepositoryCallback;
import com.example.fonos_group13.model.AiChatMessage;
import com.example.fonos_group13.model.AiResponse;
import com.example.fonos_group13.model.AiScope;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

final class AiApiClient implements AiBackendDataSource {
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 35_000;
    private final String baseUrl;
    private final FirebaseAuth auth;
    private final ExecutorService executor;
    private final Handler mainHandler;
    private final ConcurrentLinkedQueue<Future<?>> pending = new ConcurrentLinkedQueue<>();
    private final AtomicLong generation = new AtomicLong();

    AiApiClient(String baseUrl, FirebaseAuth auth, ExecutorService executor, Handler mainHandler) {
        this.baseUrl = stripTrailingSlash(baseUrl);
        this.auth = auth;
        this.executor = executor;
        this.mainHandler = mainHandler;
    }

    @Override
    public void requestResponse(String bookId, String mode, AiScope scope, String question, String locale,
                                List<AiChatMessage> history, RepositoryCallback<AiResponse> callback) {
        try {
            String body = AiApiContract.requestJson(mode, scope, question, locale, history);
            request("/api/v1/audiobooks/" + encode(bookId) + "/ai/responses", body, callback);
        } catch (Exception exception) {
            callback.onError(exception);
        }
    }

    @Override public void cancelPendingRequests() {
        generation.incrementAndGet();
        Future<?> request;
        while ((request = pending.poll()) != null) request.cancel(true);
    }

    private void request(String path, String body, RepositoryCallback<AiResponse> callback) {
        if (baseUrl.isEmpty()) {
            callback.onError(new IllegalStateException("Backend base URL is not configured."));
            return;
        }
        FirebaseUser user = auth == null ? null : auth.getCurrentUser();
        if (user == null) {
            callback.onError(new IllegalStateException("Please sign in to use Ask AI."));
            return;
        }
        long requestGeneration = generation.get();
        user.getIdToken(false)
                .addOnSuccessListener(result -> {
                    if (requestGeneration != generation.get()) return;
                    String token = result == null ? null : result.getToken();
                    if (token == null || token.trim().isEmpty()) {
                        callback.onError(new IllegalStateException("Could not get Firebase ID token."));
                    } else {
                        submit(path, body, token, requestGeneration, callback);
                    }
                })
                .addOnFailureListener(error -> {
                    if (requestGeneration == generation.get()) callback.onError(error);
                });
    }

    private void submit(String path, String body, String token, long requestGeneration,
                        RepositoryCallback<AiResponse> callback) {
        pending.removeIf(Future::isDone);
        pending.add(executor.submit(() -> {
            HttpURLConnection connection = null;
            try {
                if (requestGeneration != generation.get() || Thread.currentThread().isInterrupted()) return;
                connection = (HttpURLConnection) new URL(baseUrl + path).openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
                connection.setReadTimeout(READ_TIMEOUT_MS);
                connection.setRequestProperty("Authorization", "Bearer " + token);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                connection.setDoOutput(true);
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(body.getBytes(StandardCharsets.UTF_8));
                }
                int status = connection.getResponseCode();
                String response = readBody(connection, status);
                AiResponse parsed = AiApiContract.parseResponse(status, response);
                mainHandler.post(() -> {
                    if (requestGeneration == generation.get()) callback.onSuccess(parsed);
                });
            } catch (Exception exception) {
                mainHandler.post(() -> {
                    if (requestGeneration == generation.get()) callback.onError(exception);
                });
            } finally {
                if (connection != null) connection.disconnect();
            }
        }));
    }

    private static String readBody(HttpURLConnection connection, int status) throws Exception {
        InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
        if (stream == null) return "";
        try (InputStream input = stream; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
            return output.toString(StandardCharsets.UTF_8.name());
        }
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20");
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String stripTrailingSlash(String value) {
        String clean = value == null ? "" : value.trim();
        while (clean.endsWith("/")) clean = clean.substring(0, clean.length() - 1);
        return clean;
    }
}
