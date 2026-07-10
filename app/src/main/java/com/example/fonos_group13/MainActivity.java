package com.example.fonos_group13;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fonos_group13.data.repository.AuthRepository;

public class MainActivity extends AppCompatActivity {
    private static final long HANDOFF_DELAY_MS = 32L;
    private boolean handoffStarted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View root = findViewById(R.id.main);
        if (root == null) {
            openInitialActivity();
        } else {
            root.postDelayed(this::openInitialActivity, HANDOFF_DELAY_MS);
        }
    }

    private void openInitialActivity() {
        if (handoffStarted || isFinishing() || isDestroyed()) {
            return;
        }
        handoffStarted = true;
        AuthRepository authRepository = FonosApplication.container(this).authRepository();
        Intent intent = new Intent(
                this,
                authRepository.getCurrentUser() == null ? LoginActivity.class : DiscoverActivity.class
        ).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
        overridePendingTransition(0, 0);
    }
}
