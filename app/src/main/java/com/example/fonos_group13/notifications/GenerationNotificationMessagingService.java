package com.example.fonos_group13.notifications;

import com.example.fonos_group13.data.core.RepositoryCallback;
import com.example.fonos_group13.data.notification.UploadNotificationTokenRepository;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class GenerationNotificationMessagingService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage message) {
        super.onMessageReceived(message);
        if (message != null) {
            GenerationNotificationHelper.showGenerationStatusNotification(this, message.getData());
        }
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        new UploadNotificationTokenRepository(this).saveToken(token, new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                // Token refresh is best-effort; no foreground UI is available here.
            }

            @Override
            public void onError(Exception exception) {
                // Token refresh is retried from My Uploads when generation is requested or pending.
            }
        });
    }
}
