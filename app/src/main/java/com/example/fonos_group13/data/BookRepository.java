package com.example.fonos_group13.data;

import android.content.Context;

import com.example.fonos_group13.model.AudiobookGenerationStatus;
import com.example.fonos_group13.model.Book;
import com.example.fonos_group13.model.BookChapter;
import com.example.fonos_group13.model.FirestoreValueReader;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BookRepository {
    private final boolean configured;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    public BookRepository(Context context) {
        configured = FirebaseConfig.isConfigured(context);
        if (configured) {
            auth = FirebaseAuth.getInstance();
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
        getBook(bookId, BookAccessMode.PUBLISHED_ONLY, callback);
    }

    public void getBook(
            String bookId,
            BookAccessMode accessMode,
            RepositoryCallback<Book> callback
    ) {
        if (!configured || firestore == null) {
            callback.onError(FirebaseConfig.missingConfigException());
            return;
        }

        firestore.collection("books")
                .document(bookId)
                .get()
                .addOnSuccessListener(document -> {
                    BookAccessMode resolvedAccessMode = accessMode == null
                            ? BookAccessMode.PUBLISHED_ONLY
                            : accessMode;
                    AudiobookGenerationStatus generationStatus = AudiobookGenerationStatus.fromValue(
                            FirestoreValueReader.string(document, "generationStatus")
                    );
                    if (document.exists() && BookAccessPolicy.canReadBook(
                            FirestoreValueReader.booleanValue(document, "published", false),
                            FirestoreValueReader.string(document, "creatorUid"),
                            generationStatus,
                            currentUserUid(),
                            resolvedAccessMode
                    )) {
                        callback.onSuccess(Book.fromDocument(document));
                    } else {
                        callback.onError(unavailableBookException());
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    public void getChapters(String bookId, RepositoryCallback<List<BookChapter>> callback) {
        getChapters(bookId, BookAccessMode.PUBLISHED_ONLY, callback);
    }

    public void getChapters(
            String bookId,
            BookAccessMode accessMode,
            RepositoryCallback<List<BookChapter>> callback
    ) {
        if (!configured || firestore == null) {
            callback.onError(FirebaseConfig.missingConfigException());
            return;
        }

        BookAccessMode resolvedAccessMode = accessMode == null
                ? BookAccessMode.PUBLISHED_ONLY
                : accessMode;
        getBook(bookId, resolvedAccessMode, new RepositoryCallback<Book>() {
            @Override
            public void onSuccess(Book book) {
                loadAuthorizedChapters(book, resolvedAccessMode, callback);
            }

            @Override
            public void onError(Exception exception) {
                callback.onError(exception);
            }
        });
    }

    private void loadAuthorizedChapters(
            Book book,
            BookAccessMode accessMode,
            RepositoryCallback<List<BookChapter>> callback
    ) {
        String bookId = book.getId();
        boolean creatorPreviewAuthorized = BookAccessPolicy.canPreviewUnpublishedChapters(
                book.getCreatorUid(),
                book.getGenerationStatus(),
                currentUserUid(),
                accessMode
        );
        firestore.collection("books")
                .document(bookId)
                .collection("chapters")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<BookChapter> chapters = new ArrayList<>();
                    boolean hasChapterDocuments = !querySnapshot.isEmpty();
                    querySnapshot.getDocuments().forEach(document -> {
                        BookChapter chapter = BookChapter.fromDocument(bookId, document);
                        if (BookAccessPolicy.shouldIncludeChapter(
                                book.isPublished(),
                                creatorPreviewAuthorized,
                                chapter.isPublished()
                        )) {
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
                        List<BookChapter> fallbackChapters = new ArrayList<>();
                        fallbackChapters.add(BookChapter.fromLegacyBook(book));
                        callback.onSuccess(fallbackChapters);
                    } else {
                        callback.onSuccess(chapters);
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    private String currentUserUid() {
        FirebaseUser user = auth == null ? null : auth.getCurrentUser();
        return user == null ? null : user.getUid();
    }

    private Exception unavailableBookException() {
        return new SecurityException("This audiobook is unavailable.");
    }
}
