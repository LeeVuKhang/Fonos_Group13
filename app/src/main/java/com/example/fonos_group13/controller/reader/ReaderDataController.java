package com.example.fonos_group13.controller.reader;

import android.net.Uri;

import com.example.fonos_group13.audio.AudioSourceResolver;
import com.example.fonos_group13.controller.core.RequestGate;
import com.example.fonos_group13.data.catalog.BookAccessMode;
import com.example.fonos_group13.data.core.RepositoryCallback;
import com.example.fonos_group13.data.core.RequestHandle;
import com.example.fonos_group13.data.repository.AudioDownloadRepository;
import com.example.fonos_group13.data.repository.AuthRepository;
import com.example.fonos_group13.data.repository.CatalogRepository;
import com.example.fonos_group13.data.repository.ProgressRepository;
import com.example.fonos_group13.model.Book;
import com.example.fonos_group13.model.BookChapter;
import com.example.fonos_group13.model.UserAccount;
import com.example.fonos_group13.model.UserProgress;

import java.io.File;
import java.util.List;

public final class ReaderDataController {
    private final CatalogRepository catalogRepository;
    private final ProgressRepository progressRepository;
    private final AudioDownloadRepository audioRepository;
    private final AuthRepository authRepository;
    private final AudioSourceResolver audioSourceResolver;
    private final RequestGate requestGate = new RequestGate();
    private RequestHandle downloadRequest = RequestHandle.NONE;
    private long generation;

    public ReaderDataController(
            CatalogRepository catalogRepository,
            ProgressRepository progressRepository,
            AudioDownloadRepository audioRepository,
            AuthRepository authRepository
    ) {
        this.catalogRepository = catalogRepository;
        this.progressRepository = progressRepository;
        this.audioRepository = audioRepository;
        this.authRepository = authRepository;
        audioSourceResolver = new AudioSourceResolver(audioRepository);
    }

    public void start() {
        generation = requestGate.open();
    }

    public void stop() {
        requestGate.invalidate();
        downloadRequest.cancel();
        downloadRequest = RequestHandle.NONE;
    }

    public void getBook(String id, BookAccessMode mode, RepositoryCallback<Book> callback) {
        catalogRepository.getBook(id, mode, guarded(callback));
    }

    public void getChapters(
            String id,
            BookAccessMode mode,
            RepositoryCallback<List<BookChapter>> callback
    ) {
        catalogRepository.getChapters(id, mode, guarded(callback));
    }

    public void getProgress(String bookId, String chapterId, RepositoryCallback<UserProgress> callback) {
        progressRepository.getProgress(bookId, chapterId, guarded(callback));
    }

    public void saveProgress(String bookId, String chapterId, long positionMs, long durationMs) {
        progressRepository.saveProgress(bookId, chapterId, positionMs, durationMs);
    }

    public boolean isDownloaded(String bookId, String chapterId) {
        return audioRepository.isDownloaded(bookId, chapterId);
    }

    public void download(Book book, BookChapter chapter, RepositoryCallback<File> callback) {
        downloadRequest.cancel();
        downloadRequest = audioRepository.download(book, chapter, guarded(callback));
    }

    public Uri resolve(Book book, BookChapter chapter) {
        return audioSourceResolver.resolve(book, chapter);
    }

    public boolean isCurrentCreator(Book book) {
        UserAccount user = authRepository.getCurrentUser();
        return book != null && user != null && user.getUid() != null
                && user.getUid().equals(book.getCreatorUid());
    }

    private <T> RepositoryCallback<T> guarded(RepositoryCallback<T> callback) {
        long requestGeneration = generation;
        return new RepositoryCallback<T>() {
            @Override
            public void onSuccess(T data) {
                if (requestGate.isCurrent(requestGeneration)) {
                    callback.onSuccess(data);
                }
            }

            @Override
            public void onError(Exception exception) {
                if (requestGate.isCurrent(requestGeneration)) {
                    callback.onError(exception);
                }
            }
        };
    }
}
