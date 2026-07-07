package com.example.fonos_group13.model;

import com.google.firebase.firestore.DocumentSnapshot;

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
    }

    public static Book fromDocument(DocumentSnapshot document) {
        return new Book(
                document.getId(),
                valueOrDefault(FirestoreValueReader.string(document, "title"), "Untitled"),
                valueOrDefault(FirestoreValueReader.string(document, "author"), "Unknown author"),
                valueOrDefault(FirestoreValueReader.string(document, "chapterTitle"), "Chapter 1"),
                valueOrDefault(FirestoreValueReader.string(document, "contentSample"), ""),
                firstNonBlank(
                        FirestoreValueReader.string(document, "coverUrl"),
                        FirestoreValueReader.string(document, "coverImageUrl"),
                        FirestoreValueReader.string(document, "imageUrl"),
                        FirestoreValueReader.string(document, "thumbnailUrl")
                ),
                optionalString(FirestoreValueReader.string(document, "isbn")),
                firstNonBlank(
                        FirestoreValueReader.string(document, "audioUrl"),
                        FirestoreValueReader.string(document, "url")
                ),
                optionalString(FirestoreValueReader.string(document, "audioStoragePath")),
                FirestoreValueReader.longValue(document, "durationSec"),
                valueOrDefault(FirestoreValueReader.string(document, "languageCode"), "en-US"),
                valueOrDefault(FirestoreValueReader.string(document, "voiceGender"), "female"),
                FirestoreValueReader.string(document, "creatorUid"),
                AudiobookGenerationStatus.fromValue(FirestoreValueReader.string(document, "generationStatus")),
                FirestoreValueReader.booleanValue(document, "featured", false),
                FirestoreValueReader.booleanValue(document, "published", false),
                (int) FirestoreValueReader.longValue(document, "order")
        );
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
}
