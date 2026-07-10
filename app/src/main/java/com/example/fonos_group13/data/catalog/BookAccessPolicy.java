package com.example.fonos_group13.data.catalog;

import com.example.fonos_group13.model.AudiobookGenerationStatus;

final class BookAccessPolicy {
    private BookAccessPolicy() {
    }

    static boolean canReadBook(
            boolean published,
            boolean hiddenByCreator,
            String creatorUid,
            AudiobookGenerationStatus generationStatus,
            String currentUid,
            BookAccessMode accessMode
    ) {
        boolean ownerPreview = accessMode == BookAccessMode.CREATOR_REVIEW_PREVIEW
                && sameNonBlankValue(creatorUid, currentUid);
        if (hiddenByCreator) {
            return ownerPreview;
        }
        if (published) {
            return true;
        }
        return ownerPreview
                && generationStatus == AudiobookGenerationStatus.READY_FOR_REVIEW;
    }

    static boolean shouldIncludeChapter(
            boolean bookPublished,
            boolean creatorPreviewAuthorized,
            boolean chapterPublished
    ) {
        if (creatorPreviewAuthorized) {
            return true;
        }
        return bookPublished && chapterPublished;
    }

    static boolean canPreviewUnpublishedChapters(
            String creatorUid,
            AudiobookGenerationStatus generationStatus,
            String currentUid,
            BookAccessMode accessMode
    ) {
        return accessMode == BookAccessMode.CREATOR_REVIEW_PREVIEW
                && generationStatus == AudiobookGenerationStatus.READY_FOR_REVIEW
                && sameNonBlankValue(creatorUid, currentUid);
    }

    private static boolean sameNonBlankValue(String left, String right) {
        String normalizedLeft = trimToNull(left);
        String normalizedRight = trimToNull(right);
        return normalizedLeft != null && normalizedLeft.equals(normalizedRight);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
