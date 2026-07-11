package com.example.fonos_group13.data.community;

import android.os.Handler;

import com.example.fonos_group13.data.core.RepositoryCallback;
import com.example.fonos_group13.model.BookReviewPage;
import com.example.fonos_group13.model.ReviewMutationResult;
import com.example.fonos_group13.model.SaveMutationResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public final class CommunityApiClient implements CommunityBackendDataSource {
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 30_000;
    private final String baseUrl;
    private final FirebaseAuth auth;
    private final ExecutorService executor;
    private final Handler mainHandler;
    private final ConcurrentLinkedQueue<Future<?>> pending = new ConcurrentLinkedQueue<>();

    public CommunityApiClient(String baseUrl, FirebaseAuth auth, ExecutorService executor, Handler mainHandler) {
        this.baseUrl = stripTrailingSlash(baseUrl);
        this.auth = auth;
        this.executor = executor;
        this.mainHandler = mainHandler;
    }

    @Override public void getReviews(String bookId, String cursor, RepositoryCallback<BookReviewPage> callback) {
        String path = "/api/v1/audiobooks/" + encode(bookId) + "/reviews?limit=10";
        if (cursor != null) path += "&cursor=" + encode(cursor);
        request("GET", path, null, CommunityApiContract::parseReviewPage, callback);
    }

    @Override public void upsertReview(String bookId, int rating, String comment, RepositoryCallback<ReviewMutationResult> callback) {
        try {
            request("PUT", "/api/v1/audiobooks/" + encode(bookId) + "/reviews/me",
                    CommunityApiContract.reviewJson(rating, comment), CommunityApiContract::parseReviewMutation, callback);
        } catch (Exception exception) {
            callback.onError(exception);
        }
    }

    @Override public void deleteReview(String bookId, RepositoryCallback<ReviewMutationResult> callback) {
        request("DELETE", "/api/v1/audiobooks/" + encode(bookId) + "/reviews/me",
                null, CommunityApiContract::parseReviewMutation, callback);
    }

    @Override public void setSaved(String bookId, boolean saved, RepositoryCallback<SaveMutationResult> callback) {
        request(saved ? "PUT" : "DELETE", "/api/v1/users/me/saved-books/" + encode(bookId),
                saved ? "{}" : null, CommunityApiContract::parseSaveMutation, callback);
    }

    @Override public void cancelPendingRequests() {
        Future<?> request;
        while ((request = pending.poll()) != null) request.cancel(true);
    }

    private <T> void request(String method, String path, String body, Parser<T> parser, RepositoryCallback<T> callback) {
        if (baseUrl.isEmpty()) {
            callback.onError(new IllegalStateException("Backend base URL is not configured."));
            return;
        }
        FirebaseUser user = auth == null ? null : auth.getCurrentUser();
        if (user == null) {
            callback.onError(new IllegalStateException("Please sign in to use community features."));
            return;
        }
        user.getIdToken(false)
                .addOnSuccessListener(result -> {
                    String token = result == null ? null : result.getToken();
                    if (token == null || token.trim().isEmpty()) {
                        callback.onError(new IllegalStateException("Could not get Firebase ID token."));
                    } else {
                        submit(method, path, body, token, parser, callback);
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    private <T> void submit(String method, String path, String body, String token, Parser<T> parser, RepositoryCallback<T> callback) {
        pending.removeIf(Future::isDone);
        pending.add(executor.submit(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(baseUrl + path).openConnection();
                connection.setRequestMethod(method);
                connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
                connection.setReadTimeout(READ_TIMEOUT_MS);
                connection.setRequestProperty("Authorization", "Bearer " + token);
                if (body != null) {
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                    try (OutputStream output = connection.getOutputStream()) {
                        output.write(body.getBytes(StandardCharsets.UTF_8));
                    }
                }
                int status = connection.getResponseCode();
                String response = readBody(connection, status);
                postSuccess(callback, parser.parse(status, response));
            } catch (Exception exception) {
                postError(callback, exception);
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

    private <T> void postSuccess(RepositoryCallback<T> callback, T data) {
        mainHandler.post(() -> callback.onSuccess(data));
    }

    private void postError(RepositoryCallback<?> callback, Exception error) {
        mainHandler.post(() -> callback.onError(error));
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8.name()).replace("+", "%20");
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String stripTrailingSlash(String value) {
        String clean = value == null ? "" : value.trim();
        while (clean.endsWith("/")) clean = clean.substring(0, clean.length() - 1);
        return clean;
    }

    private interface Parser<T> {
        T parse(int statusCode, String body) throws Exception;
    }
}
