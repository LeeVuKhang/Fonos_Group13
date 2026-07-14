package com.example.fonos_group13.model;

public class Book {
    private final String id;
    private final String title;
    private final String author;
    private final String chapterTitle;
    private final String contentSample;
    private final String coverUrl;
    private final String isbn;
    private final String audioUrl;
    private final String audioStoragePath;
    private final long durationSec;
    private final String languageCode;
    private final String voiceGender;
    private final String creatorUid;
    private final AudiobookGenerationStatus generationStatus;
    private final boolean featured;
    private final boolean published;
    private final int order;
    private final double ratingAverage;
    private final int ratingCount;
    private final int saveCount;
    private final AiStatus aiStatus;
    private final String aiStatusReason;

    public Book(
            String id,
            String title,
            String author,
            String chapterTitle,
            String contentSample,
            String coverUrl,
            String isbn,
            String audioUrl,
            String audioStoragePath,
            long durationSec,
            String languageCode,
            String voiceGender,
            String creatorUid,
            AudiobookGenerationStatus generationStatus,
            boolean featured,
            boolean published,
            int order
    ) {
        this(id, title, author, chapterTitle, contentSample, coverUrl, isbn, audioUrl,
                audioStoragePath, durationSec, languageCode, voiceGender, creatorUid,
                generationStatus, featured, published, order, 0, 0, 0,
                AiStatus.UNAVAILABLE, null);
    }

    public Book(
            String id,
            String title,
            String author,
            String chapterTitle,
            String contentSample,
            String coverUrl,
            String isbn,
            String audioUrl,
            String audioStoragePath,
            long durationSec,
            String languageCode,
            String voiceGender,
            String creatorUid,
            AudiobookGenerationStatus generationStatus,
            boolean featured,
            boolean published,
            int order,
            double ratingAverage,
            int ratingCount,
            int saveCount
    ) {
        this(id, title, author, chapterTitle, contentSample, coverUrl, isbn, audioUrl,
                audioStoragePath, durationSec, languageCode, voiceGender, creatorUid,
                generationStatus, featured, published, order, ratingAverage, ratingCount,
                saveCount, AiStatus.UNAVAILABLE, null);
    }

    public Book(
            String id,
            String title,
            String author,
            String chapterTitle,
            String contentSample,
            String coverUrl,
            String isbn,
            String audioUrl,
            String audioStoragePath,
            long durationSec,
            String languageCode,
            String voiceGender,
            String creatorUid,
            AudiobookGenerationStatus generationStatus,
            boolean featured,
            boolean published,
            int order,
            double ratingAverage,
            int ratingCount,
            int saveCount,
            AiStatus aiStatus,
            String aiStatusReason
    ) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.chapterTitle = chapterTitle;
        this.contentSample = contentSample;
        this.coverUrl = firstNonBlank(coverUrl, coverUrlFromIsbn(isbn));
        this.isbn = optionalString(isbn);
        this.audioUrl = audioUrl;
        this.audioStoragePath = audioStoragePath;
        this.durationSec = durationSec;
        this.languageCode = languageCode;
        this.voiceGender = voiceGender;
        this.creatorUid = optionalString(creatorUid);
        this.generationStatus = generationStatus == null ? AudiobookGenerationStatus.DRAFT : generationStatus;
        this.featured = featured;
        this.published = published;
        this.order = order;
        this.ratingAverage = Double.isFinite(ratingAverage) ? Math.max(0, ratingAverage) : 0;
        this.ratingCount = Math.max(0, ratingCount);
        this.saveCount = Math.max(0, saveCount);
        this.aiStatus = aiStatus == null ? AiStatus.UNAVAILABLE : aiStatus;
        this.aiStatusReason = optionalString(aiStatusReason);
    }

    private static String valueOrDefault(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
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

    private static String coverUrlFromIsbn(String isbn) {
        String normalizedIsbn = normalizeIsbn(isbn);
        if (normalizedIsbn == null) {
            return null;
        }
        return "https://covers.openlibrary.org/b/isbn/" + normalizedIsbn + "-L.jpg?default=false";
    }

    private static String normalizeIsbn(String isbn) {
        String trimmed = optionalString(isbn);
        if (trimmed == null) {
            return null;
        }
        String normalized = trimmed.replaceAll("[^0-9Xx]", "").toUpperCase();
        return normalized.isEmpty() ? null : normalized;
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

    public String getChapterTitle() {
        return chapterTitle;
    }

    public String getContentSample() {
        return contentSample;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public String getIsbn() {
        return isbn;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public String getAudioStoragePath() {
        return audioStoragePath;
    }

    public long getDurationSec() {
        return durationSec;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public String getVoiceGender() {
        return voiceGender;
    }

    public String getCreatorUid() {
        return creatorUid;
    }

    public AudiobookGenerationStatus getGenerationStatus() {
        return generationStatus;
    }

    public boolean isFeatured() {
        return featured;
    }

    public boolean isPublished() {
        return published;
    }

    public int getOrder() {
        return order;
    }

    public double getRatingAverage() { return ratingAverage; }

    public int getRatingCount() { return ratingCount; }

    public int getSaveCount() { return saveCount; }

    public AiStatus getAiStatus() { return aiStatus; }

    public String getAiStatusReason() { return aiStatusReason; }
}
