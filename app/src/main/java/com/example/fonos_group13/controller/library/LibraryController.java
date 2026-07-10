package com.example.fonos_group13.controller.library;

import com.example.fonos_group13.controller.core.RequestGate;
import com.example.fonos_group13.data.core.RepositoryCallback;
import com.example.fonos_group13.data.repository.CatalogRepository;
import com.example.fonos_group13.data.repository.ProgressRepository;
import com.example.fonos_group13.data.repository.SavedBooksRepository;
import com.example.fonos_group13.model.Book;
import com.example.fonos_group13.model.BookChapter;
import com.example.fonos_group13.model.ProgressKey;
import com.example.fonos_group13.model.UserProgress;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class LibraryController {
    public interface View {
        void renderLibrary(LibraryViewState state);
    }

    private final SavedBooksRepository savedBooksRepository;
    private final CatalogRepository catalogRepository;
    private final ProgressRepository progressRepository;
    private final View view;
    private final RequestGate requestGate = new RequestGate();

    public LibraryController(
            SavedBooksRepository savedBooksRepository,
            CatalogRepository catalogRepository,
            ProgressRepository progressRepository,
            View view
    ) {
        this.savedBooksRepository = savedBooksRepository;
        this.catalogRepository = catalogRepository;
        this.progressRepository = progressRepository;
        this.view = view;
    }

    public void start() {
        long request = requestGate.open();
        view.renderLibrary(LibraryViewState.loading());
        savedBooksRepository.getSavedBookIds(new RepositoryCallback<List<String>>() {
            @Override
            public void onSuccess(List<String> ids) {
                if (!requestGate.isCurrent(request)) {
                    return;
                }
                Set<String> savedIds = new HashSet<>(ids == null ? Collections.emptyList() : ids);
                if (savedIds.isEmpty()) {
                    view.renderLibrary(LibraryViewState.ready(
                            Collections.emptyList(),
                            Collections.emptyMap(),
                            Collections.emptyMap(),
                            false
                    ));
                    return;
                }
                loadSavedBooks(request, savedIds);
            }

            @Override
            public void onError(Exception exception) {
                renderError(request, exception);
            }
        });
    }

    public void stop() {
        requestGate.invalidate();
    }

    private void loadSavedBooks(long request, Set<String> savedIds) {
        catalogRepository.getPublishedBooks(new RepositoryCallback<List<Book>>() {
            @Override
            public void onSuccess(List<Book> books) {
                if (!requestGate.isCurrent(request)) {
                    return;
                }
                List<Book> savedBooks = new ArrayList<>();
                for (Book book : books == null ? Collections.<Book>emptyList() : books) {
                    if (savedIds.contains(book.getId())) {
                        savedBooks.add(book);
                    }
                }
                loadProgress(request, savedBooks);
            }

            @Override
            public void onError(Exception exception) {
                renderError(request, exception);
            }
        });
    }

    private void loadProgress(long request, List<Book> books) {
        progressRepository.getAllProgress(new RepositoryCallback<Map<ProgressKey, UserProgress>>() {
            @Override
            public void onSuccess(Map<ProgressKey, UserProgress> progress) {
                loadChapters(request, books, progress, false);
            }

            @Override
            public void onError(Exception exception) {
                loadChapters(request, books, Collections.emptyMap(), true);
            }
        });
    }

    private void loadChapters(
            long request,
            List<Book> books,
            Map<ProgressKey, UserProgress> progress,
            boolean initiallyPartial
    ) {
        if (!requestGate.isCurrent(request)) {
            return;
        }
        if (books.isEmpty()) {
            view.renderLibrary(LibraryViewState.ready(
                    books,
                    Collections.emptyMap(),
                    progress,
                    initiallyPartial
            ));
            return;
        }
        Map<String, List<BookChapter>> chaptersByBookId = new HashMap<>();
        int[] remaining = {books.size()};
        boolean[] partial = {initiallyPartial};
        for (Book book : books) {
            catalogRepository.getChapters(book, new RepositoryCallback<List<BookChapter>>() {
                @Override
                public void onSuccess(List<BookChapter> chapters) {
                    if (!requestGate.isCurrent(request)) {
                        return;
                    }
                    chaptersByBookId.put(
                            book.getId(),
                            chapters == null ? Collections.emptyList() : chapters
                    );
                    finishChapterLoad(request, books, chaptersByBookId, progress, remaining, partial);
                }

                @Override
                public void onError(Exception exception) {
                    if (!requestGate.isCurrent(request)) {
                        return;
                    }
                    partial[0] = true;
                    chaptersByBookId.put(
                            book.getId(),
                            Collections.singletonList(BookChapter.fromLegacyBook(book))
                    );
                    finishChapterLoad(request, books, chaptersByBookId, progress, remaining, partial);
                }
            });
        }
    }

    private void finishChapterLoad(
            long request,
            List<Book> books,
            Map<String, List<BookChapter>> chapters,
            Map<ProgressKey, UserProgress> progress,
            int[] remaining,
            boolean[] partial
    ) {
        remaining[0]--;
        if (remaining[0] == 0 && requestGate.isCurrent(request)) {
            view.renderLibrary(LibraryViewState.ready(books, chapters, progress, partial[0]));
        }
    }

    private void renderError(long request, Exception exception) {
        if (requestGate.isCurrent(request)) {
            view.renderLibrary(LibraryViewState.error(exception));
        }
    }
}
