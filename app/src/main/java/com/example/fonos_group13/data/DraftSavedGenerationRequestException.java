package com.example.fonos_group13.data;

public class DraftSavedGenerationRequestException extends Exception {
    private final String bookId;

    public DraftSavedGenerationRequestException(String bookId, Exception cause) {
        super("Draft saved, but generation request failed. Open My Uploads to retry.", cause);
        this.bookId = bookId;
    }

    public String getBookId() {
        return bookId;
    }
}
