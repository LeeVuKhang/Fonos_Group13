package com.example.fonos_group13;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
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

import java.util.ArrayList;
import java.util.List;

public class DiscoverActivity extends AppCompatActivity {
    private BookRepository bookRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_discover);
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
                Toast.makeText(DiscoverActivity.this, "Could not load Firestore books. Showing local demo data.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindBooks(List<Book> books) {
        List<Book> featuredBooks = filterBooks(books, true);
        if (featuredBooks.isEmpty()) {
            featuredBooks = books;
        }
        bindBookCard(findViewById(R.id.featured_silent_echo), getBookAt(featuredBooks, 0));
        bindBookCard(findViewById(R.id.featured_midnight_garden), getBookAt(featuredBooks, 1));

        List<Book> regularBooks = filterBooks(books, false);
        if (regularBooks.isEmpty()) {
            regularBooks = books;
        }

        int[] regularCardIds = {
                R.id.book_pride_prejudice,
                R.id.book_dragon_keep,
                R.id.book_design_system
        };
        for (int i = 0; i < regularCardIds.length; i++) {
            bindBookCard(findViewById(regularCardIds[i]), getBookAt(regularBooks, i));
        }
    }

    private List<Book> filterBooks(List<Book> books, boolean featured) {
        List<Book> filtered = new ArrayList<>();
        for (Book book : books) {
            if (book.isFeatured() == featured) {
                filtered.add(book);
            }
        }
        return filtered;
    }

    private Book getBookAt(List<Book> books, int index) {
        return books == null || index < 0 || index >= books.size() ? null : books.get(index);
    }

    private void bindBookCard(View card, Book book) {
        if (card == null) {
            return;
        }
        if (book == null) {
            card.setVisibility(View.GONE);
            return;
        }
        card.setVisibility(View.VISIBLE);
        List<TextView> textViews = new ArrayList<>();
        collectTextViews(card, textViews);
        if (textViews.size() >= 2) {
            textViews.get(textViews.size() - 2).setText(book.getTitle());
            textViews.get(textViews.size() - 1).setText(book.getAuthor());
        }
        card.setOnClickListener(v -> openReader(book));
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

    private void openReader(Book book) {
        Intent intent = new Intent(this, ActivityReader.class);
        intent.putExtra(ActivityReader.EXTRA_BOOK_ID, book.getId());
        startActivity(intent);
    }

    private void setupBottomNavigation() {
        View navSearch = findViewById(R.id.nav_search);
        View navLibrary = findViewById(R.id.nav_library);
        View navProfile = findViewById(R.id.nav_profile);

        if (navSearch != null) {
            navSearch.setOnClickListener(v -> {
                startActivity(new Intent(this, SearchActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
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
