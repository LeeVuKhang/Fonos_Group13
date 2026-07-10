package com.example.fonos_group13;

import android.os.Handler;

import com.example.fonos_group13.data.auth.AuthRepository;
import com.example.fonos_group13.data.catalog.BookRepository;
import com.example.fonos_group13.data.creator.CreatorAudiobookRepository;
import com.example.fonos_group13.data.library.DownloadedAudioRepository;
import com.example.fonos_group13.data.library.ProgressRepository;
import com.example.fonos_group13.data.library.SavedBookRepository;
import com.example.fonos_group13.data.notification.UploadNotificationTokenRepository;

import java.util.concurrent.ExecutorService;

/** Application-scoped dependency graph used by the MVC controllers and Android components. */
public interface AppContainer {
    AuthRepository authRepository();

    BookRepository bookRepository();

    SavedBookRepository savedBookRepository();

    ProgressRepository progressRepository();

    DownloadedAudioRepository downloadedAudioRepository();

    CreatorAudiobookRepository creatorAudiobookRepository();

    UploadNotificationTokenRepository uploadNotificationTokenRepository();

    ExecutorService ioExecutor();

    Handler mainHandler();
}
