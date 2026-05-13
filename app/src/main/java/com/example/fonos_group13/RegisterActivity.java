package com.example.fonos_group13;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.button.MaterialButton;

public class RegisterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        // 1. Edge-to-Edge System Bar Padding
        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        TextView tvSignIn = findViewById(R.id.tv_sign_in); // Make sure this ID matches your XML!
        tvSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });

        // 3. Hardware Hooks
        EditText inputUsername = findViewById(R.id.et_username);
        EditText inputPassword = findViewById(R.id.et_password);
        EditText inputConfirmPassword = findViewById(R.id.et_confirm_password);
        MaterialButton btnRegister = findViewById(R.id.btn_register);

        // 4. Validation Engine
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Extract text and remove accidental spaces using .trim()
                String username = inputUsername.getText().toString().trim();
                String password = inputPassword.getText().toString().trim();
                String confirmPassword = inputConfirmPassword.getText().toString().trim();

                // Rule 1: No empty fields allowed
                if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                    Toast.makeText(RegisterActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                    return; // Stop execution here
                }

                // Rule 2: Passwords must match
                if (!password.equals(confirmPassword)) {
                    // This creates a cool red error tooltip directly on the input field!
                    inputConfirmPassword.setError("Passwords do not match");
                    inputConfirmPassword.requestFocus();
                    return; // Stop execution here
                }

                // If it passes all rules: Success!
                Toast.makeText(RegisterActivity.this, "Account Created Successfully!", Toast.LENGTH_SHORT).show();

                // Usually, you would send data to a database here, then send them to the Discover screen
                Intent intent = new Intent(RegisterActivity.this, DiscoverActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}