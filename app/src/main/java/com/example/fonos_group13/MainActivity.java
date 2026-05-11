package com.example.fonos_group13;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Start LoginActivity and finish this one
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}