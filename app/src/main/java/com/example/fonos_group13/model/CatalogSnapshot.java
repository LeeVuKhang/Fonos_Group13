package com.example.fonos_group13.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CatalogSnapshot {
    private final List<Book> books;
    private final Map<String, List<BookChapter>> chaptersByBookId;
    private final boolean partial;

    public CatalogSnapshot(
            List<Book> books,
            Map<String, List<BookChapter>> chaptersByBookId,
            boolean partial
    ) {
        this.books = Collections.unmodifiableList(new ArrayList<>(books));
        Map<String, List<BookChapter>> chapterCopy = new HashMap<>();
        for (Map.Entry<String, List<BookChapter>> entry : chaptersByBookId.entrySet()) {
            chapterCopy.put(
                    entry.getKey(),
                    Collections.unmodifiableList(new ArrayList<>(entry.getValue()))
            );
        }
        this.chaptersByBookId = Collections.unmodifiableMap(chapterCopy);
        this.partial = partial;
    }

    public List<Book> getBooks() {
        return books;
    }

    public Map<String, List<BookChapter>> getChaptersByBookId() {
        return chaptersByBookId;
    }

    public boolean isPartial() {
        return partial;
    }
}
