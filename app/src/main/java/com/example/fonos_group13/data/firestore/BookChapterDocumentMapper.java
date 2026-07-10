package com.example.fonos_group13.data.firestore;

import com.example.fonos_group13.model.BookChapter;
import com.google.firebase.firestore.DocumentSnapshot;

public final class BookChapterDocumentMapper {
    private BookChapterDocumentMapper() {
    }

    public static BookChapter fromDocument(String bookId, DocumentSnapshot document) {
        long order = FirestoreValueReader.longValue(document, "order");
        return new BookChapter(
                document.getId(),
                bookId,
                DocumentMapperSupport.valueOrDefault(
                        DocumentMapperSupport.firstNonBlank(
                                FirestoreValueReader.string(document, "title"),
                                FirestoreValueReader.string(document, "chapterTitle")
                        ),
                        "Chapter " + Math.max(order + 1, 1)
                ),
                DocumentMapperSupport.valueOrDefault(FirestoreValueReader.string(document, "contentSample"), ""),
                DocumentMapperSupport.firstNonBlank(
                        FirestoreValueReader.string(document, "audioUrl"),
                        FirestoreValueReader.string(document, "url")
                ),
                DocumentMapperSupport.optionalString(FirestoreValueReader.string(document, "audioStoragePath")),
                FirestoreValueReader.longValue(document, "durationSec"),
                (int) order,
                FirestoreValueReader.booleanValue(document, "published", true),
                false
        );
    }
}
