package com.example.fonos_group13.data.community;

import android.content.Context;
import android.os.Handler;

import com.example.fonos_group13.BuildConfig;
import com.example.fonos_group13.data.core.FirebaseConfig;
import com.example.fonos_group13.data.core.RepositoryCallback;
import com.example.fonos_group13.data.repository.BookCommunityRepository;
import com.example.fonos_group13.model.BookReviewPage;
import com.example.fonos_group13.model.ReviewMutationResult;
import com.google.firebase.auth.FirebaseAuth;

import java.util.concurrent.ExecutorService;

public final class DefaultBookCommunityRepository implements BookCommunityRepository {
    private final boolean configured;
    private final CommunityBackendDataSource backend;

    public DefaultBookCommunityRepository(Context context, ExecutorService executor, Handler mainHandler) {
        configured = FirebaseConfig.isConfigured(context);
        backend = configured
                ? new CommunityApiClient(BuildConfig.BACKEND_BASE_URL, FirebaseAuth.getInstance(), executor, mainHandler)
                : null;
    }

    DefaultBookCommunityRepository(boolean configured, CommunityBackendDataSource backend) {
        this.configured = configured;
        this.backend = backend;
    }

    @Override
    public void getReviews(String bookId, String cursor, RepositoryCallback<BookReviewPage> callback) {
        String safeBookId = validateBookId(bookId, callback);
        if (safeBookId != null) backend.getReviews(safeBookId, trimToNull(cursor), callback);
    }

    @Override
    public void upsertReview(String bookId, int rating, String comment, RepositoryCallback<ReviewMutationResult> callback) {
        String safeBookId = validateBookId(bookId, callback);
        if (safeBookId == null) return;
        if (rating < 1 || rating > 5) {
            callback.onError(new IllegalArgumentException("Choose a whole-star rating from 1 to 5."));
            return;
        }
        String cleanComment = trimToNull(comment);
        if (cleanComment != null && cleanComment.length() > 1000) {
            callback.onError(new IllegalArgumentException("Review comments cannot exceed 1000 characters."));
            return;
        }
        backend.upsertReview(safeBookId, rating, cleanComment, callback);
    }

    @Override
    public void deleteReview(String bookId, RepositoryCallback<ReviewMutationResult> callback) {
        String safeBookId = validateBookId(bookId, callback);
        if (safeBookId != null) backend.deleteReview(safeBookId, callback);
    }

    @Override
    public void cancelPendingRequests() {
        if (backend != null) backend.cancelPendingRequests();
    }

    private <T> String validateBookId(String bookId, RepositoryCallback<T> callback) {
        if (!configured || backend == null) {
            callback.onError(FirebaseConfig.missingConfigException());
            return null;
        }
        String safe = trimToNull(bookId);
        if (safe == null) callback.onError(new IllegalArgumentException("Missing audiobook id."));
        return safe;
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String clean = value.trim();
        return clean.isEmpty() ? null : clean;
    }
}
