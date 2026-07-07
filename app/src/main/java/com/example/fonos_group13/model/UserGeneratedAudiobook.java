package com.example.fonos_group13.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

public class UserGeneratedAudiobook {
    private final String id;
    private final String title;
    private final String author;
    private final String coverUrl;
    private final String languageCode;
    private final String voiceGender;
    private final String pollyVoiceId;
    private final AudiobookGenerationStatus generationStatus;
    private final String activeChapterId;
    private final String reviewStatus;
    private final boolean published;
    private final boolean hiddenByCreator;
    private final Timestamp createdAt;
    private final Timestamp updatedAt;
    private final String generationError;

    public UserGeneratedAudiobook(
            String id,
            String title,
            String author,
            String coverUrl,
            String languageCode,
            String voiceGender,
            String pollyVoiceId,
            AudiobookGenerationStatus generationStatus,
            String activeChapterId,
            String reviewStatus,
            boolean published,
            boolean hiddenByCreator,
            Timestamp createdAt,
            Timestamp updatedAt,
            String generationError
    ) {
        this.id = valueOrDefault(id, "");
        this.title = valueOrDefault(title, "Untitled");
        this.author = valueOrDefault(author, "Unknown author");
        this.coverUrl = optionalString(coverUrl);
        this.languageCode = valueOrDefault(languageCode, CreateAudiobookDraftInput.DEFAULT_LANGUAGE_CODE);
        this.voiceGender = valueOrDefault(voiceGender, "male");
        this.pollyVoiceId = optionalString(pollyVoiceId);
        this.generationStatus = generationStatus == null ? AudiobookGenerationStatus.DRAFT : generationStatus;
        this.activeChapterId = optionalString(activeChapterId);
        this.reviewStatus = valueOrDefault(reviewStatus, "pending");
        this.published = published;
        this.hiddenByCreator = hiddenByCreator;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.generationError = optionalString(generationError);
    }

    public static UserGeneratedAudiobook fromDocument(DocumentSnapshot document) {
        String rawStatus = FirestoreValueReader.string(document, "generationStatus");
        AudiobookGenerationStatus status = AudiobookGenerationStatus.fromValue(rawStatus);
        boolean published = FirestoreValueReader.booleanValue(document, "published", false);
        String activeChapterId = optionalString(FirestoreValueReader.string(document, "activeChapterId"));
        if (published && (optionalString(rawStatus) == null || activeChapterId == null || status == AudiobookGenerationStatus.PUBLISHED)) {
            status = AudiobookGenerationStatus.PUBLISHED;
        }
        return new UserGeneratedAudiobook(
                document.getId(),
                FirestoreValueReader.string(document, "title"),
                FirestoreValueReader.string(document, "author"),
                firstNonBlank(
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
                FirestoreValueReader.timestamp(document, "createdAt"),
                FirestoreValueReader.timestamp(document, "updatedAt"),
                FirestoreValueReader.string(document, "generationError")
        );
    }

    public long getSortTimestampMillis() {
        Timestamp timestamp = updatedAt == null ? createdAt : updatedAt;
        return timestamp == null ? 0 : timestamp.toDate().getTime();
    }

    public boolean canRequestGeneration() {
        return generationStatus == AudiobookGenerationStatus.DRAFT
                || generationStatus == AudiobookGenerationStatus.FAILED;
    }

    public boolean hasChapterUpdate() {
        return activeChapterId != null;
    }

    public boolean activeChapterIsInitialChapter() {
        return BookChapter.LEGACY_CHAPTER_ID.equals(activeChapterId);
    }

    public String getVoiceLabel() {
        if (pollyVoiceId != null) {
            return pollyVoiceId + " - " + voiceGender;
        }
        return voiceGender;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public String getVoiceGender() {
        return voiceGender;
    }

    public String getPollyVoiceId() {
        return pollyVoiceId;
    }

    public AudiobookGenerationStatus getGenerationStatus() {
        return generationStatus;
    }

    public String getActiveChapterId() {
        return activeChapterId;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public boolean isPublished() {
        return published;
    }

    public boolean isHiddenByCreator() {
        return hiddenByCreator;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public String getGenerationError() {
        return generationError;
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
}
