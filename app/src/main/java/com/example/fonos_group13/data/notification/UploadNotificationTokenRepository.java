package com.example.fonos_group13.data.notification;

import android.content.Context;

import com.example.fonos_group13.data.core.FirebaseConfig;
import com.example.fonos_group13.data.core.RepositoryCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessaging;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class UploadNotificationTokenRepository {
    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_NOTIFICATION_TOKENS = "notificationTokens";

    private final boolean configured;
    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;

    public UploadNotificationTokenRepository(Context context) {
        configured = FirebaseConfig.isConfigured(context);
        if (configured) {
            auth = FirebaseAuth.getInstance();
            firestore = FirebaseFirestore.getInstance();
        } else {
            auth = null;
            firestore = null;
        }
    }

    public void registerCurrentDevice(RepositoryCallback<Void> callback) {
        if (!canSave(callback)) {
            return;
        }

        FirebaseMessaging.getInstance()
                .getToken()
                .addOnSuccessListener(token -> saveToken(token, callback))
                .addOnFailureListener(callback::onError);
    }

    public void saveToken(String token, RepositoryCallback<Void> callback) {
        if (!canSave(callback)) {
            return;
        }
        String uid = currentUserUid();
        String safeToken = trimToNull(token);
        if (safeToken == null) {
            callback.onError(new IllegalArgumentException("Missing notification token."));
            return;
        }

        Map<String, Object> tokenData = new HashMap<>();
        tokenData.put("token", safeToken);
        tokenData.put("platform", "android");
        tokenData.put("updatedAt", FieldValue.serverTimestamp());

        firestore.collection(COLLECTION_USERS)
                .document(uid)
                .collection(COLLECTION_NOTIFICATION_TOKENS)
                .document(documentIdForToken(safeToken))
                .set(tokenData, SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    public static String documentIdForToken(String token) {
        String safeToken = token == null ? "" : token;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(safeToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }

    private boolean canSave(RepositoryCallback<?> callback) {
        if (!configured || auth == null || firestore == null) {
            callback.onError(FirebaseConfig.missingConfigException());
            return false;
        }
        if (currentUserUid() == null) {
            callback.onError(new IllegalStateException("Please sign in to receive upload notifications."));
            return false;
        }
        return true;
    }

    private String currentUserUid() {
        return auth.getCurrentUser() == null ? null : trimToNull(auth.getCurrentUser().getUid());
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
