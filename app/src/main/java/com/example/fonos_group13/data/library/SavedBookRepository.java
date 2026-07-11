package com.example.fonos_group13.data.library;

import android.content.Context;
import android.os.Handler;

import com.example.fonos_group13.data.core.FirebaseConfig;
import com.example.fonos_group13.data.core.RepositoryCallback;
import com.example.fonos_group13.BuildConfig;
import com.example.fonos_group13.data.community.CommunityApiClient;
import com.example.fonos_group13.data.community.CommunityBackendDataSource;
import com.example.fonos_group13.model.SaveMutationResult;
import com.example.fonos_group13.data.firestore.FirestoreValueReader;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class SavedBookRepository implements com.example.fonos_group13.data.repository.SavedBooksRepository {
    private final boolean configured;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private CommunityBackendDataSource backend;

    public SavedBookRepository(Context context) {
        this(context, null, null);
    }

    public SavedBookRepository(Context context, ExecutorService executor, Handler mainHandler) {
        configured = FirebaseConfig.isConfigured(context);
        if (configured) {
            auth = FirebaseAuth.getInstance();
            firestore = FirebaseFirestore.getInstance();
            if (executor != null && mainHandler != null) {
                backend = new CommunityApiClient(BuildConfig.BACKEND_BASE_URL, auth, executor, mainHandler);
            }
        }
    }

    public void isSaved(String bookId, RepositoryCallback<Boolean> callback) {
        FirebaseUser user = currentUser();
        if (!configured || firestore == null || user == null || isBlank(bookId)) {
            callback.onSuccess(false);
            return;
        }

        firestore.collection("users")
                .document(user.getUid())
                .collection("savedBooks")
                .document(bookId)
                .get()
                .addOnSuccessListener(document -> callback.onSuccess(document.exists()))
                .addOnFailureListener(callback::onError);
    }

    public void saveBook(String bookId, RepositoryCallback<Void> callback) {
        setSavedWithResult(bookId, true, voidCallback(callback));
    }

    public void unsaveBook(String bookId, RepositoryCallback<Void> callback) {
        setSavedWithResult(bookId, false, voidCallback(callback));
    }

    @Override
    public void setSavedWithResult(String bookId, boolean saved, RepositoryCallback<SaveMutationResult> callback) {
        if (!canWrite(bookId, callback)) return;
        if (backend == null) {
            callback.onError(new IllegalStateException("Backend base URL is not configured."));
            return;
        }
        backend.setSaved(bookId, saved, callback);
    }

    public void getSavedBookIds(RepositoryCallback<List<String>> callback) {
        FirebaseUser user = currentUser();
        if (!configured || firestore == null || user == null) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        firestore.collection("users")
                .document(user.getUid())
                .collection("savedBooks")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> bookIds = new ArrayList<>();
                    querySnapshot.getDocuments().forEach(document -> {
                        String bookId = FirestoreValueReader.string(document, "bookId");
                        bookIds.add(isBlank(bookId) ? document.getId() : bookId);
                    });
                    callback.onSuccess(bookIds);
                })
                .addOnFailureListener(callback::onError);
    }

    private boolean canWrite(String bookId, RepositoryCallback<?> callback) {
        if (!configured || firestore == null) {
            callback.onError(FirebaseConfig.missingConfigException());
            return false;
        }
        if (currentUser() == null) {
            callback.onError(new IllegalStateException("Please sign in to update your library."));
            return false;
        }
        if (isBlank(bookId)) {
            callback.onError(new IllegalArgumentException("Missing book id."));
            return false;
        }
        return true;
    }

    private RepositoryCallback<SaveMutationResult> voidCallback(RepositoryCallback<Void> callback) {
        return new RepositoryCallback<SaveMutationResult>() {
            @Override public void onSuccess(SaveMutationResult data) { callback.onSuccess(null); }
            @Override public void onError(Exception exception) { callback.onError(exception); }
        };
    }

    private FirebaseUser currentUser() {
        return auth == null ? null : auth.getCurrentUser();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
