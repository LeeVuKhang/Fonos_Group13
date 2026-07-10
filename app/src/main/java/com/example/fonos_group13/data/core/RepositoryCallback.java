package com.example.fonos_group13.data.core;

public interface RepositoryCallback<T> {
    void onSuccess(T data);

    void onError(Exception exception);
}
