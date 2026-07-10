package com.example.fonos_group13.data.repository;

import com.example.fonos_group13.data.catalog.BookAccessMode;
import com.example.fonos_group13.data.core.RepositoryCallback;
import com.example.fonos_group13.model.Book;
import com.example.fonos_group13.model.BookChapter;

import java.util.List;

public interface CatalogRepository {
    void getPublishedBooks(RepositoryCallback<List<Book>> callback);

    void getBook(String bookId, RepositoryCallback<Book> callback);

    void getBook(String bookId, BookAccessMode accessMode, RepositoryCallback<Book> callback);

    void getChapters(String bookId, RepositoryCallback<List<BookChapter>> callback);

    void getChapters(
            String bookId,
            BookAccessMode accessMode,
            RepositoryCallback<List<BookChapter>> callback
    );
}
