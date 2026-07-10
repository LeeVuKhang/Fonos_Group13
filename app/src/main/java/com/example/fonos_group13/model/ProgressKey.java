package com.example.fonos_group13.model;

import java.util.Objects;

public final class ProgressKey {
    private final String bookId;
    private final String chapterId;

    public ProgressKey(String bookId, String chapterId) {
        this.bookId = normalize(bookId);
        this.chapterId = normalize(chapterId);
    }

    public static ProgressKey from(UserProgress progress) {
        return new ProgressKey(progress.getBookId(), progress.getChapterId());
    }

    public String getBookId() {
        return bookId;
    }

    public String getChapterId() {
        return chapterId;
    }

    @Override
    public boolean equals(Object value) {
        if (this == value) {
            return true;
        }
        if (!(value instanceof ProgressKey)) {
            return false;
        }
        ProgressKey other = (ProgressKey) value;
        return bookId.equals(other.bookId) && chapterId.equals(other.chapterId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bookId, chapterId);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
