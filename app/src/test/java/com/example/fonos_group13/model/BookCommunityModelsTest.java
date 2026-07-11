package com.example.fonos_group13.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import com.example.fonos_group13.ui.BookRatingFormatter;

import org.junit.Test;

import java.util.Collections;

public class BookCommunityModelsTest {
    @Test
    public void bookCarriesSafeCommunityAggregates() {
        Book book = new Book(
                "book-1", "Title", "Author", "Chapter", "Sample", null, null,
                null, null, 0, "en-US", "female", null,
                AudiobookGenerationStatus.PUBLISHED, false, true, 0,
                4.25, 12, 7
        );

        assertEquals(4.25, book.getRatingAverage(), 0.001);
        assertEquals(12, book.getRatingCount());
        assertEquals(7, book.getSaveCount());
        assertEquals("★ 4.3 (12)", BookRatingFormatter.format(book));
    }

    @Test
    public void reviewPageKeepsViewerReviewSeparateFromCommentFeed() {
        BookReview viewer = new BookReview("Reader", 4, null, null, null, false);
        BookReviewPage page = new BookReviewPage(Collections.emptyList(), viewer, null, false);

        assertEquals(0, page.getReviews().size());
        assertEquals(viewer, page.getViewerReview());
        assertNull(page.getNextCursor());
        assertFalse(page.hasMore());
    }
}
