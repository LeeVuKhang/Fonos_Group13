package com.example.fonos_group13.data.repository;

import com.example.fonos_group13.data.core.RepositoryCallback;
import com.example.fonos_group13.model.UserAccount;

public interface AuthRepository {
    boolean isConfigured();

    UserAccount getCurrentUser();

    void signIn(String email, String password, RepositoryCallback<UserAccount> callback);

    void register(String email, String password, RepositoryCallback<UserAccount> callback);

    void updateDisplayName(String displayName, RepositoryCallback<UserAccount> callback);

    void signOut();
}
