package com.example.fonos_group13.data;

import android.content.Context;

import com.example.fonos_group13.model.Book;
import com.example.fonos_group13.model.BookChapter;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BookRepository {
    private final boolean configured;
    private FirebaseFirestore firestore;

    public BookRepository(Context context) {
        configured = FirebaseConfig.isConfigured(context);
        if (configured) {
            firestore = FirebaseFirestore.getInstance();
        }
    }

    public void getPublishedBooks(RepositoryCallback<List<Book>> callback) {
        if (!configured || firestore == null) {
            callback.onError(FirebaseConfig.missingConfigException());
            return;
        }

        firestore.collection("books")
                .whereEqualTo("published", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Book> books = new ArrayList<>();
                    querySnapshot.getDocuments().forEach(document -> books.add(Book.fromDocument(document)));
                    Collections.sort(books, (left, right) -> Integer.compare(left.getOrder(), right.getOrder()));
                    callback.onSuccess(books);
                })
                .addOnFailureListener(callback::onError);
    }

    public void getBook(String bookId, RepositoryCallback<Book> callback) {
        if (!configured || firestore == null) {
            callback.onError(FirebaseConfig.missingConfigException());
            return;
        }

        firestore.collection("books")
                .document(bookId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        callback.onSuccess(Book.fromDocument(document));
                    } else {
                        callback.onError(new IllegalArgumentException("Book not found: " + bookId));
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    public void getChapters(String bookId, RepositoryCallback<List<BookChapter>> callback) {
        if (!configured || firestore == null) {
            callback.onError(FirebaseConfig.missingConfigException());
            return;
        }

        firestore.collection("books")
                .document(bookId)
                .collection("chapters")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<BookChapter> chapters = new ArrayList<>();
                    boolean hasChapterDocuments = !querySnapshot.isEmpty();
                    querySnapshot.getDocuments().forEach(document -> {
                        BookChapter chapter = BookChapter.fromDocument(bookId, document);
                        if (chapter.isPublished()) {
                            chapters.add(chapter);
                        }
                    });
                    Collections.sort(chapters, (left, right) -> {
                        int orderCompare = Integer.compare(left.getOrder(), right.getOrder());
                        if (orderCompare != 0) {
                            return orderCompare;
                        }
                        return left.getTitle().compareToIgnoreCase(right.getTitle());
                    });
                    if (!hasChapterDocuments) {
                        getBook(bookId, new RepositoryCallback<Book>() {
                            @Override
                            public void onSuccess(Book book) {
                                List<BookChapter> fallbackChapters = new ArrayList<>();
                                fallbackChapters.add(BookChapter.fromLegacyBook(book));
                                callback.onSuccess(fallbackChapters);
                            }

                            @Override
                            public void onError(Exception exception) {
                                callback.onError(exception);
                            }
                        });
                    } else {
                        callback.onSuccess(chapters);
                    }
                })
                .addOnFailureListener(callback::onError);
    }
}
