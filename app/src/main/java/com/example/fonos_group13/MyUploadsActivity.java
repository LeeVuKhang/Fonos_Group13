package com.example.fonos_group13;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.fonos_group13.data.AuthRepository;
import com.example.fonos_group13.data.CreatorAudiobookRepository;
import com.example.fonos_group13.data.RepositoryCallback;
import com.example.fonos_group13.data.UploadNotificationTokenRepository;
import com.example.fonos_group13.model.AudiobookGenerationStatus;
import com.example.fonos_group13.model.UserGeneratedAudiobook;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MyUploadsActivity extends AppCompatActivity {
    private static final int REQUEST_POST_NOTIFICATIONS = 4102;

    private CreatorAudiobookRepository repository;
    private UploadNotificationTokenRepository notificationTokenRepository;
    private LinearLayout uploadsContainer;
    private TextView emptyState;
    private MaterialButton createButton;
    private ListenerRegistration uploadsRegistration;
    private List<UserGeneratedAudiobook> currentUploads = Collections.emptyList();
    private String loadingBookId;
    private boolean notificationRegistrationAttempted;
    private boolean notificationPermissionRequested;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_my_uploads);

        repository = new CreatorAudiobookRepository(this);
        notificationTokenRepository = new UploadNotificationTokenRepository(this);
        bindViews();
        setupInsets();
        setupControls();
    }

    @Override
    protected void onStart() {
        super.onStart();
        startObservingUploads();
    }

    @Override
    protected void onStop() {
        stopObservingUploads();
        super.onStop();
    }

    private void bindViews() {
        uploadsContainer = findViewById(R.id.uploads_container);
        emptyState = findViewById(R.id.uploads_empty_state);
        createButton = findViewById(R.id.btn_create_upload);
    }

    private void setupInsets() {
        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }
    }

    private void setupControls() {
        View back = findViewById(R.id.btn_back);
        if (back != null) {
            back.setOnClickListener(v -> finish());
        }
        if (createButton != null) {
            createButton.setOnClickListener(v -> startActivity(new Intent(this, CreateAudiobookActivity.class)));
        }
    }

    private void startObservingUploads() {
        stopObservingUploads();
        if (currentUploads.isEmpty()) {
            if (uploadsContainer != null) {
                uploadsContainer.removeAllViews();
            }
            showMessage("Loading uploads...");
        }
        uploadsRegistration = repository.observeMyUploads(new RepositoryCallback<List<UserGeneratedAudiobook>>() {
            @Override
            public void onSuccess(List<UserGeneratedAudiobook> uploads) {
                currentUploads = uploads == null ? Collections.emptyList() : uploads;
                renderUploads(uploads);
                if (hasPendingUploads(currentUploads)) {
                    ensureGenerationNotifications();
                }
            }

            @Override
            public void onError(Exception exception) {
                currentUploads = Collections.emptyList();
                if (uploadsContainer != null) {
                    uploadsContainer.removeAllViews();
                }
                showMessage("Could not load your uploads.");
                Toast.makeText(
                        MyUploadsActivity.this,
                        AuthRepository.friendlyError(exception),
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    private void stopObservingUploads() {
        if (uploadsRegistration != null) {
            uploadsRegistration.remove();
            uploadsRegistration = null;
        }
    }

    private void renderUploads(List<UserGeneratedAudiobook> uploads) {
        if (uploadsContainer == null) {
            return;
        }
        uploadsContainer.removeAllViews();
        if (uploads == null || uploads.isEmpty()) {
            showMessage("No uploads yet.");
            return;
        }
        showMessage(null);
        for (UserGeneratedAudiobook upload : uploads) {
            uploadsContainer.addView(createUploadCard(upload));
        }
    }

    private View createUploadCard(UserGeneratedAudiobook upload) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card_white);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dp(14));
        card.setLayoutParams(cardParams);

        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout titleBlock = new LinearLayout(this);
        titleBlock.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams titleBlockParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        );

        TextView title = new TextView(this);
        title.setText(upload.getTitle());
        title.setTextColor(getColor(R.color.text_dark));
        title.setTextSize(17);
        title.setTypeface(null, Typeface.BOLD);
        title.setMaxLines(2);

        TextView author = new TextView(this);
        author.setText(upload.getAuthor());
        author.setTextColor(getColor(R.color.text_muted));
        author.setTextSize(13);
        author.setPadding(0, dp(3), 0, 0);

        titleBlock.addView(title);
        titleBlock.addView(author);
        headerRow.addView(titleBlock, titleBlockParams);
        headerRow.addView(createStatusChip(upload.getGenerationStatus()));
        card.addView(headerRow);

        TextView meta = new TextView(this);
        meta.setText(upload.getVoiceLabel() + " - " + formatTimestamp(upload));
        meta.setTextColor(getColor(R.color.text_muted));
        meta.setTextSize(13);
        meta.setPadding(0, dp(12), 0, 0);
        card.addView(meta);

        if (upload.getGenerationError() != null) {
            TextView error = new TextView(this);
            error.setText(upload.getGenerationError());
            error.setTextColor(0xFF9E3A32);
            error.setTextSize(13);
            error.setPadding(0, dp(8), 0, 0);
            card.addView(error);
        }

        View action = createActionButton(upload);
        if (action != null) {
            LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(48)
            );
            actionParams.setMargins(0, dp(14), 0, 0);
            card.addView(action, actionParams);
        }

        return card;
    }

    private TextView createStatusChip(AudiobookGenerationStatus status) {
        TextView chip = new TextView(this);
        chip.setText(status.getValue());
        chip.setTextSize(12);
        chip.setTypeface(null, Typeface.BOLD);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(10), dp(6), dp(10), dp(6));
        chip.setTextColor(statusTextColor(status));

        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(dp(18));
        background.setColor(statusBackgroundColor(status));
        chip.setBackground(background);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(dp(12), 0, 0, 0);
        chip.setLayoutParams(params);
        return chip;
    }

    private View createActionButton(UserGeneratedAudiobook upload) {
        if (upload.canRequestGeneration()) {
            MaterialButton button = new MaterialButton(this);
            boolean failed = upload.getGenerationStatus() == AudiobookGenerationStatus.FAILED;
            boolean loading = upload.getId().equals(loadingBookId);
            button.setAllCaps(false);
            button.setText(loading ? "Requesting..." : (failed ? "Retry Generation" : "Request Generation"));
            button.setTextColor(getColor(R.color.white));
            button.setTextSize(15);
            button.setEnabled(!loadingBookIdActive());
            button.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.accent)));
            button.setOnClickListener(v -> requestGeneration(upload));
            return button;
        }

        if (upload.getGenerationStatus() == AudiobookGenerationStatus.READY_FOR_REVIEW) {
            MaterialButton button = new MaterialButton(this);
            button.setAllCaps(false);
            button.setText("Preview Audiobook");
            button.setTextColor(getColor(R.color.accent));
            button.setTextSize(15);
            button.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.accent_soft)));
            button.setOnClickListener(v -> openBookDetail(upload, true));
            return button;
        }

        if (upload.getGenerationStatus() == AudiobookGenerationStatus.PUBLISHED) {
            MaterialButton button = new MaterialButton(this);
            button.setAllCaps(false);
            button.setText("View Audiobook");
            button.setTextColor(getColor(R.color.accent));
            button.setTextSize(15);
            button.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.accent_soft)));
            button.setOnClickListener(v -> openBookDetail(upload, false));
            return button;
        }

        return null;
    }

    private void requestGeneration(UserGeneratedAudiobook upload) {
        if (upload == null || loadingBookIdActive()) {
            return;
        }
        ensureGenerationNotifications();
        loadingBookId = upload.getId();
        renderUploads(currentUploads);
        repository.requestGeneration(upload.getId(), new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                loadingBookId = null;
                Toast.makeText(MyUploadsActivity.this, "Generation request queued.", Toast.LENGTH_SHORT).show();
                renderUploads(currentUploads);
            }

            @Override
            public void onError(Exception exception) {
                loadingBookId = null;
                Toast.makeText(
                        MyUploadsActivity.this,
                        AuthRepository.friendlyError(exception),
                        Toast.LENGTH_LONG
                ).show();
                renderUploads(currentUploads);
            }
        });
    }

    private void openBookDetail(UserGeneratedAudiobook upload, boolean creatorPreview) {
        Intent intent = new Intent(this, BookDetailActivity.class);
        intent.putExtra(BookDetailActivity.EXTRA_BOOK_ID, upload.getId());
        intent.putExtra(BookDetailActivity.EXTRA_CREATOR_PREVIEW, creatorPreview);
        startActivity(intent);
    }

    private boolean loadingBookIdActive() {
        return loadingBookId != null && !loadingBookId.trim().isEmpty();
    }

    private boolean hasPendingUploads(List<UserGeneratedAudiobook> uploads) {
        if (uploads == null) {
            return false;
        }
        for (UserGeneratedAudiobook upload : uploads) {
            if (upload != null && upload.getGenerationStatus() == AudiobookGenerationStatus.PENDING_GENERATION) {
                return true;
            }
        }
        return false;
    }

    private void ensureGenerationNotifications() {
        registerNotificationTokenOnce();
        requestNotificationPermissionOnce();
    }

    private void registerNotificationTokenOnce() {
        if (notificationRegistrationAttempted || notificationTokenRepository == null) {
            return;
        }
        notificationRegistrationAttempted = true;
        notificationTokenRepository.registerCurrentDevice(new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                // No UI needed; live Firestore refresh remains the foreground source of truth.
            }

            @Override
            public void onError(Exception exception) {
                // Token registration is best-effort and should not block generation.
            }
        });
    }

    private void requestNotificationPermissionOnce() {
        if (Build.VERSION.SDK_INT < 33 || notificationPermissionRequested) {
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        notificationPermissionRequested = true;
        requestPermissions(
                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                REQUEST_POST_NOTIFICATIONS
        );
    }

    private void showMessage(String message) {
        if (emptyState == null) {
            return;
        }
        if (message == null || message.trim().isEmpty()) {
            emptyState.setVisibility(View.GONE);
            emptyState.setText("");
        } else {
            emptyState.setVisibility(View.VISIBLE);
            emptyState.setText(message);
        }
    }

    private String formatTimestamp(UserGeneratedAudiobook upload) {
        Timestamp timestamp = upload.getUpdatedAt() == null ? upload.getCreatedAt() : upload.getUpdatedAt();
        if (timestamp == null) {
            return "Recently updated";
        }
        SimpleDateFormat formatter = new SimpleDateFormat("MMM d, yyyy", Locale.US);
        return "Updated " + formatter.format(timestamp.toDate());
    }

    private int statusBackgroundColor(AudiobookGenerationStatus status) {
        switch (status) {
            case PENDING_GENERATION:
                return 0xFFFFF4CF;
            case FAILED:
                return 0xFFF9D8D5;
            case READY_FOR_REVIEW:
                return 0xFFDDEAF7;
            case PUBLISHED:
                return 0xFFDCE8DD;
            case REJECTED:
                return 0xFFE4E4E4;
            case DRAFT:
            default:
                return getColor(R.color.accent_soft);
        }
    }

    private int statusTextColor(AudiobookGenerationStatus status) {
        switch (status) {
            case PENDING_GENERATION:
                return 0xFF7A5A00;
            case FAILED:
                return 0xFF9E3A32;
            case READY_FOR_REVIEW:
                return 0xFF315F8C;
            case PUBLISHED:
                return getColor(R.color.accent);
            case REJECTED:
                return 0xFF5E5E5E;
            case DRAFT:
            default:
                return getColor(R.color.accent);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
