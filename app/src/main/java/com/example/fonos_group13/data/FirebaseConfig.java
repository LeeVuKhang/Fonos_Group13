package com.example.fonos_group13.data;

import android.content.Context;

import com.google.firebase.FirebaseApp;

public final class FirebaseConfig {
    private FirebaseConfig() {
    }

    public static boolean isConfigured(Context context) {
        try {
            Context appContext = context.getApplicationContext();
            if (FirebaseApp.getApps(appContext).isEmpty()) {
                return FirebaseApp.initializeApp(appContext) != null;
            }
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public static IllegalStateException missingConfigException() {
        return new IllegalStateException("Firebase is not configured. Add app/google-services.json from Firebase Console.");
    }
}
