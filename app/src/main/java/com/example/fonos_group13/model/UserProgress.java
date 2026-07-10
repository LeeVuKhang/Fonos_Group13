package com.example.fonos_group13.model;

public class UserProgress {
    private final String bookId;
    private final String chapterId;
    private final long positionMs;
    private final long durationMs;
    private final boolean completed;

    public UserProgress(String bookId, long positionMs, long durationMs, boolean completed) {
        this(bookId, null, positionMs, durationMs, completed);
    }

    public UserProgress(String bookId, String chapterId, long positionMs, long durationMs, boolean completed) {
        this.bookId = bookId;
        this.chapterId = chapterId;
        this.positionMs = positionMs;
        this.durationMs = durationMs;
        this.completed = completed;
    }

    public static UserProgress empty(String bookId) {
        return new UserProgress(bookId, 0, 0, false);
    }

    public static UserProgress empty(String bookId, String chapterId) {
        return new UserProgress(bookId, chapterId, 0, 0, false);
    }

    public String getBookId() {
        return bookId;
    }

    public String getChapterId() {
        return chapterId;
    }

    public long getPositionMs() {
        return positionMs;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public boolean isCompleted() {
        return completed;
    }
}
