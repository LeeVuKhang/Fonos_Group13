package com.example.fonos_group13;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ActivityReader extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reader);

        View topBar = findViewById(R.id.topBar);
        if (topBar != null) {
            int initialPaddingTop = topBar.getPaddingTop();
            ViewCompat.setOnApplyWindowInsetsListener(topBar, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), initialPaddingTop + systemBars.top, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }

        View playerControls = findViewById(R.id.playerControlsContainer);
        if (playerControls != null) {
            int initialPaddingBottom = playerControls.getPaddingBottom();
            ViewCompat.setOnApplyWindowInsetsListener(playerControls, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), initialPaddingBottom + systemBars.bottom);
                return insets;
            });
        }

        findViewById(R.id.ivExit).setOnClickListener(v -> finish());
    }
}