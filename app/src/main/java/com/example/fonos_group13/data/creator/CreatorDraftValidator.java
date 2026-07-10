package com.example.fonos_group13.data.creator;

import com.example.fonos_group13.model.CreateAudiobookDraftInput;
import com.example.fonos_group13.model.CreateChapterDraftInput;

public final class CreatorDraftValidator {
    public Exception validate(CreateAudiobookDraftInput input) {
        if (input == null) {
            return new IllegalArgumentException("Missing audiobook draft.");
        }
        if (isBlank(input.getTitle())) {
            return new IllegalArgumentException("Title is required.");
        }
        if (isBlank(input.getAuthor())) {
            return new IllegalArgumentException("Author is required.");
        }
        if (isBlank(input.getChapterText())) {
            return new IllegalArgumentException("Chapter text is required.");
        }
        if (input.getTitle().length() > CreateAudiobookDraftInput.MAX_TITLE_CHARS) {
            return new IllegalArgumentException("Title must be 120 characters or fewer.");
        }
        if (input.getAuthor().length() > CreateAudiobookDraftInput.MAX_AUTHOR_CHARS) {
            return new IllegalArgumentException("Author must be 120 characters or fewer.");
        }
        if (CreateAudiobookDraftInput.countWords(input.getChapterText())
                > CreateAudiobookDraftInput.MAX_CHAPTER_TEXT_WORDS) {
            return new IllegalArgumentException("Chapter text must be 3500 words or fewer.");
        }
        return null;
    }

    public Exception validate(CreateChapterDraftInput input) {
        if (input == null) {
            return new IllegalArgumentException("Missing chapter draft.");
        }
        if (isBlank(input.getChapterTitle())) {
            return new IllegalArgumentException("Chapter title is required.");
        }
        if (isBlank(input.getChapterText())) {
            return new IllegalArgumentException("Chapter text is required.");
        }
        if (CreateChapterDraftInput.countWords(input.getChapterText())
                > CreateChapterDraftInput.MAX_CHAPTER_TEXT_WORDS) {
            return new IllegalArgumentException("Chapter text must be 3500 words or fewer.");
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
