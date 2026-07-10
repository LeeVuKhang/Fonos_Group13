package com.example.fonos_group13.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.fonos_group13.controller.creator.MyUploadsController;
import com.example.fonos_group13.data.core.RepositoryCallback;
import com.example.fonos_group13.data.core.Subscription;
import com.example.fonos_group13.data.repository.CreatorUploadsRepository;
import com.example.fonos_group13.model.AudiobookGenerationStatus;
import com.example.fonos_group13.model.UserGeneratedAudiobook;
import com.example.fonos_group13.model.UserGeneratedChapter;

import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MyUploadsControllerTest {
    @Test
    public void stoppingCancelsBookAndChapterSubscriptions() {
        FakeRepository repository = new FakeRepository();
        CapturingView view = new CapturingView();
        MyUploadsController controller = new MyUploadsController(repository, view);

        controller.start();
        repository.uploadsCallback.onSuccess(Collections.singletonList(upload("book-1")));
        controller.stop();

        assertTrue(repository.uploadsSubscription.cancelled);
        assertTrue(repository.chapterSubscription.cancelled);
        assertEquals(1, view.renderCount);
    }

    private UserGeneratedAudiobook upload(String id) {
        return new UserGeneratedAudiobook(
                id,
                "Title",
                "Author",
                null,
                "en-US",
                "male",
                "Patrick",
                AudiobookGenerationStatus.DRAFT,
                null,
                "pending",
                false,
                false,
                0,
                0,
                null
        );
    }

    private static class CapturingView implements MyUploadsController.View {
        int renderCount;

        @Override public void showUploadsLoading() { }

        @Override
        public void showUploads(
                List<UserGeneratedAudiobook> uploads,
                Map<String, List<UserGeneratedChapter>> chaptersByBookId
        ) {
            renderCount++;
        }

        @Override public void showUploadsError(Exception exception) { }
    }

    private static class FakeRepository implements CreatorUploadsRepository {
        RepositoryCallback<List<UserGeneratedAudiobook>> uploadsCallback;
        final TrackingSubscription uploadsSubscription = new TrackingSubscription();
        final TrackingSubscription chapterSubscription = new TrackingSubscription();

        @Override
        public Subscription observeMyUploads(RepositoryCallback<List<UserGeneratedAudiobook>> callback) {
            uploadsCallback = callback;
            return uploadsSubscription;
        }

        @Override
        public Subscription observeUploadChapters(
                String bookId,
                RepositoryCallback<List<UserGeneratedChapter>> callback
        ) {
            return chapterSubscription;
        }

        @Override
        public void getMyUploads(RepositoryCallback<List<UserGeneratedAudiobook>> callback) {
            throw new UnsupportedOperationException();
        }
    }

    private static class TrackingSubscription implements Subscription {
        boolean cancelled;

        @Override public void cancel() { cancelled = true; }
        @Override public boolean isCancelled() { return cancelled; }
    }
}
