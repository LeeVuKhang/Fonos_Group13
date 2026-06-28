package com.example.fonos_group13.model;

public class EditableAudiobookDraft {
    private final String bookId;
    private final String title;
    private final String author;
    private final String coverUrl;
    private final String chapterTitle;
    private final String chapterText;
    private final String languageCode;
    private final CreatorVoiceOption voiceOption;
    private final AudiobookGenerationStatus generationStatus;

    public EditableAudiobookDraft(
            String bookId,
            String title,
            String author,
            String coverUrl,
            String chapterTitle,
            String chapterText,
            String languageCode,
            CreatorVoiceOption voiceOption,
            AudiobookGenerationStatus generationStatus
    ) {
        this.bookId = trim(bookId);
        this.title = trim(title);
        this.author = trim(author);
        this.coverUrl = trimToNull(coverUrl);
        this.chapterTitle = valueOrDefault(chapterTitle, CreateAudiobookDraftInput.DEFAULT_CHAPTER_TITLE);
        this.chapterText = trim(chapterText);
        this.languageCode = valueOrDefault(languageCode, CreateAudiobookDraftInput.DEFAULT_LANGUAGE_CODE);
        this.voiceOption = voiceOption == null ? CreatorVoiceOption.PATRICK : voiceOption;
        this.generationStatus = generationStatus == null ? AudiobookGenerationStatus.DRAFT : generationStatus;
    }

    public String getBookId() {
        return bookId;
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

    public AudiobookGenerationStatus getGenerationStatus() {
        return generationStatus;
    }

    public CreateAudiobookDraftInput toInput() {
        return new CreateAudiobookDraftInput(
                title,
                author,
                coverUrl,
                chapterTitle,
                chapterText,
                languageCode,
                voiceOption
        );
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
