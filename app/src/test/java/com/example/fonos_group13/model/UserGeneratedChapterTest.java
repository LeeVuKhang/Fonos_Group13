package com.example.fonos_group13.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class UserGeneratedChapterTest {
    @Test
    public void pendingChapterCanBeCanceledButNotEditedOrPreviewed() {
        UserGeneratedChapter chapter = chapter(AudiobookGenerationStatus.PENDING_GENERATION, false, false);

        assertTrue(chapter.canDelete());
        assertEquals("Cancel Chapter", chapter.getDeleteActionLabel());
        assertFalse(chapter.canEdit());
        assertFalse(chapter.canPreview());
    }

    @Test
    public void readyChapterCanPreviewAndSoftDeleteWhenUnpublished() {
        UserGeneratedChapter chapter = chapter(AudiobookGenerationStatus.READY_FOR_REVIEW, false, true);

        assertTrue(chapter.canPreview());
        assertTrue(chapter.canDelete());
        assertEquals("Delete", chapter.getDeleteActionLabel());
    }

    @Test
    public void publishedChapterCannotBeDeleted() {
        UserGeneratedChapter chapter = chapter(AudiobookGenerationStatus.PUBLISHED, true, true);

        assertTrue(chapter.canPreview());
        assertFalse(chapter.canDelete());
        assertFalse(chapter.canRequestGeneration());
    }

    private UserGeneratedChapter chapter(
            AudiobookGenerationStatus status,
            boolean published,
            boolean hasAudio
    ) {
        return new UserGeneratedChapter(
                "chapter_1",
                "Chapter 1",
                status,
                published,
                0,
                0,
                null,
                hasAudio
        );
    }
}
