package com.example.fonos_group13;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fonos_group13.data.AuthRepository;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AuthRepository authRepository = new AuthRepository(this);
        Intent intent = new Intent(
                this,
                authRepository.getCurrentUser() == null ? LoginActivity.class : DiscoverActivity.class
        );
        startActivity(intent);
        finish();
        overridePendingTransition(0, 0);
    }
}
