package com.example.fonos_group13.data.firestore;

import com.example.fonos_group13.model.AudiobookGenerationStatus;
import com.example.fonos_group13.model.Book;
import com.example.fonos_group13.model.AiStatus;
import com.google.firebase.firestore.DocumentSnapshot;

public final class BookDocumentMapper {
    private BookDocumentMapper() {
    }

    public static Book fromDocument(DocumentSnapshot document) {
        return new Book(
                document.getId(),
                DocumentMapperSupport.valueOrDefault(FirestoreValueReader.string(document, "title"), "Untitled"),
                DocumentMapperSupport.valueOrDefault(FirestoreValueReader.string(document, "author"), "Unknown author"),
                DocumentMapperSupport.valueOrDefault(FirestoreValueReader.string(document, "chapterTitle"), "Chapter 1"),
                DocumentMapperSupport.valueOrDefault(FirestoreValueReader.string(document, "contentSample"), ""),
                DocumentMapperSupport.firstNonBlank(
                        FirestoreValueReader.string(document, "coverUrl"),
                        FirestoreValueReader.string(document, "coverImageUrl"),
                        FirestoreValueReader.string(document, "imageUrl"),
                        FirestoreValueReader.string(document, "thumbnailUrl")
                ),
                DocumentMapperSupport.optionalString(FirestoreValueReader.string(document, "isbn")),
                DocumentMapperSupport.firstNonBlank(
                        FirestoreValueReader.string(document, "audioUrl"),
                        FirestoreValueReader.string(document, "url")
                ),
                DocumentMapperSupport.optionalString(FirestoreValueReader.string(document, "audioStoragePath")),
                FirestoreValueReader.longValue(document, "durationSec"),
                DocumentMapperSupport.valueOrDefault(FirestoreValueReader.string(document, "languageCode"), "en-US"),
                DocumentMapperSupport.valueOrDefault(FirestoreValueReader.string(document, "voiceGender"), "female"),
                FirestoreValueReader.string(document, "creatorUid"),
                AudiobookGenerationStatus.fromValue(FirestoreValueReader.string(document, "generationStatus")),
                FirestoreValueReader.booleanValue(document, "featured", false),
                FirestoreValueReader.booleanValue(document, "published", false),
                (int) FirestoreValueReader.longValue(document, "order"),
                FirestoreValueReader.doubleValue(document, "ratingAverage"),
                (int) FirestoreValueReader.longValue(document, "ratingCount"),
                (int) FirestoreValueReader.longValue(document, "saveCount"),
                AiStatus.fromValue(FirestoreValueReader.string(document, "aiStatus")),
                FirestoreValueReader.string(document, "aiStatusReason")
        );
    }
}
