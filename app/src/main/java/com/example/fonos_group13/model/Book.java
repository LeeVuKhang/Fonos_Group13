package com.example.fonos_group13.model;

import com.google.firebase.firestore.DocumentSnapshot;

public class Book {
    private final String id;
    private final String title;
    private final String author;
    private final String chapterTitle;
    private final String contentSample;
    private final String audioUrl;
    private final String audioStoragePath;
    private final long durationSec;
    private final String languageCode;
    private final String voiceGender;
    private final boolean featured;
    private final boolean published;
    private final int order;

    public Book(
            String id,
            String title,
            String author,
            String chapterTitle,
            String contentSample,
            String audioUrl,
            String audioStoragePath,
            long durationSec,
            String languageCode,
            String voiceGender,
            boolean featured,
            boolean published,
            int order
    ) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.chapterTitle = chapterTitle;
        this.contentSample = contentSample;
        this.audioUrl = audioUrl;
        this.audioStoragePath = audioStoragePath;
        this.durationSec = durationSec;
        this.languageCode = languageCode;
        this.voiceGender = voiceGender;
        this.featured = featured;
        this.published = published;
        this.order = order;
    }

    public static Book fromDocument(DocumentSnapshot document) {
        return new Book(
                document.getId(),
                valueOrDefault(document.getString("title"), "Untitled"),
                valueOrDefault(document.getString("author"), "Unknown author"),
                valueOrDefault(document.getString("chapterTitle"), "Chapter 1"),
                valueOrDefault(document.getString("contentSample"), ""),
                firstNonBlank(document.getString("audioUrl"), document.getString("url")),
                optionalString(document.getString("audioStoragePath")),
                longValue(document.getLong("durationSec")),
                valueOrDefault(document.getString("languageCode"), "en-US"),
                valueOrDefault(document.getString("voiceGender"), "female"),
                booleanValue(document.getBoolean("featured")),
                booleanValue(document.getBoolean("published")),
                (int) longValue(document.getLong("order"))
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

    private static long longValue(Long value) {
        return value == null ? 0 : value;
    }

    private static boolean booleanValue(Boolean value) {
        return value != null && value;
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
