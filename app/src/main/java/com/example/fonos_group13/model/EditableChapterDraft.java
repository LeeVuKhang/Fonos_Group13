package com.example.fonos_group13.model;

public class EditableChapterDraft {
    private final String bookId;
    private final String chapterId;
    private final String bookTitle;
    private final String chapterTitle;
    private final String chapterText;
    private final String languageCode;
    private final CreatorVoiceOption voiceOption;
    private final AudiobookGenerationStatus generationStatus;

    public EditableChapterDraft(
            String bookId,
            String chapterId,
            String bookTitle,
            String chapterTitle,
            String chapterText,
            String languageCode,
            CreatorVoiceOption voiceOption,
            AudiobookGenerationStatus generationStatus
    ) {
        this.bookId = trim(bookId);
        this.chapterId = trim(chapterId);
        this.bookTitle = trim(bookTitle);
        this.chapterTitle = valueOrDefault(chapterTitle, CreateChapterDraftInput.DEFAULT_CHAPTER_TITLE);
        this.chapterText = trim(chapterText);
        this.languageCode = valueOrDefault(languageCode, CreateChapterDraftInput.DEFAULT_LANGUAGE_CODE);
        this.voiceOption = voiceOption == null ? CreatorVoiceOption.PATRICK : voiceOption;
        this.generationStatus = generationStatus == null ? AudiobookGenerationStatus.DRAFT : generationStatus;
    }

    public String getBookId() {
        return bookId;
    }

    public String getChapterId() {
        return chapterId;
    }

    public String getBookTitle() {
        return bookTitle;
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

    public AudiobookGenerationStatus getGenerationStatus() {
        return generationStatus;
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
