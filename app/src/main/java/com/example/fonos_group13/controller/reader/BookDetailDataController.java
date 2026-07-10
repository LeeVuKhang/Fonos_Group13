package com.example.fonos_group13.controller.reader;

import com.example.fonos_group13.controller.core.RequestGate;
import com.example.fonos_group13.data.catalog.BookAccessMode;
import com.example.fonos_group13.data.core.RepositoryCallback;
import com.example.fonos_group13.data.core.RequestHandle;
import com.example.fonos_group13.data.repository.AudioDownloadRepository;
import com.example.fonos_group13.data.repository.AuthRepository;
import com.example.fonos_group13.data.repository.CatalogRepository;
import com.example.fonos_group13.data.repository.CreatorCommandRepository;
import com.example.fonos_group13.data.repository.ProgressRepository;
import com.example.fonos_group13.data.repository.SavedBooksRepository;
import com.example.fonos_group13.model.Book;
import com.example.fonos_group13.model.BookChapter;
import com.example.fonos_group13.model.UserAccount;
import com.example.fonos_group13.model.UserProgress;

import java.io.File;
import java.util.List;

public final class BookDetailDataController {
    private final CatalogRepository catalog;
    private final ProgressRepository progress;
    private final AudioDownloadRepository audio;
    private final SavedBooksRepository savedBooks;
    private final CreatorCommandRepository creatorCommands;
    private final AuthRepository auth;
    private final RequestGate requestGate = new RequestGate();
    private RequestHandle downloadRequest = RequestHandle.NONE;
    private long generation;

    public BookDetailDataController(
            CatalogRepository catalog,
            ProgressRepository progress,
            AudioDownloadRepository audio,
            SavedBooksRepository savedBooks,
            CreatorCommandRepository creatorCommands,
            AuthRepository auth
    ) {
        this.catalog = catalog;
        this.progress = progress;
        this.audio = audio;
        this.savedBooks = savedBooks;
        this.creatorCommands = creatorCommands;
        this.auth = auth;
    }

    public void start() { generation = requestGate.open(); }

    public void stop() {
        requestGate.invalidate();
        downloadRequest.cancel();
        creatorCommands.cancelPendingRequests();
    }

    public void getBook(String id, BookAccessMode mode, RepositoryCallback<Book> callback) {
        catalog.getBook(id, mode, guarded(callback));
    }

    public void getChapters(String id, BookAccessMode mode, RepositoryCallback<List<BookChapter>> callback) {
        catalog.getChapters(id, mode, guarded(callback));
    }

    public void getProgress(String bookId, String chapterId, RepositoryCallback<UserProgress> callback) {
        progress.getProgress(bookId, chapterId, guarded(callback));
    }

    public void isSaved(String bookId, RepositoryCallback<Boolean> callback) {
        savedBooks.isSaved(bookId, guarded(callback));
    }

    public void setSaved(String bookId, boolean saved, RepositoryCallback<Void> callback) {
        if (saved) {
            savedBooks.saveBook(bookId, guarded(callback));
        } else {
            savedBooks.unsaveBook(bookId, guarded(callback));
        }
    }

    public void publish(String bookId, RepositoryCallback<Void> callback) {
        creatorCommands.publishAudiobook(bookId, guarded(callback));
    }

    public boolean isDownloaded(String bookId, String chapterId) {
        return audio.isDownloaded(bookId, chapterId);
    }

    public void download(Book book, BookChapter chapter, RepositoryCallback<File> callback) {
        downloadRequest.cancel();
        downloadRequest = audio.download(book, chapter, guarded(callback));
    }

    public boolean isCurrentCreator(Book book) {
        UserAccount user = auth.getCurrentUser();
        return book != null && user != null && user.getUid() != null
                && user.getUid().equals(book.getCreatorUid());
    }

    private <T> RepositoryCallback<T> guarded(RepositoryCallback<T> callback) {
        long requestGeneration = generation;
        return new RepositoryCallback<T>() {
            @Override public void onSuccess(T data) {
                if (requestGate.isCurrent(requestGeneration)) callback.onSuccess(data);
            }
            @Override public void onError(Exception exception) {
                if (requestGate.isCurrent(requestGeneration)) callback.onError(exception);
            }
        };
    }
}
