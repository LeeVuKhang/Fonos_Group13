package com.example.fonos_group13.data.core;

/**
 * Receives the terminal result of a repository operation.
 *
 * <p>Production repositories deliver both methods on the Android main thread so callers can
 * safely render the result. Test doubles may invoke callbacks synchronously.</p>
 */
public interface RepositoryCallback<T> {
    void onSuccess(T data);

    void onError(Exception exception);
}
