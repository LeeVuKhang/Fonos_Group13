package com.example.fonos_group13.model;

public class CreateChapterDraftInput {
    public static final String DEFAULT_CHAPTER_TITLE = "Chapter";
    public static final String DEFAULT_LANGUAGE_CODE = CreateAudiobookDraftInput.DEFAULT_LANGUAGE_CODE;
    public static final int MAX_CHAPTER_TEXT_WORDS = CreateAudiobookDraftInput.MAX_CHAPTER_TEXT_WORDS;

    private final String chapterTitle;
    private final String chapterText;
    private final String languageCode;
    private final CreatorVoiceOption voiceOption;

    public CreateChapterDraftInput(
            String chapterTitle,
            String chapterText,
            String languageCode,
            CreatorVoiceOption voiceOption
    ) {
        this.chapterTitle = valueOrDefault(chapterTitle, DEFAULT_CHAPTER_TITLE);
        this.chapterText = trim(chapterText);
        this.languageCode = valueOrDefault(languageCode, DEFAULT_LANGUAGE_CODE);
        this.voiceOption = voiceOption == null ? CreatorVoiceOption.PATRICK : voiceOption;
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

    public static int countWords(String value) {
        return CreateAudiobookDraftInput.countWords(value);
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
