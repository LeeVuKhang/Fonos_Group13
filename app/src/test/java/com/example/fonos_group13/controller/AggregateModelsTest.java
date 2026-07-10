package com.example.fonos_group13.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.fonos_group13.controller.core.RequestGate;
import com.example.fonos_group13.controller.profile.ProfileStats;
import com.example.fonos_group13.model.AudiobookGenerationStatus;
import com.example.fonos_group13.model.Book;
import com.example.fonos_group13.model.BookChapter;
import com.example.fonos_group13.model.ProgressKey;
import com.example.fonos_group13.model.UserProgress;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AggregateModelsTest {
    @Test
    public void progressKeyUsesBookAndChapterIdentity() {
        ProgressKey first = new ProgressKey("book-1", "chapter-1");
        ProgressKey same = new ProgressKey("book-1", "chapter-1");
        ProgressKey other = new ProgressKey("book-1", "chapter-2");

        assertEquals(first, same);
        assertEquals(first.hashCode(), same.hashCode());
        assertFalse(first.equals(other));
    }

    @Test
    public void profileStatsAggregateCompletedBooksAndListeningTime() {
        Book book = book("book-1");
        BookChapter first = chapter("book-1", "chapter-1");
        BookChapter second = chapter("book-1", "chapter-2");
        Map<String, List<BookChapter>> chapters = new HashMap<>();
        chapters.put(book.getId(), Arrays.asList(first, second));
        Map<ProgressKey, UserProgress> progress = new HashMap<>();
        progress.put(new ProgressKey("book-1", "chapter-1"), new UserProgress("book-1", "chapter-1", 1000, 1000, true));
        progress.put(new ProgressKey("book-1", "chapter-2"), new UserProgress("book-1", "chapter-2", 2000, 2000, true));

        ProfileStats stats = ProfileStats.calculate(Collections.singletonList(book), chapters, progress);

        assertEquals(1, stats.getCompletedBooks());
        assertEquals(3000, stats.getListenedMs());
    }

    @Test
    public void requestGateRejectsResultsAfterInvalidation() {
        RequestGate gate = new RequestGate();
        long request = gate.open();

        assertTrue(gate.isCurrent(request));
        gate.invalidate();
        assertFalse(gate.isCurrent(request));
    }

    private Book book(String id) {
        return new Book(
                id, "Title", "Author", "Chapter 1", "", null, null, null, null,
                0, "en-US", "female", null, AudiobookGenerationStatus.PUBLISHED,
                false, true, 0
        );
    }

    private BookChapter chapter(String bookId, String chapterId) {
        return new BookChapter(
                chapterId, bookId, chapterId, "", null, null, 1, 0, true, false
        );
    }
}
