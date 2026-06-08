package com.example.fonos_group13.data;

import android.content.Context;

import com.example.fonos_group13.model.Book;
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
}
