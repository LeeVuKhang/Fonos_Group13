package com.example.fonos_group13;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.fonos_group13.data.AuthRepository;
import com.example.fonos_group13.data.CreatorAudiobookRepository;
import com.example.fonos_group13.data.RepositoryCallback;
import com.example.fonos_group13.model.AudiobookGenerationStatus;
import com.example.fonos_group13.model.UserGeneratedAudiobook;
import com.example.fonos_group13.model.UserGeneratedChapter;
import com.example.fonos_group13.notifications.GenerationNotificationSetup;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MyUploadsActivity extends AppCompatActivity {
    private CreatorAudiobookRepository repository;
    private GenerationNotificationSetup notificationSetup;
    private LinearLayout uploadsContainer;
    private TextView emptyState;
    private MaterialButton createButton;
    private ListenerRegistration uploadsRegistration;
    private List<UserGeneratedAudiobook> currentUploads = Collections.emptyList();
    private final Map<String, ListenerRegistration> chapterRegistrations = new HashMap<>();
    private final Map<String, List<UserGeneratedChapter>> chaptersByBookId = new HashMap<>();
    private String loadingBookId;
    private String loadingChapterKey;
    private String visibilityBookId;
    private String deletingChapterKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_my_uploads);

        repository = new CreatorAudiobookRepository(this);
        notificationSetup = new GenerationNotificationSetup(this);
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
                syncChapterObservers(currentUploads);
                renderUploads(currentUploads);
                if (hasPendingUploads(currentUploads) || hasPendingChapters()) {
                    ensureGenerationNotifications();
                }
            }

            @Override
            public void onError(Exception exception) {
                currentUploads = Collections.emptyList();
                stopObservingChapters();
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
        stopObservingChapters();
    }

    private void syncChapterObservers(List<UserGeneratedAudiobook> uploads) {
        Set<String> activeBookIds = new HashSet<>();
        if (uploads != null) {
            for (UserGeneratedAudiobook upload : uploads) {
                if (upload == null || upload.getId() == null || upload.getId().trim().isEmpty()) {
                    continue;
                }
                activeBookIds.add(upload.getId());
                if (!chapterRegistrations.containsKey(upload.getId())) {
                    ListenerRegistration registration = repository.observeUploadChapters(
                            upload.getId(),
                            new RepositoryCallback<List<UserGeneratedChapter>>() {
                                @Override
                                public void onSuccess(List<UserGeneratedChapter> chapters) {
                                    chaptersByBookId.put(
                                            upload.getId(),
                                            chapters == null ? Collections.emptyList() : chapters
                                    );
                                    if (hasPendingChapters()) {
                                        ensureGenerationNotifications();
                                    }
                                    renderUploads(currentUploads);
                                }

                                @Override
                                public void onError(Exception exception) {
                                    chaptersByBookId.put(upload.getId(), Collections.emptyList());
                                    renderUploads(currentUploads);
                                }
                            }
                    );
                    if (registration != null) {
                        chapterRegistrations.put(upload.getId(), registration);
                    }
                }
            }
        }

        List<String> staleBookIds = new ArrayList<>();
        for (String bookId : chapterRegistrations.keySet()) {
            if (!activeBookIds.contains(bookId)) {
                staleBookIds.add(bookId);
            }
        }
        for (String bookId : staleBookIds) {
            ListenerRegistration registration = chapterRegistrations.remove(bookId);
            if (registration != null) {
                registration.remove();
            }
            chaptersByBookId.remove(bookId);
        }
    }

    private void stopObservingChapters() {
        for (ListenerRegistration registration : chapterRegistrations.values()) {
            if (registration != null) {
                registration.remove();
            }
        }
        chapterRegistrations.clear();
        chaptersByBookId.clear();
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
        String visibilityLabel = upload.isHiddenByCreator() ? " - Hidden from public" : "";
        meta.setText(upload.getVoiceLabel() + " - " + formatTimestamp(upload) + visibilityLabel);
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

        View actions = createActionButtons(upload);
        if (actions != null) {
            LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            actionsParams.setMargins(0, dp(14), 0, 0);
            card.addView(actions, actionsParams);
        }

        View chapterPanel = createChapterPanel(upload);
        LinearLayout.LayoutParams chapterParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        chapterParams.setMargins(0, dp(14), 0, 0);
        card.addView(chapterPanel, chapterParams);

        return card;
    }

    private TextView createStatusChip(AudiobookGenerationStatus status) {
        TextView chip = new TextView(this);
        chip.setText(status.getDisplayLabel());
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

    private View createChapterPanel(UserGeneratedAudiobook upload) {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);

        TextView heading = new TextView(this);
        heading.setText("Chapters");
        heading.setTextColor(getColor(R.color.text_dark));
        heading.setTextSize(14);
        heading.setTypeface(null, Typeface.BOLD);
        panel.addView(heading);

        List<UserGeneratedChapter> chapters = chaptersByBookId.get(upload.getId());
        if (chapters == null) {
            panel.addView(createMutedText("Loading chapters..."));
            return panel;
        }
        if (chapters.isEmpty()) {
            panel.addView(createMutedText("No active chapters."));
            panel.addView(createAddChapterButton(upload), fullWidthButtonParams(dp(8), dp(44)));
            return panel;
        }

        for (UserGeneratedChapter chapter : chapters) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, dp(10), 0, 0);
            panel.addView(createChapterRow(upload, chapter), params);
        }
        return panel;
    }

    private View createChapterRow(UserGeneratedAudiobook upload, UserGeneratedChapter chapter) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));

        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(dp(8));
        background.setColor(0xFFF8FAF7);
        background.setStroke(dp(1), 0xFFE3E8DE);
        row.setBackground(background);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);

        TextView title = new TextView(this);
        title.setText(chapter.getTitle());
        title.setTextColor(getColor(R.color.text_dark));
        title.setTextSize(14);
        title.setTypeface(null, Typeface.BOLD);
        title.setMaxLines(2);
        header.addView(title, new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        ));
        header.addView(createStatusChip(chapter.getGenerationStatus()));
        row.addView(header);

        if (chapter.getGenerationError() != null) {
            TextView error = new TextView(this);
            error.setText(chapter.getGenerationError());
            error.setTextColor(0xFF9E3A32);
            error.setTextSize(12);
            error.setPadding(0, dp(6), 0, 0);
            row.addView(error);
        }

        View actions = createChapterActions(upload, chapter);
        if (actions != null) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, dp(8), 0, 0);
            row.addView(actions, params);
        }
        return row;
    }

    private View createChapterActions(UserGeneratedAudiobook upload, UserGeneratedChapter chapter) {
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.VERTICAL);

        boolean hasAction = false;
        if (chapter.canEdit()) {
            actions.addView(createSmallActionButton(
                    "Edit",
                    getColor(R.color.accent),
                    getColor(R.color.accent_soft),
                    v -> openChapterEditor(upload, chapter)
            ), rowButtonParams(hasAction));
            hasAction = true;
        }
        if (chapter.canRequestGeneration()) {
            boolean loading = chapterKey(upload.getId(), chapter.getId()).equals(loadingChapterKey);
            actions.addView(createSmallActionButton(
                    loading ? "Requesting..." : "Request",
                    getColor(R.color.white),
                    getColor(R.color.accent),
                    v -> requestChapterGeneration(upload, chapter)
            ), rowButtonParams(hasAction));
            hasAction = true;
        }
        if (chapter.canPreview()) {
            actions.addView(createSmallActionButton(
                    "Preview",
                    getColor(R.color.accent),
                    getColor(R.color.accent_soft),
                    v -> openBookDetail(upload, true)
            ), rowButtonParams(hasAction));
            hasAction = true;
        }
        if (chapter.canDelete()) {
            boolean deleting = chapterKey(upload.getId(), chapter.getId()).equals(deletingChapterKey);
            actions.addView(createSmallActionButton(
                    deleting ? "Deleting..." : chapter.getDeleteActionLabel(),
                    0xFF9E3A32,
                    0xFFF9D8D5,
                    v -> showDeleteChapterConfirmation(upload, chapter)
            ), rowButtonParams(hasAction));
            hasAction = true;
        }

        return hasAction ? actions : null;
    }

    private TextView createMutedText(String message) {
        TextView text = new TextView(this);
        text.setText(message);
        text.setTextColor(getColor(R.color.text_muted));
        text.setTextSize(13);
        text.setPadding(0, dp(8), 0, 0);
        return text;
    }

    private View createActionButtons(UserGeneratedAudiobook upload) {
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.VERTICAL);

        View primaryAction = createPrimaryActionButton(upload);
        if (primaryAction != null) {
            actions.addView(primaryAction, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(48)
            ));
        }

        View visibilityAction = createVisibilityButton(upload);
        if (visibilityAction != null) {
            LinearLayout.LayoutParams visibilityParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(44)
            );
            visibilityParams.setMargins(0, primaryAction == null ? 0 : dp(10), 0, 0);
            actions.addView(visibilityAction, visibilityParams);
        }
        return actions;
    }

    private View createPrimaryActionButton(UserGeneratedAudiobook upload) {
        if (upload.getGenerationStatus() == AudiobookGenerationStatus.DRAFT) {
            MaterialButton button = new MaterialButton(this);
            button.setAllCaps(false);
            button.setText(upload.hasChapterUpdate() && !upload.activeChapterIsInitialChapter()
                    ? "Edit Chapter Draft"
                    : "Edit Draft");
            button.setTextColor(getColor(R.color.white));
            button.setTextSize(15);
            button.setEnabled(!requestInProgress());
            button.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.accent)));
            button.setOnClickListener(v -> {
                if (upload.hasChapterUpdate() && !upload.activeChapterIsInitialChapter()) {
                    openChapterEditor(upload);
                } else {
                    openDraftEditor(upload);
                }
            });
            return button;
        }

        if (upload.canRequestGeneration()) {
            MaterialButton button = new MaterialButton(this);
            boolean failed = upload.getGenerationStatus() == AudiobookGenerationStatus.FAILED;
            boolean loading = upload.getId().equals(loadingBookId);
            button.setAllCaps(false);
            button.setText(loading ? "Requesting..." : (failed ? "Retry Generation" : "Request Generation"));
            button.setTextColor(getColor(R.color.white));
            button.setTextSize(15);
            button.setEnabled(!requestInProgress());
            button.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.accent)));
            button.setOnClickListener(v -> requestGeneration(upload));
            return button;
        }

        if (upload.getGenerationStatus() == AudiobookGenerationStatus.READY_FOR_REVIEW) {
            MaterialButton button = new MaterialButton(this);
            button.setAllCaps(false);
            button.setText(upload.isPublished() ? "Preview Updates" : "Preview Audiobook");
            button.setTextColor(getColor(R.color.accent));
            button.setTextSize(15);
            button.setEnabled(!requestInProgress());
            button.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.accent_soft)));
            button.setOnClickListener(v -> openBookDetail(upload, true));
            return button;
        }

        if (upload.getGenerationStatus() == AudiobookGenerationStatus.PUBLISHED) {
            MaterialButton button = new MaterialButton(this);
            button.setAllCaps(false);
            button.setText("Add Chapter");
            button.setTextColor(getColor(R.color.accent));
            button.setTextSize(15);
            button.setEnabled(!requestInProgress());
            button.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.accent_soft)));
            button.setOnClickListener(v -> openAddChapter(upload));
            return button;
        }

        return null;
    }

    private View createVisibilityButton(UserGeneratedAudiobook upload) {
        if (!upload.isPublished() && !upload.isHiddenByCreator()) {
            return null;
        }
        MaterialButton button = new MaterialButton(this);
        boolean updating = upload.getId().equals(visibilityBookId);
        boolean hidden = upload.isHiddenByCreator();
        button.setAllCaps(false);
        button.setText(updating
                ? "Updating..."
                : (hidden ? "Show Publicly" : "Hide from Public"));
        button.setTextColor(hidden ? getColor(R.color.accent) : 0xFF9E3A32);
        button.setTextSize(14);
        button.setEnabled(!requestInProgress());
        button.setBackgroundTintList(ColorStateList.valueOf(hidden ? getColor(R.color.accent_soft) : 0xFFF9D8D5));
        button.setOnClickListener(v -> toggleVisibility(upload));
        return button;
    }

    private MaterialButton createAddChapterButton(UserGeneratedAudiobook upload) {
        MaterialButton button = new MaterialButton(this);
        button.setAllCaps(false);
        button.setText("Add Chapter");
        button.setTextColor(getColor(R.color.accent));
        button.setTextSize(14);
        button.setEnabled(!requestInProgress());
        button.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.accent_soft)));
        button.setOnClickListener(v -> openAddChapter(upload));
        return button;
    }

    private MaterialButton createSmallActionButton(
            String text,
            int textColor,
            int backgroundColor,
            View.OnClickListener listener
    ) {
        MaterialButton button = new MaterialButton(this);
        button.setAllCaps(false);
        button.setText(text);
        button.setTextSize(12);
        button.setTextColor(textColor);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setInsetTop(0);
        button.setInsetBottom(0);
        button.setEnabled(!requestInProgress());
        button.setBackgroundTintList(ColorStateList.valueOf(backgroundColor));
        button.setOnClickListener(listener);
        return button;
    }

    private LinearLayout.LayoutParams rowButtonParams(boolean hasPreviousAction) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(38)
        );
        if (hasPreviousAction) {
            params.setMargins(0, dp(8), 0, 0);
        }
        return params;
    }

    private LinearLayout.LayoutParams fullWidthButtonParams(int topMargin, int height) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                height
        );
        params.setMargins(0, topMargin, 0, 0);
        return params;
    }

    private void requestGeneration(UserGeneratedAudiobook upload) {
        if (upload == null || requestInProgress()) {
            return;
        }
        ensureGenerationNotifications();
        loadingBookId = upload.getId();
        renderUploads(currentUploads);
        RepositoryCallback<Void> callback = new RepositoryCallback<Void>() {
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
        };
        if (upload.hasChapterUpdate() && !upload.activeChapterIsInitialChapter()) {
            repository.requestChapterGeneration(upload.getId(), upload.getActiveChapterId(), callback);
        } else {
            repository.requestGeneration(upload.getId(), callback);
        }
    }

    private void toggleVisibility(UserGeneratedAudiobook upload) {
        if (upload == null || requestInProgress()) {
            return;
        }
        boolean shouldHide = !upload.isHiddenByCreator();
        if (shouldHide) {
            new AlertDialog.Builder(this)
                    .setTitle("Hide from Public")
                    .setMessage("Hide \"" + upload.getTitle() + "\" from public discovery? It will stay here in My Uploads.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Hide", (dialog, which) -> setAudiobookVisibility(upload, true))
                    .show();
            return;
        }
        setAudiobookVisibility(upload, false);
    }

    private void setAudiobookVisibility(UserGeneratedAudiobook upload, boolean hiddenByCreator) {
        if (upload == null || requestInProgress()) {
            return;
        }
        visibilityBookId = upload.getId();
        renderUploads(currentUploads);
        repository.setAudiobookVisibility(upload.getId(), hiddenByCreator, new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                visibilityBookId = null;
                Toast.makeText(
                        MyUploadsActivity.this,
                        hiddenByCreator ? "Hidden from public." : "Visible publicly.",
                        Toast.LENGTH_SHORT
                ).show();
                renderUploads(currentUploads);
            }

            @Override
            public void onError(Exception exception) {
                visibilityBookId = null;
                Toast.makeText(
                        MyUploadsActivity.this,
                        AuthRepository.friendlyError(exception),
                        Toast.LENGTH_LONG
                ).show();
                renderUploads(currentUploads);
            }
        });
    }

    private void requestChapterGeneration(UserGeneratedAudiobook upload, UserGeneratedChapter chapter) {
        if (upload == null || chapter == null || requestInProgress()) {
            return;
        }
        ensureGenerationNotifications();
        loadingChapterKey = chapterKey(upload.getId(), chapter.getId());
        renderUploads(currentUploads);
        repository.requestChapterGeneration(upload.getId(), chapter.getId(), new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                loadingChapterKey = null;
                Toast.makeText(MyUploadsActivity.this, "Chapter generation queued.", Toast.LENGTH_SHORT).show();
                renderUploads(currentUploads);
            }

            @Override
            public void onError(Exception exception) {
                loadingChapterKey = null;
                Toast.makeText(
                        MyUploadsActivity.this,
                        AuthRepository.friendlyError(exception),
                        Toast.LENGTH_LONG
                ).show();
                renderUploads(currentUploads);
            }
        });
    }

    private void showDeleteChapterConfirmation(UserGeneratedAudiobook upload, UserGeneratedChapter chapter) {
        if (upload == null || chapter == null || requestInProgress()) {
            return;
        }
        boolean pending = chapter.getGenerationStatus() == AudiobookGenerationStatus.PENDING_GENERATION;
        String title = pending ? "Cancel Chapter" : "Delete Chapter";
        String message = pending
                ? "Cancel \"" + chapter.getTitle() + "\"? Generation may finish in the background, but its result will be ignored."
                : "Delete \"" + chapter.getTitle() + "\" from this audiobook? Existing audio files and metadata will be kept.";
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton("Cancel", null)
                .setPositiveButton(pending ? "Cancel Chapter" : "Delete", (dialog, which) -> deleteChapter(upload, chapter))
                .show();
    }

    private void deleteChapter(UserGeneratedAudiobook upload, UserGeneratedChapter chapter) {
        if (upload == null || chapter == null || requestInProgress()) {
            return;
        }
        deletingChapterKey = chapterKey(upload.getId(), chapter.getId());
        renderUploads(currentUploads);
        repository.deleteChapter(upload.getId(), chapter.getId(), new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                String key = deletingChapterKey;
                deletingChapterKey = null;
                removeChapterFromPanel(key);
                Toast.makeText(MyUploadsActivity.this, "Chapter removed.", Toast.LENGTH_SHORT).show();
                renderUploads(currentUploads);
            }

            @Override
            public void onError(Exception exception) {
                deletingChapterKey = null;
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

    private void openDraftEditor(UserGeneratedAudiobook upload) {
        Intent intent = new Intent(this, CreateAudiobookActivity.class);
        intent.putExtra(CreateAudiobookActivity.EXTRA_EDIT_BOOK_ID, upload.getId());
        startActivity(intent);
    }

    private void openAddChapter(UserGeneratedAudiobook upload) {
        Intent intent = new Intent(this, ManageChapterActivity.class);
        intent.putExtra(ManageChapterActivity.EXTRA_BOOK_ID, upload.getId());
        startActivity(intent);
    }

    private void openChapterEditor(UserGeneratedAudiobook upload) {
        Intent intent = new Intent(this, ManageChapterActivity.class);
        intent.putExtra(ManageChapterActivity.EXTRA_BOOK_ID, upload.getId());
        intent.putExtra(ManageChapterActivity.EXTRA_CHAPTER_ID, upload.getActiveChapterId());
        startActivity(intent);
    }

    private void openChapterEditor(UserGeneratedAudiobook upload, UserGeneratedChapter chapter) {
        Intent intent = new Intent(this, ManageChapterActivity.class);
        intent.putExtra(ManageChapterActivity.EXTRA_BOOK_ID, upload.getId());
        intent.putExtra(ManageChapterActivity.EXTRA_CHAPTER_ID, chapter.getId());
        startActivity(intent);
    }

    private boolean loadingBookIdActive() {
        return loadingBookId != null && !loadingBookId.trim().isEmpty();
    }

    private boolean loadingChapterKeyActive() {
        return loadingChapterKey != null && !loadingChapterKey.trim().isEmpty();
    }

    private boolean visibilityBookIdActive() {
        return visibilityBookId != null && !visibilityBookId.trim().isEmpty();
    }

    private boolean deletingChapterKeyActive() {
        return deletingChapterKey != null && !deletingChapterKey.trim().isEmpty();
    }

    private boolean requestInProgress() {
        return loadingBookIdActive()
                || loadingChapterKeyActive()
                || visibilityBookIdActive()
                || deletingChapterKeyActive();
    }

    private String chapterKey(String bookId, String chapterId) {
        return (bookId == null ? "" : bookId) + "::" + (chapterId == null ? "" : chapterId);
    }

    private void removeChapterFromPanel(String key) {
        if (key == null || !key.contains("::")) {
            return;
        }
        String[] parts = key.split("::", 2);
        List<UserGeneratedChapter> chapters = chaptersByBookId.get(parts[0]);
        if (chapters == null) {
            return;
        }
        List<UserGeneratedChapter> remaining = new ArrayList<>();
        for (UserGeneratedChapter chapter : chapters) {
            if (chapter != null && !parts[1].equals(chapter.getId())) {
                remaining.add(chapter);
            }
        }
        chaptersByBookId.put(parts[0], remaining);
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

    private boolean hasPendingChapters() {
        for (List<UserGeneratedChapter> chapters : chaptersByBookId.values()) {
            if (chapters == null) {
                continue;
            }
            for (UserGeneratedChapter chapter : chapters) {
                if (chapter != null
                        && chapter.getGenerationStatus() == AudiobookGenerationStatus.PENDING_GENERATION) {
                    return true;
                }
            }
        }
        return false;
    }

    private void ensureGenerationNotifications() {
        if (notificationSetup != null) {
            notificationSetup.ensureReady();
        }
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
