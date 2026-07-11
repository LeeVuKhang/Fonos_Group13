package com.example.fonos_group13.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BookReviewPage {
    private final List<BookReview> reviews;
    private final BookReview viewerReview;
    private final String nextCursor;
    private final boolean hasMore;

    public BookReviewPage(List<BookReview> reviews, BookReview viewerReview, String nextCursor, boolean hasMore) {
        this.reviews = Collections.unmodifiableList(new ArrayList<>(
                reviews == null ? Collections.emptyList() : reviews
        ));
        this.viewerReview = viewerReview;
        this.nextCursor = nextCursor == null || nextCursor.trim().isEmpty() ? null : nextCursor.trim();
        this.hasMore = hasMore;
    }

    public List<BookReview> getReviews() { return reviews; }
    public BookReview getViewerReview() { return viewerReview; }
    public String getNextCursor() { return nextCursor; }
    public boolean hasMore() { return hasMore; }
}
