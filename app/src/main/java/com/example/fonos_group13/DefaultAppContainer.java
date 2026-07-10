package com.example.fonos_group13;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.fonos_group13.data.creator.CreatorAudiobookRepository;
import com.example.fonos_group13.data.creator.FirestoreCreatorUploadsRepository;
import com.example.fonos_group13.data.notification.UploadNotificationTokenRepository;
import com.example.fonos_group13.data.repository.AudioDownloadRepository;
import com.example.fonos_group13.data.repository.AuthRepository;
import com.example.fonos_group13.data.repository.CatalogRepository;
import com.example.fonos_group13.data.repository.CreatorCommandRepository;
import com.example.fonos_group13.data.repository.CreatorUploadsRepository;
import com.example.fonos_group13.data.repository.ProgressRepository;
import com.example.fonos_group13.data.repository.SavedBooksRepository;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class DefaultAppContainer implements AppContainer {
    private final AuthRepository authRepository;
    private final CatalogRepository catalogRepository;
    private final SavedBooksRepository savedBooksRepository;
    private final ProgressRepository progressRepository;
    private final AudioDownloadRepository audioDownloadRepository;
    private final CreatorCommandRepository creatorCommandRepository;
    private final CreatorUploadsRepository creatorUploadsRepository;
    private final UploadNotificationTokenRepository uploadNotificationTokenRepository;
    private final ExecutorService ioExecutor;
    private final Handler mainHandler;

    DefaultAppContainer(Context context) {
        Context appContext = context.getApplicationContext();
        ioExecutor = Executors.newFixedThreadPool(3);
        mainHandler = new Handler(Looper.getMainLooper());
        authRepository = new com.example.fonos_group13.data.auth.AuthRepository(appContext);
        catalogRepository = new com.example.fonos_group13.data.catalog.BookRepository(appContext);
        savedBooksRepository = new com.example.fonos_group13.data.library.SavedBookRepository(appContext);
        progressRepository = new com.example.fonos_group13.data.library.ProgressRepository(appContext);
        audioDownloadRepository = new com.example.fonos_group13.data.library.DownloadedAudioRepository(appContext);
        creatorCommandRepository = new CreatorAudiobookRepository(appContext, ioExecutor, mainHandler);
        creatorUploadsRepository = new FirestoreCreatorUploadsRepository(appContext);
        uploadNotificationTokenRepository = new UploadNotificationTokenRepository(appContext);
    }

    @Override
    public AuthRepository authRepository() {
        return authRepository;
    }

    @Override
    public CatalogRepository catalogRepository() {
        return catalogRepository;
    }

    @Override
    public SavedBooksRepository savedBooksRepository() {
        return savedBooksRepository;
    }

    @Override
    public ProgressRepository progressRepository() {
        return progressRepository;
    }

    @Override
    public AudioDownloadRepository audioDownloadRepository() {
        return audioDownloadRepository;
    }

    @Override
    public CreatorCommandRepository creatorCommandRepository() {
        return creatorCommandRepository;
    }

    @Override
    public CreatorUploadsRepository creatorUploadsRepository() {
        return creatorUploadsRepository;
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
