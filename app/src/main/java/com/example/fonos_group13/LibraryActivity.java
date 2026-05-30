package com.example.fonos_group13;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.fonos_group13.data.BookRepository;
import com.example.fonos_group13.data.ProgressRepository;
import com.example.fonos_group13.data.RepositoryCallback;
import com.example.fonos_group13.model.Book;
import com.example.fonos_group13.model.UserProgress;

import java.util.ArrayList;
import java.util.List;

public class LibraryActivity extends AppCompatActivity {
    private BookRepository bookRepository;
    private ProgressRepository progressRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_library);
        bookRepository = new BookRepository(this);
        progressRepository = new ProgressRepository(this);

        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }
        
        setupBottomNavigation();
        bindBooks(Book.fallbackBooks());
        loadBooks();
    }

    private void loadBooks() {
        bookRepository.getPublishedBooks(new RepositoryCallback<List<Book>>() {
            @Override
            public void onSuccess(List<Book> books) {
                bindBooks(books);
            }

            @Override
            public void onError(Exception exception) {
                Toast.makeText(LibraryActivity.this, "Could not load Firestore library. Showing local demo data.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindBooks(List<Book> books) {
        int[] rowIds = {
                R.id.library_book_1,
                R.id.library_book_2,
                R.id.library_book_3
        };

        for (int i = 0; i < rowIds.length; i++) {
            View row = findViewById(rowIds[i]);
            Book book = i < books.size() ? books.get(i) : null;
            bindLibraryRow(row, book);
        }
    }

    private void bindLibraryRow(View row, Book book) {
        if (row == null) {
            return;
        }
        if (book == null) {
            row.setVisibility(View.GONE);
            return;
        }
        row.setVisibility(View.VISIBLE);
        row.setOnClickListener(v -> openReader(book));

        List<TextView> textViews = new ArrayList<>();
        collectTextViews(row, textViews);
        if (textViews.size() >= 2) {
            textViews.get(0).setText(book.getTitle());
            textViews.get(1).setText(book.getAuthor());
        }
        bindProgress(row, book, UserProgress.empty(book.getId()));

        progressRepository.getProgress(book.getId(), new RepositoryCallback<UserProgress>() {
            @Override
            public void onSuccess(UserProgress progress) {
                bindProgress(row, book, progress);
            }

            @Override
            public void onError(Exception exception) {
            }
        });
    }

    private void bindProgress(View row, Book book, UserProgress progress) {
        long durationMs = progress.getDurationMs() > 0 ? progress.getDurationMs() : book.getDurationSec() * 1000L;
        long positionMs = Math.max(progress.getPositionMs(), 0);
        int percent = durationMs <= 0 ? 0 : Math.min(100, Math.round(positionMs * 100f / durationMs));

        List<TextView> textViews = new ArrayList<>();
        collectTextViews(row, textViews);
        if (textViews.size() >= 4) {
            textViews.get(2).setText(percent + "% COMPLETED");
            textViews.get(3).setText(formatRemaining(durationMs - positionMs) + " LEFT");
        }

        ProgressBar progressBar = findProgressBar(row);
        if (progressBar != null) {
            progressBar.setProgress(percent);
        }
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

    private void openReader(Book book) {
        Intent intent = new Intent(this, ActivityReader.class);
        intent.putExtra(ActivityReader.EXTRA_BOOK_ID, book.getId());
        startActivity(intent);
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
