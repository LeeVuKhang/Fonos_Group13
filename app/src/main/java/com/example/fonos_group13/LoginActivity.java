package com.example.fonos_group13;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
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

public class LoginActivity extends AppCompatActivity {
    private static final String DEMO_EMAIL = "admin@gmail.com";
    private static final String DEMO_PASSWORD = "123456";

    private AuthRepository authRepository;
    private EditText inputEmail;
    private EditText inputPassword;
    private Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        authRepository = new AuthRepository(this);
        if (authRepository.getCurrentUser() != null) {
            openDiscover();
            return;
        }

        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        TextView tvRegister = findViewById(R.id.tv_register);
        tvRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });

        inputEmail = findViewById(R.id.inputUsername);
        inputPassword = findViewById(R.id.inputPassword);
        prefillDemoCredentials();
        btnLogin = findViewById(R.id.btn_login);
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });
    }

    private void prefillDemoCredentials() {
        if ((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) == 0) {
            return;
        }
        inputEmail.setText(DEMO_EMAIL);
        inputPassword.setText(DEMO_PASSWORD);
    }

    private void signIn() {
        String email = inputEmail.getText().toString().trim();
        String password = inputPassword.getText().toString();

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            inputEmail.setError("Please enter a valid email");
            inputEmail.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            inputPassword.setError("Please enter your password");
            inputPassword.requestFocus();
            return;
        }

        setLoading(true);
        authRepository.signIn(email, password, new RepositoryCallback<FirebaseUser>() {
            @Override
            public void onSuccess(FirebaseUser data) {
                setLoading(false);
                openDiscover();
            }

            @Override
            public void onError(Exception exception) {
                setLoading(false);
                Toast.makeText(LoginActivity.this, AuthRepository.friendlyError(exception), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        btnLogin.setEnabled(!loading);
        btnLogin.setText(loading ? "Signing in..." : getString(R.string.sign_in));
    }

    private void openDiscover() {
        Intent intent = new Intent(LoginActivity.this, DiscoverActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
        overridePendingTransition(0, 0);
    }
}
