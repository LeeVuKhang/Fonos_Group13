package com.example.fonos_group13;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.fonos_group13.data.catalog.BookRepository;
import com.example.fonos_group13.data.core.RepositoryCallback;
import com.example.fonos_group13.data.library.DownloadedAudioRepository;
import com.example.fonos_group13.data.library.ProgressRepository;
import com.example.fonos_group13.data.library.SavedBookRepository;
import com.example.fonos_group13.model.Book;
import com.example.fonos_group13.model.BookChapter;
import com.example.fonos_group13.model.UserProgress;
import com.example.fonos_group13.ui.BookCoverLoader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LibraryActivity extends AppCompatActivity {
    private enum LibraryFilter {
        LISTENING,
        DOWNLOADED,
        FINISHED
    }

    private BookRepository bookRepository;
    private ProgressRepository progressRepository;
    private DownloadedAudioRepository downloadedAudioRepository;
    private SavedBookRepository savedBookRepository;
    private final List<Book> allBooks = new ArrayList<>();
    private final Set<String> savedBookIds = new HashSet<>();
    private final Map<String, List<BookChapter>> chaptersByBookId = new HashMap<>();
    private final Map<String, UserProgress> progressByChapterKey = new HashMap<>();
    private LibraryFilter currentFilter = LibraryFilter.LISTENING;
    private TextView chipListening;
    private TextView chipDownloaded;
    private TextView chipFinished;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_library);
        bookRepository = new BookRepository(this);
        progressRepository = new ProgressRepository(this);
        downloadedAudioRepository = new DownloadedAudioRepository(this);
        savedBookRepository = new SavedBookRepository(this);

        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        setupFilterChips();
        setupBottomNavigation();
        setBooks(new ArrayList<>());
        showLibraryMessage("Loading your library...");
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBooks();
    }

    private void setupFilterChips() {
        chipListening = findViewById(R.id.chip_listening);
        chipDownloaded = findViewById(R.id.chip_downloaded);
        chipFinished = findViewById(R.id.chip_finished);

        if (chipListening != null) {
            chipListening.setOnClickListener(v -> selectFilter(LibraryFilter.LISTENING));
        }
        if (chipDownloaded != null) {
            chipDownloaded.setOnClickListener(v -> selectFilter(LibraryFilter.DOWNLOADED));
        }
        if (chipFinished != null) {
            chipFinished.setOnClickListener(v -> selectFilter(LibraryFilter.FINISHED));
        }
        updateFilterChips();
    }

    private void selectFilter(LibraryFilter filter) {
        currentFilter = filter;
        updateFilterChips();
        refreshLibraryRows();
    }

    private void updateFilterChips() {
        bindFilterChip(chipListening, currentFilter == LibraryFilter.LISTENING);
        bindFilterChip(chipDownloaded, currentFilter == LibraryFilter.DOWNLOADED);
        bindFilterChip(chipFinished, currentFilter == LibraryFilter.FINISHED);
    }

    private void bindFilterChip(TextView chip, boolean active) {
        if (chip == null) {
            return;
        }
        chip.setBackgroundResource(active ? R.drawable.bg_chip_active : R.drawable.bg_chip_white);
        chip.setTextColor(getColor(active ? R.color.white : R.color.text_muted));
        chip.setTypeface(null, active ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
    }

    private void loadBooks() {
        savedBookRepository.getSavedBookIds(new RepositoryCallback<List<String>>() {
            @Override
            public void onSuccess(List<String> bookIds) {
                savedBookIds.clear();
                if (bookIds != null) {
                    savedBookIds.addAll(bookIds);
                }
                if (savedBookIds.isEmpty()) {
                    setBooks(new ArrayList<>());
                    return;
                }
                loadSavedBooks();
            }

            @Override
            public void onError(Exception exception) {
                setBooks(new ArrayList<>());
                showLibraryMessage("Could not load your saved library.");
                Toast.makeText(LibraryActivity.this, "Could not load your saved library.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadSavedBooks() {
        bookRepository.getPublishedBooks(new RepositoryCallback<List<Book>>() {
            @Override
            public void onSuccess(List<Book> books) {
                List<Book> savedBooks = new ArrayList<>();
                if (books != null) {
                    for (Book book : books) {
                        if (savedBookIds.contains(book.getId())) {
                            savedBooks.add(book);
                        }
                    }
                }
                setBooks(savedBooks);
            }

            @Override
            public void onError(Exception exception) {
                setBooks(new ArrayList<>());
                showLibraryMessage("Could not load Firestore library.");
                Toast.makeText(LibraryActivity.this, "Could not load Firestore library.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setBooks(List<Book> books) {
        allBooks.clear();
        chaptersByBookId.clear();
        progressByChapterKey.clear();
        if (books != null) {
            allBooks.addAll(books);
        }

        for (Book book : allBooks) {
            loadChapters(book);
        }
        refreshLibraryRows();
    }

    private void loadChapters(Book book) {
        bookRepository.getChapters(book.getId(), new RepositoryCallback<List<BookChapter>>() {
            @Override
            public void onSuccess(List<BookChapter> chapters) {
                if (!containsBook(book.getId())) {
                    return;
                }
                chaptersByBookId.put(book.getId(), chapters == null ? new ArrayList<>() : new ArrayList<>(chapters));
                if (chapters != null) {
                    for (BookChapter chapter : chapters) {
                        progressByChapterKey.put(chapterKey(chapter), UserProgress.empty(book.getId(), chapter.getId()));
                        loadProgress(chapter);
                    }
                }
                refreshLibraryRows();
            }

            @Override
            public void onError(Exception exception) {
                chaptersByBookId.put(book.getId(), new ArrayList<>());
                refreshLibraryRows();
            }
        });
    }

    private void refreshAllProgress() {
        for (List<BookChapter> chapters : chaptersByBookId.values()) {
            for (BookChapter chapter : chapters) {
                loadProgress(chapter);
            }
        }
    }

    private void loadProgress(BookChapter chapter) {
        progressRepository.getProgress(chapter.getBookId(), chapter.getId(), new RepositoryCallback<UserProgress>() {
            @Override
            public void onSuccess(UserProgress progress) {
                if (containsBook(chapter.getBookId())) {
                    progressByChapterKey.put(chapterKey(chapter), progress);
                    refreshLibraryRows();
                }
            }

            @Override
            public void onError(Exception exception) {
            }
        });
    }

    private boolean containsBook(String bookId) {
        for (Book book : allBooks) {
            if (book.getId().equals(bookId)) {
                return true;
            }
        }
        return false;
    }

    private void refreshLibraryRows() {
        int[] rowIds = {
                R.id.library_book_1,
                R.id.library_book_2,
                R.id.library_book_3
        };
        int[] coverIds = {
                R.id.library_cover_1,
                R.id.library_cover_2,
                R.id.library_cover_3
        };
        List<Book> visibleBooks = filterBooks();
        updateEmptyState(visibleBooks.isEmpty());

        for (int i = 0; i < rowIds.length; i++) {
            View row = findViewById(rowIds[i]);
            Book book = i < visibleBooks.size() ? visibleBooks.get(i) : null;
            bindLibraryRow(row, book, coverIds[i]);
        }
    }

    private List<Book> filterBooks() {
        List<Book> books = new ArrayList<>();
        for (Book book : allBooks) {
            List<BookChapter> chapters = chaptersForBook(book);
            int completedCount = completedChapterCount(chapters);
            if (currentFilter == LibraryFilter.DOWNLOADED) {
                if (downloadedChapterCount(chapters) > 0) {
                    books.add(book);
                }
            } else if (currentFilter == LibraryFilter.FINISHED) {
                if (!chapters.isEmpty() && completedCount == chapters.size()) {
                    books.add(book);
                }
            } else if (chapters.isEmpty() || completedCount < chapters.size()) {
                books.add(book);
            }
        }
        return books;
    }

    private void bindLibraryRow(View row, Book book, int coverId) {
        if (row == null) {
            return;
        }
        if (book == null) {
            row.setVisibility(View.GONE);
            row.setOnClickListener(null);
            return;
        }
        row.setVisibility(View.VISIBLE);
        row.setOnClickListener(v -> openBookDetail(book));
        ImageView cover = BookCoverLoader.findCoverView(row, coverId);
        BookCoverLoader.load(cover, book);

        List<TextView> textViews = new ArrayList<>();
        collectTextViews(row, textViews);
        if (textViews.size() >= 2) {
            textViews.get(0).setText(book.getTitle());
            textViews.get(1).setText(book.getAuthor());
        }
        bindProgress(row, book);
    }

    private void bindProgress(View row, Book book) {
        List<BookChapter> chapters = chaptersForBook(book);
        int percent = aggregatePercent(chapters);
        int downloadedCount = downloadedChapterCount(chapters);

        List<TextView> textViews = new ArrayList<>();
        collectTextViews(row, textViews);
        if (textViews.size() >= 4) {
            textViews.get(2).setText(percent + "% COMPLETED");
            if (currentFilter == LibraryFilter.DOWNLOADED) {
                textViews.get(3).setText(downloadedCount + "/" + Math.max(chapters.size(), 1) + " DOWNLOADED");
            } else if (!chapters.isEmpty() && completedChapterCount(chapters) == chapters.size()) {
                textViews.get(3).setText("FINISHED");
            } else {
                textViews.get(3).setText(formatRemaining(remainingMs(chapters)) + " LEFT");
            }
        }

        ProgressBar progressBar = findProgressBar(row);
        if (progressBar != null) {
            progressBar.setProgress(percent);
        }
    }

    private List<BookChapter> chaptersForBook(Book book) {
        List<BookChapter> chapters = chaptersByBookId.get(book.getId());
        if (chapters == null || chapters.isEmpty()) {
            List<BookChapter> fallback = new ArrayList<>();
            fallback.add(BookChapter.fromLegacyBook(book));
            return fallback;
        }
        return chapters;
    }

    private int completedChapterCount(List<BookChapter> chapters) {
        int completed = 0;
        for (BookChapter chapter : chapters) {
            UserProgress progress = progressByChapterKey.get(chapterKey(chapter));
            if (progress != null && progress.isCompleted()) {
                completed++;
            }
        }
        return completed;
    }

    private int downloadedChapterCount(List<BookChapter> chapters) {
        int downloaded = 0;
        for (BookChapter chapter : chapters) {
            if (downloadedAudioRepository.isDownloaded(chapter.getBookId(), chapter.getId())) {
                downloaded++;
            }
        }
        return downloaded;
    }

    private int aggregatePercent(List<BookChapter> chapters) {
        long totalDurationMs = 0;
        long totalPositionMs = 0;
        for (BookChapter chapter : chapters) {
            UserProgress progress = progressByChapterKey.get(chapterKey(chapter));
            long durationMs = durationMs(chapter, progress);
            totalDurationMs += durationMs;
            totalPositionMs += progress == null ? 0 : Math.min(Math.max(progress.getPositionMs(), 0), durationMs);
        }
        if (totalDurationMs <= 0) {
            return completedChapterCount(chapters) == chapters.size() && !chapters.isEmpty() ? 100 : 0;
        }
        return Math.min(100, Math.round(totalPositionMs * 100f / totalDurationMs));
    }

    private long remainingMs(List<BookChapter> chapters) {
        long remainingMs = 0;
        for (BookChapter chapter : chapters) {
            UserProgress progress = progressByChapterKey.get(chapterKey(chapter));
            long durationMs = durationMs(chapter, progress);
            long positionMs = progress == null ? 0 : Math.max(progress.getPositionMs(), 0);
            remainingMs += Math.max(durationMs - positionMs, 0);
        }
        return remainingMs;
    }

    private long durationMs(BookChapter chapter, UserProgress progress) {
        if (progress != null && progress.getDurationMs() > 0) {
            return progress.getDurationMs();
        }
        return Math.max(chapter.getDurationSec(), 0) * 1000L;
    }

    private void updateEmptyState(boolean empty) {
        TextView emptyState = findViewById(R.id.library_empty_state);
        if (emptyState == null) {
            return;
        }
        if (!empty) {
            emptyState.setVisibility(View.GONE);
            return;
        }

        if (allBooks.isEmpty()) {
            emptyState.setText("No audiobooks in your library yet.");
        } else if (currentFilter == LibraryFilter.DOWNLOADED) {
            emptyState.setText("No downloaded chapters yet.");
        } else if (currentFilter == LibraryFilter.FINISHED) {
            emptyState.setText("No finished audiobooks yet.");
        } else {
            emptyState.setText("No active audiobooks yet.");
        }
        emptyState.setVisibility(View.VISIBLE);
    }

    private void showLibraryMessage(String message) {
        TextView emptyState = findViewById(R.id.library_empty_state);
        if (emptyState == null) {
            return;
        }
        emptyState.setText(message);
        emptyState.setVisibility(View.VISIBLE);
    }

    private String formatRemaining(long remainingMs) {
        long minutes = Math.max(remainingMs, 0) / 60000L;
        long hours = minutes / 60L;
        long mins = minutes % 60L;
        return hours + "H " + String.format("%02d", mins) + "M";
    }

    private void collectTextViews(View view, List<TextView> textViews) {
        if (view instanceof TextView) {
            textViews.add((TextView) view);
            return;
        }
        if (!(view instanceof ViewGroup)) {
            return;
        }
        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) {
            collectTextViews(group.getChildAt(i), textViews);
        }
    }

    private ProgressBar findProgressBar(View view) {
        if (view instanceof ProgressBar) {
            return (ProgressBar) view;
        }
        if (!(view instanceof ViewGroup)) {
            return null;
        }
        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) {
            ProgressBar progressBar = findProgressBar(group.getChildAt(i));
            if (progressBar != null) {
                return progressBar;
            }
        }
        return null;
    }

    private void openBookDetail(Book book) {
        Intent intent = new Intent(this, BookDetailActivity.class);
        intent.putExtra(BookDetailActivity.EXTRA_BOOK_ID, book.getId());
        startActivity(intent);
    }

    private String chapterKey(BookChapter chapter) {
        return ProgressRepository.progressDocumentId(chapter.getBookId(), chapter.getId());
    }

    private void setupBottomNavigation() {
        View navDiscover = findViewById(R.id.nav_discover);
        View navSearch = findViewById(R.id.nav_search);
        View navProfile = findViewById(R.id.nav_profile);

        if (navDiscover != null) {
            navDiscover.setOnClickListener(v -> {
                startActivity(new Intent(this, DiscoverActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
                overridePendingTransition(0, 0);
                finish();
            });
        }
        if (navSearch != null) {
            navSearch.setOnClickListener(v -> {
                startActivity(new Intent(this, SearchActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
                overridePendingTransition(0, 0);
                finish();
            });
        }
        if (navProfile != null) {
            navProfile.setOnClickListener(v -> {
                startActivity(new Intent(this, ProfileActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
                overridePendingTransition(0, 0);
                finish();
            });
        }
    }
}
