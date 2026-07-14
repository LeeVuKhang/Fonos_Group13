package com.example.fonos_group13;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

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

import java.lang.reflect.Proxy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class TestAppContainer implements AppContainer {
    private final AuthRepository auth = fake(AuthRepository.class);
    private final CatalogRepository catalog = fake(CatalogRepository.class);
    private final SavedBooksRepository savedBooks = fake(SavedBooksRepository.class);
    private final BookCommunityRepository community = fake(BookCommunityRepository.class);
    private final AiChatRepository aiChat = fake(AiChatRepository.class);
    private final ProgressRepository progress = fake(ProgressRepository.class);
    private final AudioDownloadRepository downloads = fake(AudioDownloadRepository.class);
    private final CreatorCommandRepository creatorCommands = fake(CreatorCommandRepository.class);
    private final CreatorUploadsRepository creatorUploads = fake(CreatorUploadsRepository.class);
    private final UploadNotificationTokenRepository notificationTokens;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    TestAppContainer(Context context) {
        notificationTokens = new UploadNotificationTokenRepository(context);
    }

    @SuppressWarnings("unchecked")
    private static <T> T fake(Class<T> contract) {
        return (T) Proxy.newProxyInstance(
                contract.getClassLoader(),
                new Class<?>[]{contract},
                (proxy, method, arguments) -> {
                    Class<?> type = method.getReturnType();
                    if (type == boolean.class) return false;
                    if (type == int.class) return 0;
                    if (type == long.class) return 0L;
                    return null;
                }
        );
    }

    @Override public AuthRepository authRepository() { return auth; }
    @Override public CatalogRepository catalogRepository() { return catalog; }
    @Override public SavedBooksRepository savedBooksRepository() { return savedBooks; }
    @Override public BookCommunityRepository bookCommunityRepository() { return community; }
    @Override public AiChatRepository aiChatRepository() { return aiChat; }
    @Override public ProgressRepository progressRepository() { return progress; }
    @Override public AudioDownloadRepository audioDownloadRepository() { return downloads; }
    @Override public CreatorCommandRepository creatorCommandRepository() { return creatorCommands; }
    @Override public CreatorUploadsRepository creatorUploadsRepository() { return creatorUploads; }
    @Override public UploadNotificationTokenRepository uploadNotificationTokenRepository() { return notificationTokens; }
    @Override public ExecutorService ioExecutor() { return executor; }
    @Override public Handler mainHandler() { return mainHandler; }
}
