package com.example.fonos_group13;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.fonos_group13.data.AuthRepository;
import com.example.fonos_group13.data.BookRepository;
import com.example.fonos_group13.data.ProgressRepository;
import com.example.fonos_group13.data.RepositoryCallback;
import com.example.fonos_group13.model.Book;
import com.example.fonos_group13.model.UserProgress;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class ProfileActivity extends AppCompatActivity {
    private AuthRepository authRepository;
    private BookRepository bookRepository;
    private ProgressRepository progressRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);
        authRepository = new AuthRepository(this);
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
        
        bindProfile();
        loadStats();
        setupBottomNavigation();
    }

    private void bindProfile() {
        FirebaseUser user = authRepository.getCurrentUser();
        TextView name = findViewById(R.id.tv_profile_name);
        TextView email = findViewById(R.id.tv_profile_email);

        if (user != null) {
            name.setText(user.getDisplayName() == null || user.getDisplayName().isEmpty() ? "Reader" : user.getDisplayName());
            email.setText(user.getEmail());
        } else {
            name.setText("Reader");
            email.setText("");
        }

        View logout = findViewById(R.id.btn_logout);
        if (logout != null) {
            logout.setOnClickListener(v -> {
                authRepository.signOut();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            });
        }
    }

    private void loadStats() {
        updateStats(0, 0);
        bookRepository.getPublishedBooks(new RepositoryCallback<List<Book>>() {
            @Override
            public void onSuccess(List<Book> books) {
                if (books == null || books.isEmpty()) {
                    updateStats(0, 0);
                    return;
                }
                loadProgressStats(books);
            }

            @Override
            public void onError(Exception exception) {
                updateStats(0, 0);
            }
        });
    }

    private void loadProgressStats(List<Book> books) {
        final int[] remaining = {books.size()};
        final int[] completedCount = {0};
        final long[] listenedMs = {0};

        for (Book book : books) {
            progressRepository.getProgress(book.getId(), new RepositoryCallback<UserProgress>() {
                @Override
                public void onSuccess(UserProgress progress) {
                    if (progress != null) {
                        if (progress.isCompleted()) {
                            completedCount[0]++;
                        }
                        listenedMs[0] += Math.max(progress.getPositionMs(), 0);
                    }
                    finishOneStatLoad(remaining, completedCount, listenedMs);
                }

                @Override
                public void onError(Exception exception) {
                    finishOneStatLoad(remaining, completedCount, listenedMs);
                }
            });
        }
    }

    private void finishOneStatLoad(int[] remaining, int[] completedCount, long[] listenedMs) {
        remaining[0]--;
        if (remaining[0] <= 0) {
            updateStats(completedCount[0], listenedMs[0]);
        }
    }

    private void updateStats(int completedBooks, long listenedMs) {
        TextView booksRead = findViewById(R.id.tv_books_read);
        TextView hoursListened = findViewById(R.id.tv_hours_listened);
        if (booksRead != null) {
            booksRead.setText(String.valueOf(Math.max(completedBooks, 0)));
        }
        if (hoursListened != null) {
            long listenedHours = Math.round(Math.max(listenedMs, 0) / 3600000f);
            hoursListened.setText(listenedHours + "h");
        }
    }

    private void setupBottomNavigation() {
        View navDiscover = findViewById(R.id.nav_discover);
        View navSearch = findViewById(R.id.nav_search);
        View navLibrary = findViewById(R.id.nav_library);

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
        if (navLibrary != null) {
            navLibrary.setOnClickListener(v -> {
                startActivity(new Intent(this, LibraryActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
                overridePendingTransition(0, 0);
                finish();
            });
        }
    }
}
