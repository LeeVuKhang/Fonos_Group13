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
    private final String reviewStatus;
    private final boolean published;
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
            String reviewStatus,
            boolean published,
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
        this.reviewStatus = valueOrDefault(reviewStatus, "pending");
        this.published = published;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.generationError = optionalString(generationError);
    }

    public static UserGeneratedAudiobook fromDocument(DocumentSnapshot document) {
        AudiobookGenerationStatus status = AudiobookGenerationStatus.fromValue(document.getString("generationStatus"));
        boolean published = Boolean.TRUE.equals(document.getBoolean("published"));
        if (published) {
            status = AudiobookGenerationStatus.PUBLISHED;
        }
        return new UserGeneratedAudiobook(
                document.getId(),
                document.getString("title"),
                document.getString("author"),
                firstNonBlank(
                        document.getString("coverUrl"),
                        document.getString("coverImageUrl"),
                        document.getString("imageUrl"),
                        document.getString("thumbnailUrl")
                ),
                document.getString("languageCode"),
                document.getString("voiceGender"),
                document.getString("pollyVoiceId"),
                status,
                document.getString("reviewStatus"),
                published,
                document.getTimestamp("createdAt"),
                document.getTimestamp("updatedAt"),
                document.getString("generationError")
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

    public String getReviewStatus() {
        return reviewStatus;
    }

    public boolean isPublished() {
        return published;
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
