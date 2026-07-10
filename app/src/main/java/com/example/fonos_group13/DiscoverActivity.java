package com.example.fonos_group13;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.fonos_group13.controller.catalog.CatalogController;
import com.example.fonos_group13.model.Book;
import com.example.fonos_group13.ui.BookCoverLoader;

import java.util.ArrayList;
import java.util.List;

public class DiscoverActivity extends AppCompatActivity implements CatalogController.View {
    private CatalogController catalogController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_discover);
        catalogController = new CatalogController(FonosApplication.container(this).catalogRepository(), this);

        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }
        
        setupBottomNavigation();
        bindBooks(new ArrayList<>());
        showMessage("Loading audiobooks...");
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

    @Override
    public void showCatalogLoading() {
        showMessage("Loading audiobooks...");
    }

    @Override
    public void showCatalogBooks(List<Book> books) {
        bindBooks(books);
    }

    @Override
    public void showCatalogError(Exception exception) {
        bindBooks(new ArrayList<>());
        showMessage("Could not load Firestore books.");
        Toast.makeText(this, "Could not load Firestore books.", Toast.LENGTH_SHORT).show();
    }

    private void bindBooks(List<Book> books) {
        boolean hasBooks = books != null && !books.isEmpty();
        showMessage(hasBooks ? null : "No audiobooks are published yet.");

        List<Book> featuredBooks = filterBooks(books, true);
        if (featuredBooks.isEmpty()) {
            featuredBooks = books;
        }
        bindBookCard(findViewById(R.id.featured_silent_echo), getBookAt(featuredBooks, 0), R.id.featured_cover_1);
        bindBookCard(findViewById(R.id.featured_midnight_garden), getBookAt(featuredBooks, 1), R.id.featured_cover_2);

        List<Book> regularBooks = filterBooks(books, false);
        if (regularBooks.isEmpty()) {
            regularBooks = books;
        }

        int[] regularCardIds = {
                R.id.book_pride_prejudice,
                R.id.book_dragon_keep,
                R.id.book_design_system,
                R.id.book_scandal_bohemia,
                R.id.book_time_machine
        };
        int[] regularCoverIds = {
                R.id.book_cover_1,
                R.id.book_cover_2,
                R.id.book_cover_3,
                R.id.book_cover_4,
                R.id.book_cover_5
        };
        for (int i = 0; i < regularCardIds.length; i++) {
            bindBookCard(findViewById(regularCardIds[i]), getBookAt(regularBooks, i), regularCoverIds[i]);
        }
    }

    private List<Book> filterBooks(List<Book> books, boolean featured) {
        List<Book> filtered = new ArrayList<>();
        if (books == null) {
            return filtered;
        }
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

    private void bindBookCard(View card, Book book, int coverId) {
        if (card == null) {
            return;
        }
        if (book == null) {
            card.setVisibility(View.GONE);
            card.setOnClickListener(null);
            return;
        }
        card.setVisibility(View.VISIBLE);
        ImageView cover = BookCoverLoader.findCoverView(card, coverId);
        BookCoverLoader.load(cover, book);

        List<TextView> textViews = new ArrayList<>();
        collectTextViews(card, textViews);
        if (textViews.size() >= 2) {
            textViews.get(textViews.size() - 2).setText(book.getTitle());
            textViews.get(textViews.size() - 1).setText(book.getAuthor());
        }
        card.setOnClickListener(v -> openReader(book));
    }

    private void showMessage(String message) {
        TextView messageView = findViewById(R.id.discover_empty_state);
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
        Intent intent = new Intent(this, BookDetailActivity.class);
        intent.putExtra(BookDetailActivity.EXTRA_BOOK_ID, book.getId());
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
