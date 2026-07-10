package com.example.fonos_group13.data.library;

import android.net.Uri;
import android.os.Handler;
import android.text.TextUtils;

import com.example.fonos_group13.data.core.RepositoryCallback;
import com.example.fonos_group13.data.core.RequestHandle;
import com.example.fonos_group13.data.repository.AudioDownloadRepository;
import com.example.fonos_group13.model.Book;
import com.example.fonos_group13.model.BookChapter;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DefaultAudioDownloadRepository implements AudioDownloadRepository {
    private final DownloadedAudioStore store;
    private final HttpAudioDownloader downloader;
    private final ExecutorService executor;
    private final Handler mainHandler;

    public DefaultAudioDownloadRepository(
            DownloadedAudioStore store,
            HttpAudioDownloader downloader,
            ExecutorService executor,
            Handler mainHandler
    ) {
        this.store = store;
        this.downloader = downloader;
        this.executor = executor;
        this.mainHandler = mainHandler;
    }

    @Override public boolean isDownloaded(String bookId) { return isDownloaded(bookId, BookChapter.LEGACY_CHAPTER_ID); }
    @Override public boolean isDownloaded(String bookId, String chapterId) { return store.isDownloaded(bookId, chapterId); }
    @Override public Uri getDownloadedUri(String bookId) { return getDownloadedUri(bookId, BookChapter.LEGACY_CHAPTER_ID); }
    @Override public Uri getDownloadedUri(String bookId, String chapterId) { return store.downloadedUri(bookId, chapterId); }
    @Override public long getDownloadedSizeBytes(String bookId) { return getDownloadedSizeBytes(bookId, BookChapter.LEGACY_CHAPTER_ID); }
    @Override public long getDownloadedSizeBytes(String bookId, String chapterId) { return store.downloadedSize(bookId, chapterId); }
    @Override public boolean deleteDownloadedAudio(String bookId) { return deleteDownloadedAudio(bookId, BookChapter.LEGACY_CHAPTER_ID); }
    @Override public boolean deleteDownloadedAudio(String bookId, String chapterId) { return store.delete(bookId, chapterId); }

    @Override
    public RequestHandle download(Book book, RepositoryCallback<File> callback) {
        return download(book, BookChapter.fromLegacyBook(book), callback);
    }

    @Override
    public RequestHandle download(Book book, BookChapter chapter, RepositoryCallback<File> callback) {
        AtomicBoolean cancelled = new AtomicBoolean(false);
        File[] temporaryFile = new File[1];
        Future<?> future = executor.submit(() -> {
            try {
                validate(book, chapter);
                if (!store.ensureDirectory()) {
                    throw new IOException("Could not create audio download folder.");
                }
                File temp = store.temporaryFile(book.getId(), chapter.getId());
                temporaryFile[0] = temp;
                downloader.download(chapter.getAudioUrl().trim(), temp);
                File target = store.audioFile(book.getId(), chapter.getId());
                if (!store.replaceFromTemp(temp, target)) {
                    throw new IOException("Could not save downloaded audio file.");
                }
                postSuccess(callback, target, cancelled);
            } catch (Exception exception) {
                postError(callback, exception, cancelled);
            } finally {
                File temp = temporaryFile[0];
                if (temp != null && temp.exists()) {
                    temp.delete();
                }
            }
        });
        return new RequestHandle() {
            @Override
            public void cancel() {
                if (cancelled.compareAndSet(false, true)) {
                    future.cancel(true);
                    File temp = temporaryFile[0];
                    if (temp != null) {
                        temp.delete();
                    }
                }
            }

            @Override
            public boolean isCancelled() {
                return cancelled.get();
            }
        };
    }

    private void validate(Book book, BookChapter chapter) throws IOException {
        if (book == null || TextUtils.isEmpty(book.getId())) {
            throw new IOException("Book is missing.");
        }
        if (chapter == null || TextUtils.isEmpty(chapter.getId())) {
            throw new IOException("Chapter is missing.");
        }
        if (TextUtils.isEmpty(chapter.getAudioUrl())) {
            throw new IOException("This chapter does not have an audioUrl to download.");
        }
    }

    private void postSuccess(
            RepositoryCallback<File> callback,
            File value,
            AtomicBoolean cancelled
    ) {
        mainHandler.post(() -> {
            if (!cancelled.get()) {
                callback.onSuccess(value);
            }
        });
    }

    private void postError(
            RepositoryCallback<File> callback,
            Exception exception,
            AtomicBoolean cancelled
    ) {
        mainHandler.post(() -> {
            if (!cancelled.get()) {
                callback.onError(exception);
            }
        });
    }
}
