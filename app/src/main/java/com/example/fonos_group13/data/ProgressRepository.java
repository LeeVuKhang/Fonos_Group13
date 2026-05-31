package com.example.fonos_group13.data;

import android.content.Context;

import com.example.fonos_group13.model.UserProgress;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class ProgressRepository {
    private final boolean configured;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    public ProgressRepository(Context context) {
        configured = FirebaseConfig.isConfigured(context);
        if (configured) {
            auth = FirebaseAuth.getInstance();
            firestore = FirebaseFirestore.getInstance();
        }
    }

    public void getProgress(String bookId, RepositoryCallback<UserProgress> callback) {
        FirebaseUser user = auth == null ? null : auth.getCurrentUser();
        if (!configured || firestore == null || user == null) {
            callback.onSuccess(UserProgress.empty(bookId));
            return;
        }

        firestore.collection("users")
                .document(user.getUid())
                .collection("progress")
                .document(bookId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        callback.onSuccess(UserProgress.fromDocument(bookId, document));
                    } else {
                        callback.onSuccess(UserProgress.empty(bookId));
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    public void saveProgress(String bookId, long positionMs, long durationMs) {
        FirebaseUser user = auth == null ? null : auth.getCurrentUser();
        if (!configured || firestore == null || user == null || bookId == null) {
            return;
        }

        Map<String, Object> progress = new HashMap<>();
        progress.put("positionMs", Math.max(positionMs, 0));
        progress.put("durationMs", Math.max(durationMs, 0));
        progress.put("completed", durationMs > 0 && positionMs >= durationMs * 0.95f);
        progress.put("updatedAt", FieldValue.serverTimestamp());

        firestore.collection("users")
                .document(user.getUid())
                .collection("progress")
                .document(bookId)
                .set(progress, SetOptions.merge());
    }
}
