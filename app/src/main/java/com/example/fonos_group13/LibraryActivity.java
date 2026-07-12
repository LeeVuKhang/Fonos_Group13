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

import com.example.fonos_group13.controller.library.LibraryController;
import com.example.fonos_group13.controller.library.LibraryViewState;
import com.example.fonos_group13.data.repository.AudioDownloadRepository;
import com.example.fonos_group13.model.Book;
import com.example.fonos_group13.model.BookChapter;
import com.example.fonos_group13.model.ProgressKey;
import com.example.fonos_group13.model.UserProgress;
import com.example.fonos_group13.ui.BookCoverLoader;
import com.example.fonos_group13.ui.BookRatingBinder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LibraryActivity extends AppCompatActivity implements LibraryController.View {
    private enum LibraryFilter {
        LISTENING,
        DOWNLOADED,
        FINISHED
    }

    private AudioDownloadRepository downloadedAudioRepository;
    private LibraryController libraryController;
    private final List<Book> allBooks = new ArrayList<>();
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
        AppContainer container = FonosApplication.container(this);
        downloadedAudioRepository = container.audioDownloadRepository();
        libraryController = new LibraryController(
                container.savedBooksRepository(),
                container.catalogRepository(),
                container.progressRepository(),
                this
        );

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
        renderLibrary(LibraryViewState.loading());
        showLibraryMessage("Loading your library...");
    }

    @Override
    protected void onResume() {
        super.onResume();
        libraryController.start();
    }

    @Override
    protected void onStop() {
        libraryController.stop();
        super.onStop();
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
        chip.setSelected(active);
    }

    @Override
    public void renderLibrary(LibraryViewState state) {
        allBooks.clear();
        chaptersByBookId.clear();
        progressByChapterKey.clear();
        if (state.getStatus() == LibraryViewState.Status.LOADING) {
            refreshLibraryRows();
            showLibraryMessage("Loading your library...");
            return;
        }
        if (state.getStatus() == LibraryViewState.Status.ERROR) {
            refreshLibraryRows();
            showLibraryMessage("Could not load your saved library.");
            Toast.makeText(this, "Could not load your saved library.", Toast.LENGTH_SHORT).show();
            return;
        }
        allBooks.addAll(state.getBooks());
        chaptersByBookId.putAll(state.getChaptersByBookId());
        for (Map.Entry<ProgressKey, UserProgress> entry : state.getProgressByChapter().entrySet()) {
            ProgressKey key = entry.getKey();
            progressByChapterKey.put(key.getBookId() + "__" + key.getChapterId(), entry.getValue());
        }
        refreshLibraryRows();
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
        row.setFocusable(true);
        ImageView cover = BookCoverLoader.findCoverView(row, coverId);
        BookCoverLoader.load(cover, book);

        List<TextView> textViews = new ArrayList<>();
        collectTextViews(row, textViews);
        if (textViews.size() >= 2) {
            textViews.get(0).setText(book.getTitle());
            textViews.get(1).setText(book.getAuthor());
        }
        bindProgress(row, book);
        BookRatingBinder.bind(row, book);
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
            if (!BookRatingBinder.VIEW_TAG.equals(view.getTag())) textViews.add((TextView) view);
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
        return chapter.getBookId() + "__" + chapter.getId();
    }

    private void setupBottomNavigation() {
        View navDiscover = findViewById(R.id.nav_discover);
        View navSearch = findViewById(R.id.nav_search);
        View navLibrary = findViewById(R.id.nav_library);
        View navProfile = findViewById(R.id.nav_profile);

        if (navDiscover != null) {
            navDiscover.setSelected(false);
        }
        if (navSearch != null) {
            navSearch.setSelected(false);
        }
        if (navLibrary != null) {
            navLibrary.setSelected(true);
        }
        if (navProfile != null) {
            navProfile.setSelected(false);
        }

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
