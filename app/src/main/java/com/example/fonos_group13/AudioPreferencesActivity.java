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
import com.example.fonos_group13.data.BookRepository;
import com.example.fonos_group13.data.DownloadedAudioRepository;
import com.example.fonos_group13.data.RepositoryCallback;
import com.example.fonos_group13.model.Book;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AudioPreferencesActivity extends AppCompatActivity {
    private BookRepository bookRepository;
    private DownloadedAudioRepository downloadedAudioRepository;
    private LinearLayout downloadsContainer;
    private TextView downloadsEmptyState;
    private TextView[] speedChips;
    private final List<Book> publishedBooks = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_audio_preferences);

        bookRepository = new BookRepository(this);
        downloadedAudioRepository = new DownloadedAudioRepository(this);

        bindViews();
        setupInsets();
        setupSpeedOptions();
        loadDownloads();
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

    private void loadDownloads() {
        if (downloadsContainer != null) {
            downloadsContainer.removeAllViews();
        }
        showDownloadsMessage("Loading downloaded audio...");
        bookRepository.getPublishedBooks(new RepositoryCallback<List<Book>>() {
            @Override
            public void onSuccess(List<Book> books) {
                publishedBooks.clear();
                if (books != null) {
                    publishedBooks.addAll(books);
                }
                bindDownloadedBooks();
            }

            @Override
            public void onError(Exception exception) {
                publishedBooks.clear();
                if (downloadsContainer != null) {
                    downloadsContainer.removeAllViews();
                }
                showDownloadsMessage("Could not load downloaded audio.");
                Toast.makeText(AudioPreferencesActivity.this, "Could not load downloaded audio.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindDownloadedBooks() {
        if (downloadsContainer == null) {
            return;
        }

        downloadsContainer.removeAllViews();
        int visibleCount = 0;
        for (Book book : publishedBooks) {
            if (downloadedAudioRepository.isDownloaded(book.getId())) {
                addDownloadRow(book);
                visibleCount++;
            }
        }

        if (visibleCount == 0) {
            showDownloadsMessage("No downloaded audio yet.");
        } else {
            showDownloadsMessage(null);
        }
    }

    private void addDownloadRow(Book book) {
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
        title.setText(book.getTitle());
        title.setTextColor(getColor(R.color.text_dark));
        title.setTextSize(16);
        title.setTypeface(null, Typeface.BOLD);

        TextView subtitle = new TextView(this);
        long sizeBytes = downloadedAudioRepository.getDownloadedSizeBytes(book.getId());
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
        deleteButton.setOnClickListener(v -> confirmDelete(book));

        row.addView(textContainer);
        row.addView(deleteButton);
        downloadsContainer.addView(row);
    }

    private void confirmDelete(Book book) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Download")
                .setMessage("Remove downloaded audio for \"" + book.getTitle() + "\"? Streaming will still work if audioUrl is available.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> deleteDownload(book))
                .show();
    }

    private void deleteDownload(Book book) {
        if (downloadedAudioRepository.deleteDownloadedAudio(book.getId())) {
            Toast.makeText(this, "Downloaded audio deleted.", Toast.LENGTH_SHORT).show();
            bindDownloadedBooks();
        } else {
            Toast.makeText(this, "Could not delete downloaded audio.", Toast.LENGTH_LONG).show();
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
