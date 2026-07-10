package com.example.fonos_group13;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.fonos_group13.controller.profile.ProfileController;
import com.example.fonos_group13.controller.profile.ProfileStats;
import com.example.fonos_group13.data.auth.AuthErrorFormatter;
import com.example.fonos_group13.data.core.RepositoryCallback;
import com.example.fonos_group13.data.repository.AuthRepository;
import com.example.fonos_group13.model.UserAccount;

public class ProfileActivity extends AppCompatActivity implements ProfileController.View {
    private AuthRepository authRepository;
    private ProfileController profileController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);
        AppContainer container = FonosApplication.container(this);
        authRepository = container.authRepository();
        profileController = new ProfileController(
                container.catalogRepository(),
                container.progressRepository(),
                this
        );

        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }
        
        bindProfile();
        updateStats(0, 0);
        setupProfileActions();
        setupBottomNavigation();
    }

    private void bindProfile() {
        UserAccount user = authRepository.getCurrentUser();
        TextView name = findViewById(R.id.tv_profile_name);
        TextView email = findViewById(R.id.tv_profile_email);

        if (user != null) {
            name.setText(user.getDisplayName() == null || user.getDisplayName().isEmpty() ? "Reader" : user.getDisplayName());
            email.setText(user.getEmail());
        } else {
            name.setText("Reader");
            email.setText("");
        }
    }

    private void setupProfileActions() {
        View accountSettings = findViewById(R.id.btn_account_settings);
        if (accountSettings != null) {
            accountSettings.setOnClickListener(v -> showDisplayNameDialog());
        }

        View audioPreferences = findViewById(R.id.btn_audio_preferences);
        if (audioPreferences != null) {
            audioPreferences.setOnClickListener(v -> startActivity(new Intent(this, AudioPreferencesActivity.class)));
        }

        View createAudiobook = findViewById(R.id.btn_create_audiobook);
        if (createAudiobook != null) {
            createAudiobook.setOnClickListener(v -> startActivity(new Intent(this, CreateAudiobookActivity.class)));
        }

        View myUploads = findViewById(R.id.btn_my_uploads);
        if (myUploads != null) {
            myUploads.setOnClickListener(v -> startActivity(new Intent(this, MyUploadsActivity.class)));
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

    private void showDisplayNameDialog() {
        UserAccount user = authRepository.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please sign in again to update your profile.", Toast.LENGTH_LONG).show();
            return;
        }

        String currentName = user.getDisplayName();
        if (currentName == null || currentName.trim().isEmpty()) {
            TextView profileName = findViewById(R.id.tv_profile_name);
            currentName = profileName == null ? "" : profileName.getText().toString();
        }

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_edit_display_name);

        EditText input = dialog.findViewById(R.id.dialog_display_name);
        TextView cancel = dialog.findViewById(R.id.dialog_cancel);
        TextView save = dialog.findViewById(R.id.dialog_save);
        input.setText(currentName);

        cancel.setOnClickListener(v -> dialog.dismiss());
        save.setOnClickListener(v -> {
            String displayName = input.getText().toString().trim();
            if (displayName.isEmpty()) {
                input.setError("Display name cannot be empty");
                input.requestFocus();
                return;
            }

            input.setEnabled(false);
            cancel.setEnabled(false);
            save.setEnabled(false);
            save.setAlpha(0.65f);
            save.setText("Saving...");
            authRepository.updateDisplayName(displayName, new RepositoryCallback<UserAccount>() {
                @Override
                public void onSuccess(UserAccount data) {
                    runOnUiThread(() -> {
                        updateProfileName(displayName);
                        Toast.makeText(ProfileActivity.this, "Display name updated.", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    });
                }

                @Override
                public void onError(Exception exception) {
                    runOnUiThread(() -> {
                        input.setEnabled(true);
                        cancel.setEnabled(true);
                        save.setEnabled(true);
                        save.setAlpha(1f);
                        save.setText("Save");
                        Toast.makeText(
                                ProfileActivity.this,
                                AuthErrorFormatter.friendlyMessage(exception),
                                Toast.LENGTH_LONG
                        ).show();
                    });
                }
            });
        });

        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams params = new WindowManager.LayoutParams();
            params.copyFrom(window.getAttributes());
            params.width = Math.min(getResources().getDisplayMetrics().widthPixels - dp(48), dp(360));
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(params);
        }
    }

    private void updateProfileName(String displayName) {
        TextView name = findViewById(R.id.tv_profile_name);
        if (name != null) {
            name.setText(displayName);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void renderProfileStats(ProfileStats stats, boolean partial) {
        updateStats(stats.getCompletedBooks(), stats.getListenedMs());
    }

    @Override
    protected void onStart() {
        super.onStart();
        profileController.start();
    }

    @Override
    protected void onStop() {
        profileController.stop();
        super.onStop();
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
