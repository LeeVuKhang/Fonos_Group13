package com.example.fonos_group13;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.fonos_group13.data.BookRepository;
import com.example.fonos_group13.data.RepositoryCallback;
import com.example.fonos_group13.model.Book;
import com.example.fonos_group13.ui.BookCoverLoader;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity {
    private final List<Book> allBooks = new ArrayList<>();
    private BookRepository bookRepository;
    private EditText searchInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_search);
        bookRepository = new BookRepository(this);

        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }
        
        setupBottomNavigation();
        setupSearch();
        loadBooks();
    }

    private void setupSearch() {
        searchInput = findViewById(R.id.search_input);
        if (searchInput == null) {
            return;
        }
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                bindResults(s == null ? "" : s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void loadBooks() {
        showMessage("Loading audiobooks...");
        bookRepository.getPublishedBooks(new RepositoryCallback<List<Book>>() {
            @Override
            public void onSuccess(List<Book> books) {
                allBooks.clear();
                if (books != null) {
                    allBooks.addAll(books);
                }
                bindResults(searchInput == null ? "" : searchInput.getText().toString());
            }

            @Override
            public void onError(Exception exception) {
                allBooks.clear();
                bindResults("");
                showMessage("Could not load Firestore books.");
                Toast.makeText(SearchActivity.this, "Could not load Firestore books.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindResults(String query) {
        List<Book> results = filterBooks(query);
        int[] rowIds = {
                R.id.search_result_1,
                R.id.search_result_2,
                R.id.search_result_3,
                R.id.search_result_4,
                R.id.search_result_5
        };
        int[] coverIds = {
                R.id.search_cover_1,
                R.id.search_cover_2,
                R.id.search_cover_3,
                R.id.search_cover_4,
                R.id.search_cover_5
        };

        TextView sectionLabel = findViewById(R.id.search_section_label);
        boolean searching = query != null && !query.trim().isEmpty();
        if (sectionLabel != null) {
            sectionLabel.setText(searching ? "SEARCH RESULTS" : "ALL AUDIOBOOKS");
        }

        for (int i = 0; i < rowIds.length; i++) {
            View row = findViewById(rowIds[i]);
            Book book = i < results.size() ? results.get(i) : null;
            bindSearchRow(row, book, coverIds[i]);
        }

        if (results.isEmpty()) {
            showMessage(allBooks.isEmpty()
                    ? "No audiobooks are published yet."
                    : "No books match your search.");
        } else {
            showMessage(null);
        }
    }

    private List<Book> filterBooks(String query) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase();
        if (normalizedQuery.isEmpty()) {
            return new ArrayList<>(allBooks);
        }

        List<Book> results = new ArrayList<>();
        for (Book book : allBooks) {
            String title = book.getTitle() == null ? "" : book.getTitle().toLowerCase();
            String author = book.getAuthor() == null ? "" : book.getAuthor().toLowerCase();
            if (title.contains(normalizedQuery) || author.contains(normalizedQuery)) {
                results.add(book);
            }
        }
        return results;
    }

    private void bindSearchRow(View row, Book book, int coverId) {
        if (row == null) {
            return;
        }
        if (book == null) {
            row.setVisibility(View.GONE);
            row.setOnClickListener(null);
            return;
        }

        row.setVisibility(View.VISIBLE);
        row.setOnClickListener(v -> openReader(book));
        ImageView cover = BookCoverLoader.findCoverView(row, coverId);
        BookCoverLoader.load(cover, book);

        List<TextView> textViews = new ArrayList<>();
        collectTextViews(row, textViews);
        if (textViews.size() >= 2) {
            textViews.get(0).setText(book.getTitle());
            textViews.get(1).setText(book.getAuthor());
        }
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

    private void showMessage(String message) {
        TextView emptyState = findViewById(R.id.search_empty_state);
        if (emptyState == null) {
            return;
        }
        if (message == null || message.trim().isEmpty()) {
            emptyState.setVisibility(View.GONE);
            emptyState.setText("");
        } else {
            emptyState.setVisibility(View.VISIBLE);
            emptyState.setText(message);
        }
    }

    private void openReader(Book book) {
        Intent intent = new Intent(this, BookDetailActivity.class);
        intent.putExtra(BookDetailActivity.EXTRA_BOOK_ID, book.getId());
        startActivity(intent);
    }

    private void setupBottomNavigation() {
        View navDiscover = findViewById(R.id.nav_discover);
        View navLibrary = findViewById(R.id.nav_library);
        View navProfile = findViewById(R.id.nav_profile);

        if (navDiscover != null) {
            navDiscover.setOnClickListener(v -> {
                startActivity(new Intent(this, DiscoverActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
                overridePendingTransition(0, 0);
                finish();
            });
        }
        if (navLibrary != null) {
            navLibrary.setOnClickListener(v -> {
                startActivity(new Intent(this, LibraryActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
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
