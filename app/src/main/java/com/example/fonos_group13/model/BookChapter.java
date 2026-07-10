package com.example.fonos_group13.model;

public class BookChapter {
    public static final String LEGACY_CHAPTER_ID = "chapter_1";

    private final String id;
    private final String bookId;
    private final String title;
    private final String contentSample;
    private final String audioUrl;
    private final String audioStoragePath;
    private final long durationSec;
    private final int order;
    private final boolean published;
    private final boolean legacyFallback;

    public BookChapter(
            String id,
            String bookId,
            String title,
            String contentSample,
            String audioUrl,
            String audioStoragePath,
            long durationSec,
            int order,
            boolean published,
            boolean legacyFallback
    ) {
        this.id = valueOrDefault(id, LEGACY_CHAPTER_ID);
        this.bookId = valueOrDefault(bookId, "");
        this.title = valueOrDefault(title, "Chapter 1");
        this.contentSample = valueOrDefault(contentSample, "");
        this.audioUrl = optionalString(audioUrl);
        this.audioStoragePath = optionalString(audioStoragePath);
        this.durationSec = Math.max(durationSec, 0);
        this.order = order;
        this.published = published;
        this.legacyFallback = legacyFallback;
    }

    public static BookChapter fromLegacyBook(Book book) {
        return new BookChapter(
                LEGACY_CHAPTER_ID,
                book.getId(),
                valueOrDefault(book.getChapterTitle(), "Chapter 1"),
                valueOrDefault(book.getContentSample(), ""),
                book.getAudioUrl(),
                book.getAudioStoragePath(),
                book.getDurationSec(),
                0,
                true,
                true
        );
    }

    public String getId() {
        return id;
    }

    public String getBookId() {
        return bookId;
    }

    public String getTitle() {
        return title;
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

    public int getOrder() {
        return order;
    }

    public boolean isPublished() {
        return published;
    }

    public boolean isLegacyFallback() {
        return legacyFallback;
    }

    public boolean hasAudio() {
        return audioUrl != null && !audioUrl.trim().isEmpty();
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

    private static String titleFromOrder(long order) {
        return "Chapter " + Math.max(order + 1, 1);
    }
}
