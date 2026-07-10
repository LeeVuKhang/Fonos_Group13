package com.example.fonos_group13.notifications;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.example.fonos_group13.data.core.RepositoryCallback;
import com.example.fonos_group13.data.notification.UploadNotificationTokenRepository;

public final class GenerationNotificationSetup {
    public static final int REQUEST_POST_NOTIFICATIONS = 4102;

    private static final String TAG = "GenerationNotifySetup";

    private final Activity activity;
    private final UploadNotificationTokenRepository tokenRepository;
    private boolean tokenRegistrationAttempted;
    private boolean permissionRequested;

    public GenerationNotificationSetup(Activity activity) {
        this.activity = activity;
        this.tokenRepository = new UploadNotificationTokenRepository(activity);
    }

    public void ensureReady() {
        registerNotificationTokenOnce();
        requestNotificationPermissionOnce();
    }

    private void registerNotificationTokenOnce() {
        if (tokenRegistrationAttempted) {
            return;
        }
        tokenRegistrationAttempted = true;
        tokenRepository.registerCurrentDevice(new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                Log.d(TAG, "Generation notification token registered.");
            }

            @Override
            public void onError(Exception exception) {
                Log.w(TAG, "Could not register generation notification token.", exception);
            }
        });
    }

    private void requestNotificationPermissionOnce() {
        if (Build.VERSION.SDK_INT < 33 || permissionRequested) {
            return;
        }
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        permissionRequested = true;
        activity.requestPermissions(
                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                REQUEST_POST_NOTIFICATIONS
        );
    }
}
