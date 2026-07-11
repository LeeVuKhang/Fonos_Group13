package com.example.fonos_group13.ui;

import com.example.fonos_group13.model.Book;

import java.util.Locale;

public final class BookRatingFormatter {
    private BookRatingFormatter() {
    }

    public static String format(Book book) {
        if (book == null || book.getRatingCount() <= 0) {
            return "No ratings yet";
        }
        return String.format(Locale.US, "★ %.1f (%d)", book.getRatingAverage(), book.getRatingCount());
    }
}
