package com.example.fonos_group13.data.community;

import static org.junit.Assert.assertEquals;

import com.example.fonos_group13.data.core.RepositoryCallback;
import com.example.fonos_group13.model.BookReviewPage;
import com.example.fonos_group13.model.ReviewMutationResult;
import com.example.fonos_group13.model.SaveMutationResult;

import org.junit.Test;

public class DefaultBookCommunityRepositoryTest {
    @Test
    public void delegatesValidatedReviewOperations() {
        RecordingBackend backend = new RecordingBackend();
        DefaultBookCommunityRepository repository = new DefaultBookCommunityRepository(true, backend);

        repository.getReviews(" book-1 ", null, new NoOpCallback<>());
        repository.upsertReview("book-1", 5, " Great ", new NoOpCallback<>());
        repository.deleteReview("book-1", new NoOpCallback<>());

        assertEquals("book-1", backend.bookId);
        assertEquals(5, backend.rating);
        assertEquals("Great", backend.comment);
    }

    @Test
    public void rejectsHalfStarsAndLongCommentsBeforeNetwork() {
        RecordingBackend backend = new RecordingBackend();
        DefaultBookCommunityRepository repository = new DefaultBookCommunityRepository(true, backend);
        RecordingCallback<ReviewMutationResult> callback = new RecordingCallback<>();

        repository.upsertReview("book-1", 4, "x".repeat(1001), callback);

        assertEquals(0, backend.calls);
        assertEquals(IllegalArgumentException.class, callback.error.getClass());
    }

    private static final class RecordingBackend implements CommunityBackendDataSource {
        String bookId;
        int rating;
        String comment;
        int calls;

        @Override public void getReviews(String bookId, String cursor, RepositoryCallback<BookReviewPage> callback) {
            this.bookId = bookId;
            calls++;
        }
        @Override public void upsertReview(String bookId, int rating, String comment, RepositoryCallback<ReviewMutationResult> callback) {
            this.bookId = bookId;
            this.rating = rating;
            this.comment = comment;
            calls++;
        }
        @Override public void deleteReview(String bookId, RepositoryCallback<ReviewMutationResult> callback) {
            this.bookId = bookId;
            calls++;
        }
        @Override public void setSaved(String bookId, boolean saved, RepositoryCallback<SaveMutationResult> callback) {
            calls++;
        }
    }

    private static class NoOpCallback<T> implements RepositoryCallback<T> {
        @Override public void onSuccess(T data) { }
        @Override public void onError(Exception exception) { }
    }

    private static final class RecordingCallback<T> extends NoOpCallback<T> {
        Exception error;
        @Override public void onError(Exception exception) { error = exception; }
    }
}
