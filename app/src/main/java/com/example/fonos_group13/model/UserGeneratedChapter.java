package com.example.fonos_group13.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

public class UserGeneratedChapter {
    private final String id;
    private final String title;
    private final AudiobookGenerationStatus generationStatus;
    private final boolean published;
    private final int order;
    private final Timestamp updatedAt;
    private final String generationError;
    private final boolean hasAudio;

    public UserGeneratedChapter(
            String id,
            String title,
            AudiobookGenerationStatus generationStatus,
            boolean published,
            int order,
            Timestamp updatedAt,
            String generationError,
            boolean hasAudio
    ) {
        this.id = valueOrDefault(id, BookChapter.LEGACY_CHAPTER_ID);
        this.title = valueOrDefault(title, titleFromOrder(order));
        this.generationStatus = generationStatus == null ? AudiobookGenerationStatus.DRAFT : generationStatus;
        this.published = published;
        this.order = Math.max(order, 0);
        this.updatedAt = updatedAt;
        this.generationError = optionalString(generationError);
        this.hasAudio = hasAudio;
    }

    public static UserGeneratedChapter fromDocument(String bookId, DocumentSnapshot document) {
        long order = FirestoreValueReader.longValue(document, "order");
        return new UserGeneratedChapter(
                document == null ? null : document.getId(),
                firstNonBlank(
                        FirestoreValueReader.string(document, "title"),
                        FirestoreValueReader.string(document, "chapterTitle")
                ),
                AudiobookGenerationStatus.fromValue(FirestoreValueReader.string(document, "generationStatus")),
                FirestoreValueReader.booleanValue(document, "published", false),
                (int) order,
                FirestoreValueReader.timestamp(document, "updatedAt"),
                FirestoreValueReader.string(document, "generationError"),
                hasAudio(document)
        );
    }

    public static boolean isDeletedDocument(DocumentSnapshot document) {
        return FirestoreValueReader.booleanValue(document, "deletedByCreator", false)
                || FirestoreValueReader.timestamp(document, "deletedAt") != null
                || AudiobookGenerationStatus.DELETED == AudiobookGenerationStatus.fromValue(
                FirestoreValueReader.string(document, "generationStatus")
        );
    }

    public boolean canEdit() {
        return generationStatus == AudiobookGenerationStatus.DRAFT;
    }

    public boolean canRequestGeneration() {
        return generationStatus == AudiobookGenerationStatus.DRAFT
                || generationStatus == AudiobookGenerationStatus.FAILED;
    }

    public boolean canPreview() {
        return hasAudio
                && (generationStatus == AudiobookGenerationStatus.READY_FOR_REVIEW
                || generationStatus == AudiobookGenerationStatus.PUBLISHED);
    }

    public boolean canDelete() {
        return !published
                && generationStatus != AudiobookGenerationStatus.PUBLISHED
                && (generationStatus == AudiobookGenerationStatus.DRAFT
                || generationStatus == AudiobookGenerationStatus.FAILED
                || generationStatus == AudiobookGenerationStatus.PENDING_GENERATION
                || generationStatus == AudiobookGenerationStatus.READY_FOR_REVIEW);
    }

    public String getDeleteActionLabel() {
        return generationStatus == AudiobookGenerationStatus.PENDING_GENERATION
                ? "Cancel Chapter"
                : "Delete";
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public AudiobookGenerationStatus getGenerationStatus() {
        return generationStatus;
    }

    public boolean isPublished() {
        return published;
    }

    public int getOrder() {
        return order;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public String getGenerationError() {
        return generationError;
    }

    public boolean hasAudio() {
        return hasAudio;
    }

    private static boolean hasAudio(DocumentSnapshot document) {
        return firstNonBlank(
                FirestoreValueReader.string(document, "audioUrl"),
                FirestoreValueReader.string(document, "url"),
                FirestoreValueReader.string(document, "audioStoragePath"),
                FirestoreValueReader.string(document, "s3Key")
        ) != null;
    }

    private static String valueOrDefault(String value, String fallback) {
        String trimmed = optionalString(value);
        return trimmed == null ? fallback : trimmed;
    }

    private static String optionalString(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            String trimmed = optionalString(value);
            if (trimmed != null) {
                return trimmed;
            }
        }
        return null;
    }

    private static String titleFromOrder(int order) {
        return "Chapter " + Math.max(order + 1, 1);
    }
}
