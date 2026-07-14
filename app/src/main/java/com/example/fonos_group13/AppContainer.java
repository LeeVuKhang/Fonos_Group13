package com.example.fonos_group13;

import android.os.Handler;

import com.example.fonos_group13.data.notification.UploadNotificationTokenRepository;
import com.example.fonos_group13.data.repository.AudioDownloadRepository;
import com.example.fonos_group13.data.repository.AiChatRepository;
import com.example.fonos_group13.data.repository.BookCommunityRepository;
import com.example.fonos_group13.data.repository.AuthRepository;
import com.example.fonos_group13.data.repository.CatalogRepository;
import com.example.fonos_group13.data.repository.CreatorCommandRepository;
import com.example.fonos_group13.data.repository.CreatorUploadsRepository;
import com.example.fonos_group13.data.repository.ProgressRepository;
import com.example.fonos_group13.data.repository.SavedBooksRepository;

import java.util.concurrent.ExecutorService;

/** Application-scoped dependency graph used by the MVC controllers and Android components. */
public interface AppContainer {
    AuthRepository authRepository();

    CatalogRepository catalogRepository();

    SavedBooksRepository savedBooksRepository();

    BookCommunityRepository bookCommunityRepository();

    AiChatRepository aiChatRepository();

    ProgressRepository progressRepository();

    AudioDownloadRepository audioDownloadRepository();

    CreatorCommandRepository creatorCommandRepository();

    CreatorUploadsRepository creatorUploadsRepository();

    UploadNotificationTokenRepository uploadNotificationTokenRepository();

    ExecutorService ioExecutor();

    Handler mainHandler();
}
