package com.example.fonos_group13.model;

public final class BookReview {
    private final String reviewerDisplayName;
    private final int rating;
    private final String comment;
    private final String createdAt;
    private final String updatedAt;
    private final boolean edited;

    public BookReview(
            String reviewerDisplayName,
            int rating,
            String comment,
            String createdAt,
            String updatedAt,
            boolean edited
    ) {
        this.reviewerDisplayName = reviewerDisplayName == null || reviewerDisplayName.trim().isEmpty()
                ? "Reader" : reviewerDisplayName.trim();
        this.rating = Math.max(1, Math.min(5, rating));
        this.comment = comment == null || comment.trim().isEmpty() ? null : comment.trim();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.edited = edited;
    }

    public String getReviewerDisplayName() { return reviewerDisplayName; }
    public int getRating() { return rating; }
    public String getComment() { return comment; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public boolean isEdited() { return edited; }
}
