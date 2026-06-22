package com.example.fonos_group13.model;

import com.google.firebase.firestore.DocumentSnapshot;

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

    public static UserProgress fromDocument(String bookId, DocumentSnapshot document) {
        return fromDocument(bookId, null, document);
    }

    public static UserProgress fromDocument(String bookId, String chapterId, DocumentSnapshot document) {
        long positionMs = FirestoreValueReader.longValue(document, "positionMs");
        long durationMs = FirestoreValueReader.longValue(document, "durationMs");
        boolean completed = FirestoreValueReader.booleanValue(document, "completed", false);
        String storedChapterId = FirestoreValueReader.string(document, "chapterId");
        return new UserProgress(
                bookId,
                storedChapterId == null || storedChapterId.trim().isEmpty() ? chapterId : storedChapterId,
                positionMs,
                durationMs,
                completed
        );
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
