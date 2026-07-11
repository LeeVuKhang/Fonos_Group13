package com.example.fonos_group13.data.repository;

import com.example.fonos_group13.data.core.RepositoryCallback;

import java.util.List;
import com.example.fonos_group13.model.SaveMutationResult;

public interface SavedBooksRepository {
    void isSaved(String bookId, RepositoryCallback<Boolean> callback);

    void saveBook(String bookId, RepositoryCallback<Void> callback);

    void unsaveBook(String bookId, RepositoryCallback<Void> callback);

    default void setSavedWithResult(String bookId, boolean saved, RepositoryCallback<SaveMutationResult> callback) {
        RepositoryCallback<Void> adapter = new RepositoryCallback<Void>() {
            @Override public void onSuccess(Void data) { callback.onSuccess(new SaveMutationResult(saved, 0)); }
            @Override public void onError(Exception exception) { callback.onError(exception); }
        };
        if (saved) saveBook(bookId, adapter); else unsaveBook(bookId, adapter);
    }

    void getSavedBookIds(RepositoryCallback<List<String>> callback);
}
