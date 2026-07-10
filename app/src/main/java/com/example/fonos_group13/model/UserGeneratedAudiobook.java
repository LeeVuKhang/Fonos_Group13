package com.example.fonos_group13.model;

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
    private final long createdAtMillis;
    private final long updatedAtMillis;
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
            long createdAtMillis,
            long updatedAtMillis,
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
        this.createdAtMillis = Math.max(createdAtMillis, 0);
        this.updatedAtMillis = Math.max(updatedAtMillis, 0);
        this.generationError = optionalString(generationError);
    }

    public long getSortTimestampMillis() {
        return updatedAtMillis > 0 ? updatedAtMillis : createdAtMillis;
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

    public long getCreatedAtMillis() {
        return createdAtMillis;
    }

    public long getUpdatedAtMillis() {
        return updatedAtMillis;
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
