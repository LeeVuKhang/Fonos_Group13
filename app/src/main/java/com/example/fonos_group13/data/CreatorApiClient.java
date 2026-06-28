package com.example.fonos_group13.data;

import android.os.Handler;
import android.os.Looper;

import com.example.fonos_group13.model.CreateAudiobookDraftInput;
import com.example.fonos_group13.model.EditableAudiobookDraft;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class CreatorApiClient implements CreatorBackendDataSource {
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 30_000;

    private final String baseUrl;
    private final FirebaseAuth auth;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    CreatorApiClient(String baseUrl, FirebaseAuth auth) {
        this(baseUrl, auth, Executors.newSingleThreadExecutor(), new Handler(Looper.getMainLooper()));
    }

    CreatorApiClient(String baseUrl, FirebaseAuth auth, ExecutorService executorService, Handler mainHandler) {
        this.baseUrl = stripTrailingSlash(baseUrl);
        this.auth = auth;
        this.executorService = executorService;
        this.mainHandler = mainHandler;
    }

    @Override
    public void createDraft(CreateAudiobookDraftInput input, RepositoryCallback<String> callback) {
        try {
            String body = CreatorApiContract.createDraftJson(input);
            withToken(callback, token -> sendJson("POST", "/api/v1/audiobooks", token, body, callback));
        } catch (JSONException exception) {
            callback.onError(exception);
        }
    }

    @Override
    public void getDraftForEdit(String bookId, RepositoryCallback<EditableAudiobookDraft> callback) {
        String encodedBookId = encodePathSegment(bookId);
        withToken(callback, token -> getEditableDraft(
                "/api/v1/audiobooks/" + encodedBookId + "/draft",
                token,
                callback
        ));
    }

    @Override
    public void updateDraft(String bookId, CreateAudiobookDraftInput input, RepositoryCallback<String> callback) {
        try {
            String encodedBookId = encodePathSegment(bookId);
            String body = CreatorApiContract.createDraftJson(input);
            withToken(callback, token -> sendJson(
                    "PUT",
                    "/api/v1/audiobooks/" + encodedBookId + "/draft",
                    token,
                    body,
                    callback
            ));
        } catch (JSONException exception) {
            callback.onError(exception);
        }
    }

    @Override
    public void requestGeneration(String bookId, RepositoryCallback<Void> callback) {
        String encodedBookId = encodePathSegment(bookId);
        withToken(callback, token -> sendJson(
                "POST",
                "/api/v1/audiobooks/" + encodedBookId + "/generation-jobs",
                token,
                "{}",
                new RepositoryCallback<String>() {
                    @Override
                    public void onSuccess(String data) {
                        callback.onSuccess(null);
                    }

                    @Override
                    public void onError(Exception exception) {
                        callback.onError(exception);
                    }
                }
        ));
    }

    @Override
    public void publishAudiobook(String bookId, RepositoryCallback<Void> callback) {
        String encodedBookId = encodePathSegment(bookId);
        withToken(callback, token -> sendJson(
                "POST",
                "/api/v1/audiobooks/" + encodedBookId + "/publications",
                token,
                "{}",
                new RepositoryCallback<String>() {
                    @Override
                    public void onSuccess(String data) {
                        callback.onSuccess(null);
                    }

                    @Override
                    public void onError(Exception exception) {
                        callback.onError(exception);
                    }
                }
        ));
    }

    private void withToken(RepositoryCallback<?> callback, TokenCallback tokenCallback) {
        if (baseUrl == null || baseUrl.isEmpty()) {
            callback.onError(new IllegalStateException("Backend base URL is not configured."));
            return;
        }
        FirebaseUser user = auth == null ? null : auth.getCurrentUser();
        if (user == null) {
            callback.onError(new IllegalStateException("Please sign in to create audiobooks."));
            return;
        }
        user.getIdToken(false)
                .addOnSuccessListener(result -> {
                    String token = result == null ? null : result.getToken();
                    if (token == null || token.trim().isEmpty()) {
                        callback.onError(new IllegalStateException("Could not get Firebase ID token."));
                    } else {
                        tokenCallback.onToken(token);
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    private void sendJson(String method, String path, String token, String body, RepositoryCallback<String> callback) {
        executorService.execute(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(baseUrl + path).openConnection();
                connection.setRequestMethod(method);
                connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
                connection.setReadTimeout(READ_TIMEOUT_MS);
                connection.setDoOutput(true);
                connection.setRequestProperty("Authorization", "Bearer " + token);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                byte[] requestBody = body.getBytes(StandardCharsets.UTF_8);
                try (OutputStream outputStream = connection.getOutputStream()) {
                    outputStream.write(requestBody);
                }
                int statusCode = connection.getResponseCode();
                String responseBody = readResponseBody(connection, statusCode);
                String bookId = CreatorApiContract.parseBookId(statusCode, responseBody);
                postSuccess(callback, bookId);
            } catch (Exception exception) {
                postError(callback, exception);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private void getEditableDraft(String path, String token, RepositoryCallback<EditableAudiobookDraft> callback) {
        executorService.execute(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(baseUrl + path).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
                connection.setReadTimeout(READ_TIMEOUT_MS);
                connection.setRequestProperty("Authorization", "Bearer " + token);
                int statusCode = connection.getResponseCode();
                String responseBody = readResponseBody(connection, statusCode);
                EditableAudiobookDraft draft = CreatorApiContract.parseEditableDraft(statusCode, responseBody);
                postSuccess(callback, draft);
            } catch (Exception exception) {
                postError(callback, exception);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private String readResponseBody(HttpURLConnection connection, int statusCode) throws IOException {
        InputStream stream = statusCode >= 400 ? connection.getErrorStream() : connection.getInputStream();
        if (stream == null) {
            return "";
        }
        try (InputStream inputStream = stream; ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            return outputStream.toString(StandardCharsets.UTF_8.name());
        }
    }

    private <T> void postSuccess(RepositoryCallback<T> callback, T value) {
        mainHandler.post(() -> callback.onSuccess(value));
    }

    private void postError(RepositoryCallback<?> callback, Exception exception) {
        mainHandler.post(() -> callback.onError(exception));
    }

    private static String stripTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private static String encodePathSegment(String value) {
        try {
            return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8.name()).replace("+", "%20");
        } catch (Exception exception) {
            return "";
        }
    }

    private interface TokenCallback {
        void onToken(String token);
    }
}
