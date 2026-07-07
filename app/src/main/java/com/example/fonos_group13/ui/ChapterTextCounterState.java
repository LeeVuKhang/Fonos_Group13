package com.example.fonos_group13.ui;

import com.example.fonos_group13.model.CreateAudiobookDraftInput;

public final class ChapterTextCounterState {
    public enum Severity {
        NORMAL,
        WARNING,
        ERROR
    }

    public static final int WARNING_CHAPTER_TEXT_WORDS =
            (int) Math.ceil(CreateAudiobookDraftInput.MAX_CHAPTER_TEXT_WORDS * 0.9);

    private final int wordCount;
    private final Severity severity;

    private ChapterTextCounterState(int wordCount, Severity severity) {
        this.wordCount = wordCount;
        this.severity = severity;
    }

    public static ChapterTextCounterState from(String chapterText) {
        int wordCount = CreateAudiobookDraftInput.countWords(chapterText);
        Severity severity = severityFor(wordCount);
        return new ChapterTextCounterState(wordCount, severity);
    }

    public int getWordCount() {
        return wordCount;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getCounterText() {
        return wordCount + " / " + CreateAudiobookDraftInput.MAX_CHAPTER_TEXT_WORDS + " words";
    }

    public String getMessage() {
        if (severity == Severity.ERROR) {
            return maxWordsErrorMessage();
        }
        if (severity == Severity.WARNING) {
            return "Approaching the " + CreateAudiobookDraftInput.MAX_CHAPTER_TEXT_WORDS + "-word limit.";
        }
        return "";
    }

    public static String maxWordsErrorMessage() {
        return "Chapter text must be " + CreateAudiobookDraftInput.MAX_CHAPTER_TEXT_WORDS + " words or fewer";
    }

    private static Severity severityFor(int wordCount) {
        if (wordCount > CreateAudiobookDraftInput.MAX_CHAPTER_TEXT_WORDS) {
            return Severity.ERROR;
        }
        if (wordCount >= WARNING_CHAPTER_TEXT_WORDS) {
            return Severity.WARNING;
        }
        return Severity.NORMAL;
    }
}
