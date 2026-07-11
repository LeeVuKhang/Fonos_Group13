package com.example.fonos_group13.model;

public final class ReviewMutationResult {
    private final BookReview review;
    private final boolean deleted;
    private final double ratingAverage;
    private final int ratingCount;

    public ReviewMutationResult(BookReview review, boolean deleted, double ratingAverage, int ratingCount) {
        this.review = review;
        this.deleted = deleted;
        this.ratingAverage = Math.max(0, ratingAverage);
        this.ratingCount = Math.max(0, ratingCount);
    }

    public BookReview getReview() { return review; }
    public boolean isDeleted() { return deleted; }
    public double getRatingAverage() { return ratingAverage; }
    public int getRatingCount() { return ratingCount; }
}
