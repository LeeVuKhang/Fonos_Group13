package com.example.fonos_group13.model;

public class CreateAudiobookDraftInput {
    public static final String DEFAULT_CHAPTER_TITLE = "Chapter 1";
    public static final String DEFAULT_LANGUAGE_CODE = "en-US";
    public static final int MAX_TITLE_CHARS = 120;
    public static final int MAX_AUTHOR_CHARS = 120;
    public static final int MAX_CHAPTER_TEXT_CHARS = 4000;

    private final String title;
    private final String author;
    private final String coverUrl;
    private final String chapterTitle;
    private final String chapterText;
    private final String languageCode;
    private final CreatorVoiceOption voiceOption;

    public CreateAudiobookDraftInput(
            String title,
            String author,
            String coverUrl,
            String chapterTitle,
            String chapterText,
            String languageCode,
            CreatorVoiceOption voiceOption
    ) {
        this.title = trim(title);
        this.author = trim(author);
        this.coverUrl = trimToNull(coverUrl);
        this.chapterTitle = valueOrDefault(chapterTitle, DEFAULT_CHAPTER_TITLE);
        this.chapterText = trim(chapterText);
        this.languageCode = valueOrDefault(languageCode, DEFAULT_LANGUAGE_CODE);
        this.voiceOption = voiceOption == null ? CreatorVoiceOption.MATTHEW : voiceOption;
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

    public String getChapterTitle() {
        return chapterTitle;
    }

    public String getChapterText() {
        return chapterText;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public CreatorVoiceOption getVoiceOption() {
        return voiceOption;
    }

    private static String valueOrDefault(String value, String fallback) {
        String trimmed = trimToNull(value);
        return trimmed == null ? fallback : trimmed;
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
