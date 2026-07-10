package com.example.fonos_group13.data.firestore;

import com.example.fonos_group13.model.AudiobookGenerationStatus;
import com.example.fonos_group13.model.UserGeneratedAudiobook;
import com.example.fonos_group13.model.UserGeneratedChapter;
import com.google.firebase.firestore.DocumentSnapshot;

public final class CreatorUploadDocumentMapper {
    private CreatorUploadDocumentMapper() {
    }

    public static UserGeneratedAudiobook audiobook(DocumentSnapshot document) {
        String rawStatus = FirestoreValueReader.string(document, "generationStatus");
        AudiobookGenerationStatus status = AudiobookGenerationStatus.fromValue(rawStatus);
        boolean published = FirestoreValueReader.booleanValue(document, "published", false);
        String activeChapterId = DocumentMapperSupport.optionalString(
                FirestoreValueReader.string(document, "activeChapterId")
        );
        if (published && (DocumentMapperSupport.optionalString(rawStatus) == null
                || activeChapterId == null
                || status == AudiobookGenerationStatus.PUBLISHED)) {
            status = AudiobookGenerationStatus.PUBLISHED;
        }
        return new UserGeneratedAudiobook(
                document.getId(),
                FirestoreValueReader.string(document, "title"),
                FirestoreValueReader.string(document, "author"),
                DocumentMapperSupport.firstNonBlank(
                        FirestoreValueReader.string(document, "coverUrl"),
                        FirestoreValueReader.string(document, "coverImageUrl"),
                        FirestoreValueReader.string(document, "imageUrl"),
                        FirestoreValueReader.string(document, "thumbnailUrl")
                ),
                FirestoreValueReader.string(document, "languageCode"),
                FirestoreValueReader.string(document, "voiceGender"),
                FirestoreValueReader.string(document, "pollyVoiceId"),
                status,
                activeChapterId,
                FirestoreValueReader.string(document, "reviewStatus"),
                published,
                FirestoreValueReader.booleanValue(document, "hiddenByCreator", false),
                FirestoreValueReader.timestampMillis(document, "createdAt"),
                FirestoreValueReader.timestampMillis(document, "updatedAt"),
                FirestoreValueReader.string(document, "generationError")
        );
    }

    public static UserGeneratedChapter chapter(DocumentSnapshot document) {
        long order = FirestoreValueReader.longValue(document, "order");
        return new UserGeneratedChapter(
                document == null ? null : document.getId(),
                DocumentMapperSupport.firstNonBlank(
                        FirestoreValueReader.string(document, "title"),
                        FirestoreValueReader.string(document, "chapterTitle")
                ),
                AudiobookGenerationStatus.fromValue(FirestoreValueReader.string(document, "generationStatus")),
                FirestoreValueReader.booleanValue(document, "published", false),
                (int) order,
                FirestoreValueReader.timestampMillis(document, "updatedAt"),
                FirestoreValueReader.string(document, "generationError"),
                DocumentMapperSupport.firstNonBlank(
                        FirestoreValueReader.string(document, "audioUrl"),
                        FirestoreValueReader.string(document, "url"),
                        FirestoreValueReader.string(document, "audioStoragePath"),
                        FirestoreValueReader.string(document, "s3Key")
                ) != null
        );
    }

    public static boolean isDeletedChapter(DocumentSnapshot document) {
        return FirestoreValueReader.booleanValue(document, "deletedByCreator", false)
                || FirestoreValueReader.hasTimestamp(document, "deletedAt")
                || AudiobookGenerationStatus.DELETED == AudiobookGenerationStatus.fromValue(
                FirestoreValueReader.string(document, "generationStatus")
        );
    }
}
