package com.example.fonos_group13.controller.profile;

import com.example.fonos_group13.model.Book;
import com.example.fonos_group13.model.BookChapter;
import com.example.fonos_group13.model.ProgressKey;
import com.example.fonos_group13.model.UserProgress;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class ProfileStats {
    private final int completedBooks;
    private final long listenedMs;

    public ProfileStats(int completedBooks, long listenedMs) {
        this.completedBooks = Math.max(completedBooks, 0);
        this.listenedMs = Math.max(listenedMs, 0);
    }

    public static ProfileStats calculate(
            List<Book> books,
            Map<String, List<BookChapter>> chaptersByBookId,
            Map<ProgressKey, UserProgress> progressByChapter
    ) {
        int completedBooks = 0;
        long listenedMs = 0;
        for (Book book : books == null ? Collections.<Book>emptyList() : books) {
            List<BookChapter> chapters = chaptersByBookId == null
                    ? Collections.emptyList()
                    : chaptersByBookId.get(book.getId());
            if (chapters == null || chapters.isEmpty()) {
                continue;
            }
            boolean allCompleted = true;
            for (BookChapter chapter : chapters) {
                UserProgress progress = progressByChapter == null
                        ? null
                        : progressByChapter.get(new ProgressKey(book.getId(), chapter.getId()));
                if (progress == null || !progress.isCompleted()) {
                    allCompleted = false;
                }
                if (progress != null) {
                    listenedMs += Math.max(progress.getPositionMs(), 0);
                }
            }
            if (allCompleted) {
                completedBooks++;
            }
        }
        return new ProfileStats(completedBooks, listenedMs);
    }

    public int getCompletedBooks() {
        return completedBooks;
    }

    public long getListenedMs() {
        return listenedMs;
    }
}
