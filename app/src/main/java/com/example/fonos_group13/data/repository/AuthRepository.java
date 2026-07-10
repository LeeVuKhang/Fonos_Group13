package com.example.fonos_group13.data.repository;

import com.example.fonos_group13.data.core.RepositoryCallback;
import com.google.firebase.auth.FirebaseUser;

public interface AuthRepository {
    boolean isConfigured();

    FirebaseUser getCurrentUser();

    void signIn(String email, String password, RepositoryCallback<FirebaseUser> callback);

    void register(String email, String password, RepositoryCallback<FirebaseUser> callback);

    void updateDisplayName(String displayName, RepositoryCallback<FirebaseUser> callback);

    void signOut();
}
