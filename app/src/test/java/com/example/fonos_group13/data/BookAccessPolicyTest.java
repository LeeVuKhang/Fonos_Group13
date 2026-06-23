package com.example.fonos_group13.data;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.fonos_group13.model.AudiobookGenerationStatus;

import org.junit.Test;

public class BookAccessPolicyTest {
    private static final String CREATOR_UID = "creator-123";

    @Test
    public void publishedBookIsReadableWithoutPreviewMode() {
        assertTrue(BookAccessPolicy.canReadBook(
                true,
                null,
                AudiobookGenerationStatus.DRAFT,
                null,
                BookAccessMode.PUBLISHED_ONLY
        ));
    }

    @Test
    public void unpublishedBookIsNotReadableInPublishedOnlyMode() {
        assertFalse(BookAccessPolicy.canReadBook(
                false,
                CREATOR_UID,
                AudiobookGenerationStatus.READY_FOR_REVIEW,
                CREATOR_UID,
                BookAccessMode.PUBLISHED_ONLY
        ));
    }

    @Test
    public void readyForReviewBookIsReadableByItsCreatorInPreviewMode() {
        assertTrue(BookAccessPolicy.canReadBook(
                false,
                CREATOR_UID,
                AudiobookGenerationStatus.READY_FOR_REVIEW,
                CREATOR_UID,
                BookAccessMode.CREATOR_REVIEW_PREVIEW
        ));
    }

    @Test
    public void previewModeDoesNotAuthorizeAnotherUser() {
        assertFalse(BookAccessPolicy.canReadBook(
                false,
                CREATOR_UID,
                AudiobookGenerationStatus.READY_FOR_REVIEW,
                "different-user",
                BookAccessMode.CREATOR_REVIEW_PREVIEW
        ));
    }

    @Test
    public void previewModeRequiresAuthentication() {
        assertFalse(BookAccessPolicy.canReadBook(
                false,
                CREATOR_UID,
                AudiobookGenerationStatus.READY_FOR_REVIEW,
                null,
                BookAccessMode.CREATOR_REVIEW_PREVIEW
        ));
    }

    @Test
    public void previewModeRejectsOtherGenerationStatuses() {
        assertFalse(BookAccessPolicy.canReadBook(
                false,
                CREATOR_UID,
                AudiobookGenerationStatus.PENDING_GENERATION,
                CREATOR_UID,
                BookAccessMode.CREATOR_REVIEW_PREVIEW
        ));
    }

    @Test
    public void publishedBookOnlyIncludesPublishedChapters() {
        assertFalse(BookAccessPolicy.shouldIncludeChapter(true, false, false));
    }

    @Test
    public void creatorPreviewIncludesUnpublishedChapters() {
        assertTrue(BookAccessPolicy.shouldIncludeChapter(false, true, false));
    }

    @Test
    public void unauthorizedPreviewDoesNotIncludeUnpublishedChapters() {
        assertFalse(BookAccessPolicy.shouldIncludeChapter(false, false, false));
    }
}
