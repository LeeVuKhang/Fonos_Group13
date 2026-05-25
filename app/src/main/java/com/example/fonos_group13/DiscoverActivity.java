package com.example.fonos_group13;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class DiscoverActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_discover);

        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }
        
        setupBottomNavigation();
        setupBookClicks();
    }

    private void setupBookClicks() {
        int[] bookIds = {
            R.id.book_pride_prejudice,
            R.id.book_dragon_keep,
            R.id.book_design_system,
            R.id.featured_silent_echo,
            R.id.featured_midnight_garden
        };

        for (int id : bookIds) {
            View book = findViewById(id);
            if (book != null) {
                book.setOnClickListener(v -> {
                    startActivity(new Intent(this, ActivityReader.class));
                });
            }
        }
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