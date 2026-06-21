package com.example.fonos_group13.data;

import com.example.fonos_group13.model.AudiobookGenerationStatus;

final class BookAccessPolicy {
    private BookAccessPolicy() {
    }

    static boolean canReadBook(
            boolean published,
            String creatorUid,
            AudiobookGenerationStatus generationStatus,
            String currentUid,
            BookAccessMode accessMode
    ) {
        if (published) {
            return true;
        }
        return accessMode == BookAccessMode.CREATOR_REVIEW_PREVIEW
                && generationStatus == AudiobookGenerationStatus.READY_FOR_REVIEW
                && sameNonBlankValue(creatorUid, currentUid);
    }

    static boolean shouldIncludeChapter(
            boolean bookPublished,
            boolean creatorPreviewAuthorized,
            boolean chapterPublished
    ) {
        return bookPublished ? chapterPublished : creatorPreviewAuthorized;
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
