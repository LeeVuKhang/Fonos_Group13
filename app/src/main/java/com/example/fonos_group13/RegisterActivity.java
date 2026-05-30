package com.example.fonos_group13;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.fonos_group13.data.AuthRepository;
import com.example.fonos_group13.data.RepositoryCallback;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.material.button.MaterialButton;

public class RegisterActivity extends AppCompatActivity {
    private AuthRepository authRepository;
    private EditText inputEmail;
    private EditText inputPassword;
    private EditText inputConfirmPassword;
    private MaterialButton btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        authRepository = new AuthRepository(this);

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
        inputEmail = findViewById(R.id.et_username);
        inputPassword = findViewById(R.id.et_password);
        inputConfirmPassword = findViewById(R.id.et_confirm_password);
        btnRegister = findViewById(R.id.btn_register);

        // 4. Validation Engine
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                register();
            }
        });
    }

    private void register() {
        String email = inputEmail.getText().toString().trim();
        String password = inputPassword.getText().toString();
        String confirmPassword = inputConfirmPassword.getText().toString();

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            inputEmail.setError("Please enter a valid email");
            inputEmail.requestFocus();
            return;
        }
        if (password.length() < 6) {
            inputPassword.setError("Password must be at least 6 characters");
            inputPassword.requestFocus();
            return;
        }
        if (!password.equals(confirmPassword)) {
            inputConfirmPassword.setError("Passwords do not match");
            inputConfirmPassword.requestFocus();
            return;
        }

        setLoading(true);
        authRepository.register(email, password, new RepositoryCallback<FirebaseUser>() {
            @Override
            public void onSuccess(FirebaseUser data) {
                setLoading(false);
                Toast.makeText(RegisterActivity.this, "Account created successfully", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(RegisterActivity.this, DiscoverActivity.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(Exception exception) {
                setLoading(false);
                Toast.makeText(RegisterActivity.this, AuthRepository.friendlyError(exception), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        btnRegister.setEnabled(!loading);
        btnRegister.setText(loading ? "Creating account..." : getString(R.string.create_account));
    }
}
