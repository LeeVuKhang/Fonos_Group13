package com.example.fonos_group13.controller;

import static org.junit.Assert.assertEquals;

import com.example.fonos_group13.controller.library.LibraryController;
import com.example.fonos_group13.controller.library.LibraryViewState;
import com.example.fonos_group13.data.catalog.BookAccessMode;
import com.example.fonos_group13.data.core.RepositoryCallback;
import com.example.fonos_group13.data.repository.CatalogRepository;
import com.example.fonos_group13.data.repository.ProgressRepository;
import com.example.fonos_group13.data.repository.SavedBooksRepository;
import com.example.fonos_group13.model.AudiobookGenerationStatus;
import com.example.fonos_group13.model.Book;
import com.example.fonos_group13.model.BookChapter;
import com.example.fonos_group13.model.CatalogSnapshot;
import com.example.fonos_group13.model.ProgressKey;
import com.example.fonos_group13.model.UserProgress;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class LibraryControllerTest {
    @Test
    public void loadsProgressOnceAndChaptersOnlyForSavedBooks() {
        FakeSavedBooksRepository saved = new FakeSavedBooksRepository();
        saved.ids = Collections.singletonList("saved");
        FakeCatalogRepository catalog = new FakeCatalogRepository();
        catalog.books = java.util.Arrays.asList(book("saved"), book("other"));
        FakeProgressRepository progress = new FakeProgressRepository();
        CapturingView view = new CapturingView();

        new LibraryController(saved, catalog, progress, view).start();

        assertEquals(1, catalog.publishedBookCalls);
        assertEquals(1, catalog.chapterCalls);
        assertEquals(1, progress.allProgressCalls);
        assertEquals(LibraryViewState.Status.READY, view.lastState.getStatus());
        assertEquals(1, view.lastState.getBooks().size());
    }

    @Test
    public void stoppedControllerIgnoresLateSavedBookResult() {
        DeferredSavedBooksRepository saved = new DeferredSavedBooksRepository();
        FakeCatalogRepository catalog = new FakeCatalogRepository();
        FakeProgressRepository progress = new FakeProgressRepository();
        CapturingView view = new CapturingView();
        LibraryController controller = new LibraryController(saved, catalog, progress, view);

        controller.start();
        controller.stop();
        saved.callback.onSuccess(Collections.singletonList("saved"));

        assertEquals(1, view.states.size());
        assertEquals(LibraryViewState.Status.LOADING, view.lastState.getStatus());
        assertEquals(0, catalog.publishedBookCalls);
    }

    private Book book(String id) {
        return new Book(
                id, id, "Author", "Chapter 1", "", null, null, null, null,
                0, "en-US", "female", null, AudiobookGenerationStatus.PUBLISHED,
                false, true, 0
        );
    }

    private static class CapturingView implements LibraryController.View {
        final List<LibraryViewState> states = new ArrayList<>();
        LibraryViewState lastState;

        @Override
        public void renderLibrary(LibraryViewState state) {
            states.add(state);
            lastState = state;
        }
    }

    private static class FakeSavedBooksRepository implements SavedBooksRepository {
        List<String> ids = Collections.emptyList();

        @Override
        public void getSavedBookIds(RepositoryCallback<List<String>> callback) {
            callback.onSuccess(ids);
        }

        @Override public void isSaved(String bookId, RepositoryCallback<Boolean> callback) { throw new UnsupportedOperationException(); }
        @Override public void saveBook(String bookId, RepositoryCallback<Void> callback) { throw new UnsupportedOperationException(); }
        @Override public void unsaveBook(String bookId, RepositoryCallback<Void> callback) { throw new UnsupportedOperationException(); }
    }

    private static final class DeferredSavedBooksRepository extends FakeSavedBooksRepository {
        RepositoryCallback<List<String>> callback;

        @Override
        public void getSavedBookIds(RepositoryCallback<List<String>> callback) {
            this.callback = callback;
        }
    }

    private static class FakeCatalogRepository implements CatalogRepository {
        List<Book> books = Collections.emptyList();
        int publishedBookCalls;
        int chapterCalls;

        @Override
        public void getPublishedBooks(RepositoryCallback<List<Book>> callback) {
            publishedBookCalls++;
            callback.onSuccess(books);
        }

        @Override
        public void getChapters(Book book, RepositoryCallback<List<BookChapter>> callback) {
            chapterCalls++;
            callback.onSuccess(Collections.singletonList(BookChapter.fromLegacyBook(book)));
        }

        @Override public void getBook(String bookId, RepositoryCallback<Book> callback) { throw new UnsupportedOperationException(); }
        @Override public void getBook(String bookId, BookAccessMode accessMode, RepositoryCallback<Book> callback) { throw new UnsupportedOperationException(); }
        @Override public void getChapters(String bookId, RepositoryCallback<List<BookChapter>> callback) { throw new UnsupportedOperationException(); }
        @Override public void getChapters(String bookId, BookAccessMode accessMode, RepositoryCallback<List<BookChapter>> callback) { throw new UnsupportedOperationException(); }
        @Override public void getPublishedCatalog(RepositoryCallback<CatalogSnapshot> callback) { throw new UnsupportedOperationException(); }
    }

    private static class FakeProgressRepository implements ProgressRepository {
        int allProgressCalls;

        @Override
        public void getAllProgress(RepositoryCallback<Map<ProgressKey, UserProgress>> callback) {
            allProgressCalls++;
            callback.onSuccess(Collections.emptyMap());
        }

        @Override public void getProgress(String bookId, RepositoryCallback<UserProgress> callback) { throw new UnsupportedOperationException(); }
        @Override public void getProgress(String bookId, String chapterId, RepositoryCallback<UserProgress> callback) { throw new UnsupportedOperationException(); }
        @Override public void saveProgress(String bookId, long positionMs, long durationMs) { throw new UnsupportedOperationException(); }
        @Override public void saveProgress(String bookId, String chapterId, long positionMs, long durationMs) { throw new UnsupportedOperationException(); }
        @Override public void saveProgress(String bookId, String chapterId, long positionMs, long durationMs, RepositoryCallback<Void> callback) { throw new UnsupportedOperationException(); }
    }
}
