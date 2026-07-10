package com.example.fonos_group13.data.auth;

import android.text.TextUtils;

public final class AuthErrorFormatter {
    private AuthErrorFormatter() {
    }

    public static String friendlyMessage(Exception exception) {
        String message = exception == null ? null : exception.getMessage();
        if (TextUtils.isEmpty(message)) {
            return "Something went wrong. Please try again.";
        }
        if (message.contains("google-services.json") || message.contains("Firebase is not configured")) {
            return "Firebase is not configured. Add google-services.json to the app folder.";
        }
        return message;
    }
}
