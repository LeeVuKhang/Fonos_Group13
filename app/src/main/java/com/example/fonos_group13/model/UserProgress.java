package com.example.fonos_group13.model;

import com.google.firebase.firestore.DocumentSnapshot;

public class UserProgress {
    private final String bookId;
    private final long positionMs;
    private final long durationMs;
    private final boolean completed;

    public UserProgress(String bookId, long positionMs, long durationMs, boolean completed) {
        this.bookId = bookId;
        this.positionMs = positionMs;
        this.durationMs = durationMs;
        this.completed = completed;
    }

    public static UserProgress empty(String bookId) {
        return new UserProgress(bookId, 0, 0, false);
    }

    public static UserProgress fromDocument(String bookId, DocumentSnapshot document) {
        Long positionMs = document.getLong("positionMs");
        Long durationMs = document.getLong("durationMs");
        Boolean completed = document.getBoolean("completed");
        return new UserProgress(
                bookId,
                positionMs == null ? 0 : positionMs,
                durationMs == null ? 0 : durationMs,
                completed != null && completed
        );
    }

    public String getBookId() {
        return bookId;
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
