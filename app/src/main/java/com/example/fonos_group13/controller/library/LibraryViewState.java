package com.example.fonos_group13.controller.library;

import com.example.fonos_group13.model.Book;
import com.example.fonos_group13.model.BookChapter;
import com.example.fonos_group13.model.ProgressKey;
import com.example.fonos_group13.model.UserProgress;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class LibraryViewState {
    public enum Status { LOADING, READY, ERROR }

    private final Status status;
    private final List<Book> books;
    private final Map<String, List<BookChapter>> chaptersByBookId;
    private final Map<ProgressKey, UserProgress> progressByChapter;
    private final boolean partial;
    private final Exception error;

    private LibraryViewState(
            Status status,
            List<Book> books,
            Map<String, List<BookChapter>> chaptersByBookId,
            Map<ProgressKey, UserProgress> progressByChapter,
            boolean partial,
            Exception error
    ) {
        this.status = status;
        this.books = Collections.unmodifiableList(new ArrayList<>(books));
        this.chaptersByBookId = Collections.unmodifiableMap(new HashMap<>(chaptersByBookId));
        this.progressByChapter = Collections.unmodifiableMap(new HashMap<>(progressByChapter));
        this.partial = partial;
        this.error = error;
    }

    public static LibraryViewState loading() {
        return new LibraryViewState(
                Status.LOADING,
                Collections.emptyList(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                false,
                null
        );
    }

    public static LibraryViewState ready(
            List<Book> books,
            Map<String, List<BookChapter>> chapters,
            Map<ProgressKey, UserProgress> progress,
            boolean partial
    ) {
        return new LibraryViewState(Status.READY, books, chapters, progress, partial, null);
    }

    public static LibraryViewState error(Exception error) {
        return new LibraryViewState(
                Status.ERROR,
                Collections.emptyList(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                false,
                error
        );
    }

    public Status getStatus() {
        return status;
    }

    public List<Book> getBooks() {
        return books;
    }

    public Map<String, List<BookChapter>> getChaptersByBookId() {
        return chaptersByBookId;
    }

    public Map<ProgressKey, UserProgress> getProgressByChapter() {
        return progressByChapter;
    }

    public boolean isPartial() {
        return partial;
    }

    public Exception getError() {
        return error;
    }
}
