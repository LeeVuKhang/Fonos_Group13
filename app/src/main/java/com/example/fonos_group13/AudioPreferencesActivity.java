package com.example.fonos_group13;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.fonos_group13.audio.AudioPreferences;
import com.example.fonos_group13.controller.catalog.CatalogSnapshotController;
import com.example.fonos_group13.data.repository.AudioDownloadRepository;
import com.example.fonos_group13.model.Book;
import com.example.fonos_group13.model.BookChapter;
import com.example.fonos_group13.model.CatalogSnapshot;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AudioPreferencesActivity extends AppCompatActivity implements CatalogSnapshotController.View {
    private AudioDownloadRepository downloadedAudioRepository;
    private CatalogSnapshotController catalogController;
    private LinearLayout downloadsContainer;
    private TextView downloadsEmptyState;
    private TextView[] speedChips;
    private final List<Book> publishedBooks = new ArrayList<>();
    private final Map<String, List<BookChapter>> chaptersByBookId = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_audio_preferences);

        AppContainer container = FonosApplication.container(this);
        downloadedAudioRepository = container.audioDownloadRepository();
        catalogController = new CatalogSnapshotController(container.catalogRepository(), this);

        bindViews();
        setupInsets();
        setupSpeedOptions();
    }

    @Override
    protected void onStart() {
        super.onStart();
        catalogController.start();
    }

    @Override
    protected void onStop() {
        catalogController.stop();
        super.onStop();
    }

    private void bindViews() {
        downloadsContainer = findViewById(R.id.downloads_container);
        downloadsEmptyState = findViewById(R.id.downloads_empty_state);
        speedChips = new TextView[] {
                findViewById(R.id.speed_1_0),
                findViewById(R.id.speed_1_2),
                findViewById(R.id.speed_1_5),
                findViewById(R.id.speed_2_0)
        };

        View back = findViewById(R.id.btn_back);
        if (back != null) {
            back.setOnClickListener(v -> finish());
        }
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

    private void setupSpeedOptions() {
        for (int i = 0; i < speedChips.length; i++) {
            TextView chip = speedChips[i];
            if (chip == null) {
                continue;
            }
            int speedIndex = i;
            chip.setText(AudioPreferences.formatSpeed(speedIndex));
            chip.setOnClickListener(v -> selectDefaultSpeed(speedIndex, true));
        }
        selectDefaultSpeed(AudioPreferences.getDefaultSpeedIndex(this), false);
    }

    private void selectDefaultSpeed(int speedIndex, boolean save) {
        int selectedIndex = AudioPreferences.clampSpeedIndex(speedIndex);
        if (save) {
            AudioPreferences.setDefaultSpeedIndex(this, selectedIndex);
            Toast.makeText(this, "Default speed set to " + AudioPreferences.formatSpeed(selectedIndex) + ".", Toast.LENGTH_SHORT).show();
        }

        for (int i = 0; i < speedChips.length; i++) {
            TextView chip = speedChips[i];
            if (chip == null) {
                continue;
            }
            boolean active = i == selectedIndex;
            chip.setBackgroundResource(active ? R.drawable.bg_chip_active : R.drawable.bg_chip_white);
            chip.setTextColor(getColor(active ? R.color.white : R.color.text_muted));
            chip.setTypeface(null, active ? Typeface.BOLD : Typeface.NORMAL);
        }
    }

    @Override
    public void showCatalogSnapshotLoading() {
        if (downloadsContainer != null) {
            downloadsContainer.removeAllViews();
        }
        publishedBooks.clear();
        chaptersByBookId.clear();
        showDownloadsMessage("Loading downloaded audio...");
    }

    @Override
    public void showCatalogSnapshot(CatalogSnapshot snapshot) {
        publishedBooks.clear();
        chaptersByBookId.clear();
        publishedBooks.addAll(snapshot.getBooks());
        chaptersByBookId.putAll(snapshot.getChaptersByBookId());
        bindDownloadedChapters();
    }

    @Override
    public void showCatalogSnapshotError(Exception exception) {
        if (downloadsContainer != null) {
            downloadsContainer.removeAllViews();
        }
        showDownloadsMessage("Could not load downloaded audio.");
        Toast.makeText(this, "Could not load downloaded audio.", Toast.LENGTH_SHORT).show();
    }

    private void bindDownloadedChapters() {
        if (downloadsContainer == null) {
            return;
        }

        downloadsContainer.removeAllViews();
        int visibleCount = 0;
        for (Book book : publishedBooks) {
            List<BookChapter> downloadedChapters = downloadedChaptersForBook(book);
            if (!downloadedChapters.isEmpty()) {
                addBookGroup(book, downloadedChapters);
                visibleCount += downloadedChapters.size();
            }
        }

        if (visibleCount == 0) {
            showDownloadsMessage("No downloaded audio yet.");
        } else {
            showDownloadsMessage(null);
        }
    }

    private List<BookChapter> downloadedChaptersForBook(Book book) {
        List<BookChapter> downloadedChapters = new ArrayList<>();
        List<BookChapter> chapters = chaptersByBookId.get(book.getId());
        if (chapters == null || chapters.isEmpty()) {
            chapters = new ArrayList<>();
            chapters.add(BookChapter.fromLegacyBook(book));
        }
        for (BookChapter chapter : chapters) {
            if (downloadedAudioRepository.isDownloaded(book.getId(), chapter.getId())) {
                downloadedChapters.add(chapter);
            }
        }
        return downloadedChapters;
    }

    private void addBookGroup(Book book, List<BookChapter> chapters) {
        TextView groupTitle = new TextView(this);
        groupTitle.setText(book.getTitle());
        groupTitle.setTextColor(getColor(R.color.text_dark));
        groupTitle.setTextSize(16);
        groupTitle.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        titleParams.setMargins(0, dp(12), 0, dp(6));
        downloadsContainer.addView(groupTitle, titleParams);

        for (BookChapter chapter : chapters) {
            addDownloadRow(book, chapter);
        }
    }

    private void addDownloadRow(Book book, BookChapter chapter) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(8), 0, dp(8));

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        rowParams.setMargins(0, 0, 0, dp(8));
        row.setLayoutParams(rowParams);

        LinearLayout textContainer = new LinearLayout(this);
        textContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        );
        textContainer.setLayoutParams(textParams);

        TextView title = new TextView(this);
        title.setText(chapter.getTitle());
        title.setTextColor(getColor(R.color.text_dark));
        title.setTextSize(15);
        title.setTypeface(null, Typeface.BOLD);

        TextView subtitle = new TextView(this);
        long sizeBytes = downloadedAudioRepository.getDownloadedSizeBytes(book.getId(), chapter.getId());
        subtitle.setText(book.getAuthor() + " - " + formatSize(sizeBytes));
        subtitle.setTextColor(getColor(R.color.text_muted));
        subtitle.setTextSize(13);

        textContainer.addView(title);
        textContainer.addView(subtitle);

        MaterialButton deleteButton = new MaterialButton(this);
        deleteButton.setAllCaps(false);
        deleteButton.setText("Delete");
        deleteButton.setTextSize(13);
        deleteButton.setTextColor(getColor(R.color.white));
        deleteButton.setBackgroundTintList(ColorStateList.valueOf(getColor(android.R.color.holo_red_dark)));
        deleteButton.setOnClickListener(v -> confirmDelete(book, chapter));

        row.addView(textContainer);
        row.addView(deleteButton);
        downloadsContainer.addView(row);
    }

    private void confirmDelete(Book book, BookChapter chapter) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Download")
                .setMessage("Remove downloaded audio for \"" + chapter.getTitle() + "\"? Streaming will still work if audioUrl is available.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> deleteDownload(book, chapter))
                .show();
    }

    private void deleteDownload(Book book, BookChapter chapter) {
        if (downloadedAudioRepository.deleteDownloadedAudio(book.getId(), chapter.getId())) {
            Toast.makeText(this, "Downloaded chapter deleted.", Toast.LENGTH_SHORT).show();
            bindDownloadedChapters();
        } else {
            Toast.makeText(this, "Could not delete downloaded chapter.", Toast.LENGTH_LONG).show();
        }
    }

    private void showDownloadsMessage(String message) {
        if (downloadsEmptyState == null) {
            return;
        }
        if (message == null || message.trim().isEmpty()) {
            downloadsEmptyState.setVisibility(View.GONE);
            downloadsEmptyState.setText("");
        } else {
            downloadsEmptyState.setVisibility(View.VISIBLE);
            downloadsEmptyState.setText(message);
        }
    }

    private String formatSize(long bytes) {
        if (bytes <= 0) {
            return "Downloaded";
        }
        double kilobytes = bytes / 1024d;
        if (kilobytes < 1024d) {
            return String.format(Locale.US, "%.0f KB", kilobytes);
        }
        return String.format(Locale.US, "%.1f MB", kilobytes / 1024d);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
