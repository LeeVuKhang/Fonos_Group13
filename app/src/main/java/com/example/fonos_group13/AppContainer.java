package com.example.fonos_group13;

import android.os.Handler;

import com.example.fonos_group13.data.creator.CreatorAudiobookRepository;
import com.example.fonos_group13.data.notification.UploadNotificationTokenRepository;
import com.example.fonos_group13.data.repository.AudioDownloadRepository;
import com.example.fonos_group13.data.repository.AuthRepository;
import com.example.fonos_group13.data.repository.CatalogRepository;
import com.example.fonos_group13.data.repository.ProgressRepository;
import com.example.fonos_group13.data.repository.SavedBooksRepository;

import java.util.concurrent.ExecutorService;

/** Application-scoped dependency graph used by the MVC controllers and Android components. */
public interface AppContainer {
    AuthRepository authRepository();

    CatalogRepository catalogRepository();

    SavedBooksRepository savedBooksRepository();

    ProgressRepository progressRepository();

    AudioDownloadRepository audioDownloadRepository();

    CreatorAudiobookRepository creatorAudiobookRepository();

    UploadNotificationTokenRepository uploadNotificationTokenRepository();

    ExecutorService ioExecutor();

    Handler mainHandler();
}
