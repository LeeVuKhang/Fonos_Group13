package com.example.fonos_group13;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.fonos_group13.data.BookRepository;
import com.example.fonos_group13.data.DownloadedAudioRepository;
import com.example.fonos_group13.data.ProgressRepository;
import com.example.fonos_group13.data.RepositoryCallback;
import com.example.fonos_group13.data.SavedBookRepository;
import com.example.fonos_group13.model.Book;
import com.example.fonos_group13.model.BookChapter;
import com.example.fonos_group13.model.UserProgress;
import com.example.fonos_group13.ui.BookCoverLoader;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BookDetailActivity extends AppCompatActivity {
    public static final String EXTRA_BOOK_ID = "book_id";

    private BookRepository bookRepository;
    private ProgressRepository progressRepository;
    private DownloadedAudioRepository downloadedAudioRepository;
    private SavedBookRepository savedBookRepository;
    private Book currentBook;
    private String requestedBookId;
    private String downloadingChapterId;
    private boolean currentBookSaved;
    private boolean saveBookLoading;

    private final List<BookChapter> chapters = new ArrayList<>();
    private final Map<String, UserProgress> progressByChapterId = new HashMap<>();

    private ImageView coverView;
    private TextView titleView;
    private TextView authorView;
    private TextView summaryView;
    private TextView messageView;
    private FloatingActionButton playResumeButton;
    private ImageView saveBookButton;
    private ImageView downloadAllButton;
    private LinearLayout chaptersContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_book_detail);

        bookRepository = new BookRepository(this);
        progressRepository = new ProgressRepository(this);
        downloadedAudioRepository = new DownloadedAudioRepository(this);
        savedBookRepository = new SavedBookRepository(this);

        bindViews();
        setupInsets();
        setupControls();
        handleIntent(getIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshProgress();
        renderChapters();
    }

    private void bindViews() {
        coverView = findViewById(R.id.detail_cover);
        titleView = findViewById(R.id.detail_title);
        authorView = findViewById(R.id.detail_author);
        summaryView = findViewById(R.id.detail_summary);
        messageView = findViewById(R.id.detail_message);
        playResumeButton = findViewById(R.id.btn_play_resume);
        saveBookButton = findViewById(R.id.btn_save_book);
        downloadAllButton = findViewById(R.id.btn_download_all);
        chaptersContainer = findViewById(R.id.chapters_container);
    }

    private void setupInsets() {
        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }
    }

    private void setupControls() {
        View back = findViewById(R.id.btn_back);
        if (back != null) {
            back.setOnClickListener(v -> finish());
        }
        if (playResumeButton != null) {
            playResumeButton.setOnClickListener(v -> {
                BookChapter chapter = findResumeChapter();
                if (chapter != null) {
                    openReader(chapter, true);
                }
            });
        }
        if (saveBookButton != null) {
            saveBookButton.setOnClickListener(v -> toggleSavedBook());
            updateSaveBookButton();
        }
        if (downloadAllButton != null) {
            downloadAllButton.setEnabled(false);
            downloadAllButton.setAlpha(0.35f);
        }
    }

    private void handleIntent(Intent intent) {
        String bookId = intent == null ? null : trimToNull(intent.getStringExtra(EXTRA_BOOK_ID));
        if (bookId == null) {
            Toast.makeText(this, "Missing book id.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        requestedBookId = bookId;
        loadBook(bookId);
    }

    private void loadBook(String bookId) {
        showMessage("Loading chapters...");
        bookRepository.getBook(bookId, new RepositoryCallback<Book>() {
            @Override
            public void onSuccess(Book book) {
                if (!bookId.equals(requestedBookId)) {
                    return;
                }
                currentBook = book;
                bindBookHeader(book);
                loadSavedState(bookId);
                loadChapters(bookId);
            }

            @Override
            public void onError(Exception exception) {
                if (!bookId.equals(requestedBookId)) {
                    return;
                }
                showMessage("Could not load this book.");
                Toast.makeText(BookDetailActivity.this, "Could not load this book from Firestore.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindBookHeader(Book book) {
        if (coverView != null) {
            BookCoverLoader.load(coverView, book);
        }
        if (titleView != null) {
            titleView.setText(book.getTitle());
        }
        if (authorView != null) {
            authorView.setText(book.getAuthor());
        }
    }

    private void loadSavedState(String bookId) {
        saveBookLoading = true;
        updateSaveBookButton();
        savedBookRepository.isSaved(bookId, new RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean saved) {
                if (!bookId.equals(requestedBookId)) {
                    return;
                }
                currentBookSaved = saved != null && saved;
                saveBookLoading = false;
                updateSaveBookButton();
            }

            @Override
            public void onError(Exception exception) {
                if (!bookId.equals(requestedBookId)) {
                    return;
                }
                currentBookSaved = false;
                saveBookLoading = false;
                updateSaveBookButton();
            }
        });
    }

    private void loadChapters(String bookId) {
        bookRepository.getChapters(bookId, new RepositoryCallback<List<BookChapter>>() {
            @Override
            public void onSuccess(List<BookChapter> data) {
                if (!bookId.equals(requestedBookId)) {
                    return;
                }
                setChapters(data);
            }

            @Override
            public void onError(Exception exception) {
                if (!bookId.equals(requestedBookId)) {
                    return;
                }
                chapters.clear();
                progressByChapterId.clear();
                updateSummary();
                renderChapters();
                showMessage("Could not load chapters.");
            }
        });
    }

    private void setChapters(List<BookChapter> data) {
        chapters.clear();
        progressByChapterId.clear();
        if (data != null) {
            chapters.addAll(data);
        }
        for (BookChapter chapter : chapters) {
            progressByChapterId.put(chapter.getId(), UserProgress.empty(chapter.getBookId(), chapter.getId()));
            loadProgress(chapter);
        }
        updateSummary();
        renderChapters();
        showMessage(chapters.isEmpty() ? "No chapters are available yet." : null);
        updatePlayButtonState();
    }

    private void refreshProgress() {
        for (BookChapter chapter : chapters) {
            loadProgress(chapter);
        }
    }

    private void loadProgress(BookChapter chapter) {
        progressRepository.getProgress(chapter.getBookId(), chapter.getId(), new RepositoryCallback<UserProgress>() {
            @Override
            public void onSuccess(UserProgress progress) {
                progressByChapterId.put(chapter.getId(), progress);
                updateSummary();
                renderChapters();
            }

            @Override
            public void onError(Exception exception) {
            }
        });
    }

    private void updateSummary() {
        if (summaryView == null) {
            return;
        }
        long totalDurationMs = 0;
        int completed = 0;
        for (BookChapter chapter : chapters) {
            totalDurationMs += Math.max(chapter.getDurationSec(), 0) * 1000L;
            UserProgress progress = progressByChapterId.get(chapter.getId());
            if (progress != null && progress.isCompleted()) {
                completed++;
            }
        }
        if (chapters.isEmpty()) {
            summaryView.setText("No chapters");
            return;
        }
        summaryView.setText(String.format(
                Locale.US,
                "Audio - %s - %d/%d finished",
                formatDuration(totalDurationMs),
                completed,
                chapters.size()
        ));
    }

    private void toggleSavedBook() {
        if (currentBook == null || saveBookLoading) {
            return;
        }

        String bookId = currentBook.getId();
        boolean targetSaved = !currentBookSaved;
        saveBookLoading = true;
        updateSaveBookButton();

        RepositoryCallback<Void> callback = new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                currentBookSaved = targetSaved;
                saveBookLoading = false;
                updateSaveBookButton();
                Toast.makeText(
                        BookDetailActivity.this,
                        targetSaved ? "Saved to library." : "Removed from library.",
                        Toast.LENGTH_SHORT
                ).show();
            }

            @Override
            public void onError(Exception exception) {
                saveBookLoading = false;
                updateSaveBookButton();
                String message = exception == null || exception.getMessage() == null
                        ? "Could not update your library."
                        : exception.getMessage();
                Toast.makeText(BookDetailActivity.this, message, Toast.LENGTH_LONG).show();
            }
        };

        if (targetSaved) {
            savedBookRepository.saveBook(bookId, callback);
        } else {
            savedBookRepository.unsaveBook(bookId, callback);
        }
    }

    private void updateSaveBookButton() {
        if (saveBookButton == null) {
            return;
        }
        boolean enabled = currentBook != null && !saveBookLoading;
        saveBookButton.setEnabled(enabled);
        saveBookButton.setAlpha(saveBookLoading ? 0.65f : (currentBookSaved ? 1f : 0.45f));
        saveBookButton.setColorFilter(ContextCompat.getColor(
                this,
                currentBookSaved ? R.color.accent_green : R.color.accent
        ));
        saveBookButton.setContentDescription(currentBookSaved ? "Remove audiobook from library" : "Save audiobook");
    }

    private void updatePlayButtonState() {
        if (playResumeButton == null) {
            return;
        }
        boolean enabled = !chapters.isEmpty();
        playResumeButton.setEnabled(enabled);
        playResumeButton.setAlpha(enabled ? 1f : 0.45f);
    }

    private void renderChapters() {
        if (chaptersContainer == null) {
            return;
        }
        chaptersContainer.removeAllViews();
        for (int i = 0; i < chapters.size(); i++) {
            chaptersContainer.addView(createChapterRow(chapters.get(i), i + 1));
        }
    }

    private View createChapterRow(BookChapter chapter, int number) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setBackgroundResource(R.drawable.bg_card_white);
        row.setClickable(true);
        row.setFocusable(true);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        row.setOnClickListener(v -> openReader(chapter, false));

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        rowParams.setMargins(0, 0, 0, dp(12));
        row.setLayoutParams(rowParams);

        LinearLayout contentRow = new LinearLayout(this);
        contentRow.setGravity(Gravity.CENTER_VERTICAL);
        contentRow.setOrientation(LinearLayout.HORIZONTAL);

        TextView chapterNumber = new TextView(this);
        chapterNumber.setText(String.valueOf(number));
        chapterNumber.setTextColor(getColor(R.color.text_muted));
        chapterNumber.setTextSize(14);
        chapterNumber.setGravity(Gravity.CENTER);
        contentRow.addView(chapterNumber, new LinearLayout.LayoutParams(dp(28), ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout textContainer = new LinearLayout(this);
        textContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textParams.setMargins(dp(10), 0, dp(8), 0);

        TextView title = new TextView(this);
        title.setText(chapter.getTitle());
        title.setTextColor(getColor(R.color.text_dark));
        title.setTextSize(16);
        title.setTypeface(null, Typeface.BOLD);
        title.setMaxLines(2);

        TextView subtitle = new TextView(this);
        subtitle.setText(chapterSubtitle(chapter));
        subtitle.setTextColor(getColor(R.color.text_muted));
        subtitle.setTextSize(13);
        subtitle.setPadding(0, dp(4), 0, 0);

        textContainer.addView(title);
        textContainer.addView(subtitle);
        contentRow.addView(textContainer, textParams);

        ImageView download = new ImageView(this);
        boolean downloaded = downloadedAudioRepository.isDownloaded(chapter.getBookId(), chapter.getId());
        boolean downloading = chapter.getId().equals(downloadingChapterId);
        download.setImageResource(downloaded ? R.drawable.ic_download_done : R.drawable.ic_download);
        download.setColorFilter(ContextCompat.getColor(this, R.color.accent));
        download.setPadding(dp(10), dp(10), dp(10), dp(10));
        download.setContentDescription(downloaded ? "Chapter downloaded" : "Download chapter");
        download.setEnabled(!downloaded && !downloading && chapter.hasAudio());
        download.setAlpha(download.isEnabled() || downloaded ? 1f : 0.35f);
        download.setOnClickListener(v -> downloadChapter(chapter));
        contentRow.addView(download, new LinearLayout.LayoutParams(dp(44), dp(44)));

        row.addView(contentRow);
        ProgressBar progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgress(chapterPercent(chapter));
        progressBar.setProgressDrawable(ContextCompat.getDrawable(this, R.drawable.layer_progress));
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(4)
        );
        progressParams.setMargins(dp(38), dp(10), 0, 0);
        row.addView(progressBar, progressParams);
        return row;
    }

    private String chapterSubtitle(BookChapter chapter) {
        UserProgress progress = progressByChapterId.get(chapter.getId());
        String duration = formatDuration(chapterDurationMs(chapter, progress));
        int percent = chapterPercent(chapter);
        boolean downloaded = downloadedAudioRepository.isDownloaded(chapter.getBookId(), chapter.getId());
        String downloadLabel = downloaded ? "Downloaded" : "Streaming";
        return percent + "% complete - " + duration + " - " + downloadLabel;
    }

    private int chapterPercent(BookChapter chapter) {
        UserProgress progress = progressByChapterId.get(chapter.getId());
        if (progress == null) {
            return 0;
        }
        if (progress.isCompleted()) {
            return 100;
        }
        long durationMs = chapterDurationMs(chapter, progress);
        if (durationMs <= 0) {
            return 0;
        }
        return Math.min(100, Math.round(Math.max(progress.getPositionMs(), 0) * 100f / durationMs));
    }

    private long chapterDurationMs(BookChapter chapter, UserProgress progress) {
        if (progress != null && progress.getDurationMs() > 0) {
            return progress.getDurationMs();
        }
        return Math.max(chapter.getDurationSec(), 0) * 1000L;
    }

    private void downloadChapter(BookChapter chapter) {
        if (currentBook == null || chapter == null || downloadingChapterId != null) {
            return;
        }
        if (downloadedAudioRepository.isDownloaded(chapter.getBookId(), chapter.getId())) {
            Toast.makeText(this, "Chapter is already downloaded.", Toast.LENGTH_SHORT).show();
            renderChapters();
            return;
        }
        if (!chapter.hasAudio()) {
            Toast.makeText(this, "This chapter does not have an audioUrl to download.", Toast.LENGTH_LONG).show();
            renderChapters();
            return;
        }

        downloadingChapterId = chapter.getId();
        renderChapters();
        Toast.makeText(this, "Downloading chapter...", Toast.LENGTH_SHORT).show();
        downloadedAudioRepository.download(currentBook, chapter, new RepositoryCallback<File>() {
            @Override
            public void onSuccess(File data) {
                runOnUiThread(() -> {
                    downloadingChapterId = null;
                    renderChapters();
                    Toast.makeText(BookDetailActivity.this, "Chapter downloaded.", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(Exception exception) {
                runOnUiThread(() -> {
                    downloadingChapterId = null;
                    renderChapters();
                    String message = exception == null || exception.getMessage() == null
                            ? "Could not download chapter."
                            : exception.getMessage();
                    Toast.makeText(BookDetailActivity.this, message, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private BookChapter findResumeChapter() {
        for (BookChapter chapter : chapters) {
            UserProgress progress = progressByChapterId.get(chapter.getId());
            if (progress != null && progress.getPositionMs() > 0 && !progress.isCompleted()) {
                return chapter;
            }
        }
        for (BookChapter chapter : chapters) {
            UserProgress progress = progressByChapterId.get(chapter.getId());
            if (progress == null || !progress.isCompleted()) {
                return chapter;
            }
        }
        return chapters.isEmpty() ? null : chapters.get(0);
    }

    private void openReader(BookChapter chapter, boolean autoPlay) {
        if (currentBook == null || chapter == null) {
            return;
        }
        Intent intent = new Intent(this, ActivityReader.class);
        intent.putExtra(ActivityReader.EXTRA_BOOK_ID, currentBook.getId());
        intent.putExtra(ActivityReader.EXTRA_CHAPTER_ID, chapter.getId());
        intent.putExtra(ActivityReader.EXTRA_AUTO_PLAY, autoPlay);
        startActivity(intent);
    }

    private void showMessage(String message) {
        if (messageView == null) {
            return;
        }
        if (message == null || message.trim().isEmpty()) {
            messageView.setVisibility(View.GONE);
            messageView.setText("");
        } else {
            messageView.setVisibility(View.VISIBLE);
            messageView.setText(message);
        }
    }

    private String formatDuration(long millis) {
        long totalMinutes = Math.max(millis, 0) / 60000L;
        long hours = totalMinutes / 60L;
        long minutes = totalMinutes % 60L;
        if (hours > 0) {
            return String.format(Locale.US, "%dh %02dm", hours, minutes);
        }
        return String.format(Locale.US, "%dm", minutes);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
