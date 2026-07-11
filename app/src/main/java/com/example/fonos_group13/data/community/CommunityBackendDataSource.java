package com.example.fonos_group13.data.community;

import com.example.fonos_group13.data.core.RepositoryCallback;
import com.example.fonos_group13.model.BookReviewPage;
import com.example.fonos_group13.model.ReviewMutationResult;
import com.example.fonos_group13.model.SaveMutationResult;

public interface CommunityBackendDataSource {
    default void cancelPendingRequests() { }
    void getReviews(String bookId, String cursor, RepositoryCallback<BookReviewPage> callback);
    void upsertReview(String bookId, int rating, String comment, RepositoryCallback<ReviewMutationResult> callback);
    void deleteReview(String bookId, RepositoryCallback<ReviewMutationResult> callback);
    void setSaved(String bookId, boolean saved, RepositoryCallback<SaveMutationResult> callback);
}
