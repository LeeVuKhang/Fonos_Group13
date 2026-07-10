package com.example.fonos_group13;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.fonos_group13.data.auth.AuthRepository;
import com.example.fonos_group13.data.catalog.BookRepository;
import com.example.fonos_group13.data.creator.CreatorAudiobookRepository;
import com.example.fonos_group13.data.library.DownloadedAudioRepository;
import com.example.fonos_group13.data.library.ProgressRepository;
import com.example.fonos_group13.data.library.SavedBookRepository;
import com.example.fonos_group13.data.notification.UploadNotificationTokenRepository;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class DefaultAppContainer implements AppContainer {
    private final AuthRepository authRepository;
    private final BookRepository bookRepository;
    private final SavedBookRepository savedBookRepository;
    private final ProgressRepository progressRepository;
    private final DownloadedAudioRepository downloadedAudioRepository;
    private final CreatorAudiobookRepository creatorAudiobookRepository;
    private final UploadNotificationTokenRepository uploadNotificationTokenRepository;
    private final ExecutorService ioExecutor;
    private final Handler mainHandler;

    DefaultAppContainer(Context context) {
        Context appContext = context.getApplicationContext();
        ioExecutor = Executors.newFixedThreadPool(3);
        mainHandler = new Handler(Looper.getMainLooper());
        authRepository = new AuthRepository(appContext);
        bookRepository = new BookRepository(appContext);
        savedBookRepository = new SavedBookRepository(appContext);
        progressRepository = new ProgressRepository(appContext);
        downloadedAudioRepository = new DownloadedAudioRepository(appContext);
        creatorAudiobookRepository = new CreatorAudiobookRepository(appContext);
        uploadNotificationTokenRepository = new UploadNotificationTokenRepository(appContext);
    }

    @Override
    public AuthRepository authRepository() {
        return authRepository;
    }

    @Override
    public BookRepository bookRepository() {
        return bookRepository;
    }

    @Override
    public SavedBookRepository savedBookRepository() {
        return savedBookRepository;
    }

    @Override
    public ProgressRepository progressRepository() {
        return progressRepository;
    }

    @Override
    public DownloadedAudioRepository downloadedAudioRepository() {
        return downloadedAudioRepository;
    }

    @Override
    public CreatorAudiobookRepository creatorAudiobookRepository() {
        return creatorAudiobookRepository;
    }

    @Override
    public UploadNotificationTokenRepository uploadNotificationTokenRepository() {
        return uploadNotificationTokenRepository;
    }

    @Override
    public ExecutorService ioExecutor() {
        return ioExecutor;
    }

    @Override
    public Handler mainHandler() {
        return mainHandler;
    }
}
