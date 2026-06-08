package com.example.fonos_group13.data;

public interface RepositoryCallback<T> {
    void onSuccess(T data);

    void onError(Exception exception);
}
