package com.example.fonos_group13.data.repository;

import com.example.fonos_group13.data.core.RepositoryCallback;

import java.util.List;

public interface SavedBooksRepository {
    void isSaved(String bookId, RepositoryCallback<Boolean> callback);

    void saveBook(String bookId, RepositoryCallback<Void> callback);

    void unsaveBook(String bookId, RepositoryCallback<Void> callback);

    void getSavedBookIds(RepositoryCallback<List<String>> callback);
}
