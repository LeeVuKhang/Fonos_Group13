package com.example.fonos_group13.data.auth;

import android.content.Context;
import android.text.TextUtils;

import com.example.fonos_group13.data.core.FirebaseConfig;
import com.example.fonos_group13.data.core.RepositoryCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class AuthRepository {
    private final boolean configured;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    public AuthRepository(Context context) {
        configured = FirebaseConfig.isConfigured(context);
        if (configured) {
            auth = FirebaseAuth.getInstance();
            firestore = FirebaseFirestore.getInstance();
        }
    }

    public boolean isConfigured() {
        return configured;
    }

    public FirebaseUser getCurrentUser() {
        return auth == null ? null : auth.getCurrentUser();
    }

    public void signIn(String email, String password, RepositoryCallback<FirebaseUser> callback) {
        if (!configured || auth == null) {
            callback.onError(FirebaseConfig.missingConfigException());
            return;
        }

        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> callback.onSuccess(authResult.getUser()))
                .addOnFailureListener(callback::onError);
    }

    public void register(String email, String password, RepositoryCallback<FirebaseUser> callback) {
        if (!configured || auth == null) {
            callback.onError(FirebaseConfig.missingConfigException());
            return;
        }

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user == null) {
                        callback.onError(new IllegalStateException("Account was created, but no user session was returned."));
                        return;
                    }

                    String displayName = makeDisplayName(email);
                    user.updateProfile(new UserProfileChangeRequest.Builder()
                            .setDisplayName(displayName)
                            .build());

                    Map<String, Object> profile = new HashMap<>();
                    profile.put("email", email);
                    profile.put("displayName", displayName);
                    profile.put("createdAt", FieldValue.serverTimestamp());

                    firestore.collection("users")
                            .document(user.getUid())
                            .set(profile, SetOptions.merge())
                            .addOnSuccessListener(unused -> callback.onSuccess(user))
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    public void updateDisplayName(String displayName, RepositoryCallback<FirebaseUser> callback) {
        String cleanDisplayName = displayName == null ? "" : displayName.trim();
        if (TextUtils.isEmpty(cleanDisplayName)) {
            callback.onError(new IllegalArgumentException("Display name cannot be empty."));
            return;
        }
        if (!configured || auth == null || firestore == null) {
            callback.onError(FirebaseConfig.missingConfigException());
            return;
        }

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError(new IllegalStateException("Please sign in again to update your profile."));
            return;
        }

        user.updateProfile(new UserProfileChangeRequest.Builder()
                        .setDisplayName(cleanDisplayName)
                        .build())
                .addOnSuccessListener(unused -> {
                    Map<String, Object> profile = new HashMap<>();
                    profile.put("displayName", cleanDisplayName);
                    profile.put("updatedAt", FieldValue.serverTimestamp());

                    firestore.collection("users")
                            .document(user.getUid())
                            .set(profile, SetOptions.merge())
                            .addOnSuccessListener(saved -> callback.onSuccess(user))
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    public void signOut() {
        if (auth != null) {
            auth.signOut();
        }
    }

    public static String friendlyError(Exception exception) {
        String message = exception == null ? null : exception.getMessage();
        if (TextUtils.isEmpty(message)) {
            return "Something went wrong. Please try again.";
        }
        if (message.contains("google-services.json") || message.contains("Firebase is not configured")) {
            return "Firebase is not configured. Add google-services.json to the app folder.";
        }
        return message;
    }

    private static String makeDisplayName(String email) {
        int atIndex = email == null ? -1 : email.indexOf('@');
        if (atIndex <= 0) {
            return "Reader";
        }
        return email.substring(0, atIndex);
    }
}
