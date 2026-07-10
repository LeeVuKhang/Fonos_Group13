package com.example.fonos_group13;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.fonos_group13.data.auth.AuthRepository;
import com.example.fonos_group13.data.core.RepositoryCallback;
import com.example.fonos_group13.data.creator.CreatorAudiobookRepository;
import com.example.fonos_group13.model.AudiobookGenerationStatus;
import com.example.fonos_group13.model.UserGeneratedAudiobook;
import com.example.fonos_group13.model.UserGeneratedChapter;
import com.example.fonos_group13.notifications.GenerationNotificationSetup;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.shape.ShapeAppearanceModel;
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
    private LinearLayout statePanel;
    private TextView stateTitle;
    private TextView stateMessage;
    private MaterialButton stateAction;
    private MaterialButton createButton;
    private ListenerRegistration uploadsRegistration;
    private List<UserGeneratedAudiobook> currentUploads = Collections.emptyList();
    private final Map<String, ListenerRegistration> chapterRegistrations = new HashMap<>();
    private final Map<String, List<UserGeneratedChapter>> chaptersByBookId = new HashMap<>();
    private String loadingBookId;
    private String loadingChapterKey;
    private String visibilityBookId;
    private String deletingChapterKey;
    private String publishingBookId;

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
        statePanel = findViewById(R.id.uploads_state_panel);
        stateTitle = findViewById(R.id.uploads_state_title);
        stateMessage = findViewById(R.id.uploads_state_message);
        stateAction = findViewById(R.id.uploads_state_action);
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
        boolean renderedAnyUpload = false;
        for (UserGeneratedAudiobook upload : uploads) {
            if (upload == null) {
                continue;
            }
            uploadsContainer.addView(createUploadCard(upload));
            renderedAnyUpload = true;
        }
        if (!renderedAnyUpload) {
            showMessage("No uploads yet.");
        }
    }

    private View createUploadCard(UserGeneratedAudiobook upload) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_upload_card);
        card.setClipToOutline(true);
        card.setElevation(dp(2));
        card.setPadding(dp(18), dp(18), dp(18), dp(18));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dp(18));
        card.setLayoutParams(cardParams);

        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setGravity(Gravity.TOP);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);

        ShapeableImageView cover = createCoverView(upload);
        LinearLayout.LayoutParams coverParams = new LinearLayout.LayoutParams(dp(74), dp(98));
        coverParams.setMargins(0, 0, dp(14), 0);
        headerRow.addView(cover, coverParams);

        LinearLayout infoBlock = new LinearLayout(this);
        infoBlock.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams infoBlockParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        );

        LinearLayout badgeRow = new LinearLayout(this);
        badgeRow.setGravity(Gravity.CENTER_VERTICAL);
        badgeRow.setOrientation(LinearLayout.HORIZONTAL);

        TextView statusChip = createStatusChip(upload.getGenerationStatus());
        LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        badgeRow.addView(statusChip, chipParams);

        TextView visibilityChip = createVisibilityChip(upload);
        LinearLayout.LayoutParams visibilityParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        visibilityParams.setMargins(dp(8), 0, 0, 0);
        badgeRow.addView(visibilityChip, visibilityParams);
        infoBlock.addView(badgeRow);

        TextView title = new TextView(this);
        title.setText(upload.getTitle());
        title.setTextColor(getColor(R.color.text_dark));
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        title.setMaxLines(2);
        title.setEllipsize(TextUtils.TruncateAt.END);
        title.setIncludeFontPadding(false);
        title.setPadding(0, dp(8), 0, 0);

        TextView author = new TextView(this);
        author.setText(upload.getAuthor());
        author.setTextColor(getColor(R.color.text_muted));
        author.setTextSize(13);
        author.setMaxLines(1);
        author.setEllipsize(TextUtils.TruncateAt.END);
        author.setPadding(0, dp(5), 0, 0);

        TextView meta = new TextView(this);
        meta.setText(formatUploadMeta(upload));
        meta.setTextColor(getColor(R.color.text_muted));
        meta.setTextSize(12);
        meta.setMaxLines(2);
        meta.setEllipsize(TextUtils.TruncateAt.END);
        meta.setPadding(0, dp(8), 0, 0);

        infoBlock.addView(title);
        infoBlock.addView(author);
        infoBlock.addView(meta);
        headerRow.addView(infoBlock, infoBlockParams);
        card.addView(headerRow);

        if (upload.getGenerationError() != null) {
            TextView error = createErrorText(upload.getGenerationError());
            LinearLayout.LayoutParams errorParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            errorParams.setMargins(0, dp(14), 0, 0);
            card.addView(error, errorParams);
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

    private ShapeableImageView createCoverView(UserGeneratedAudiobook upload) {
        ShapeableImageView cover = new ShapeableImageView(this);
        cover.setScaleType(ImageView.ScaleType.CENTER_CROP);
        cover.setBackgroundResource(R.drawable.bg_cover_placeholder);
        cover.setContentDescription(upload.getTitle() + " cover");
        cover.setShapeAppearanceModel(
                ShapeAppearanceModel.builder()
                        .setAllCornerSizes(dp(14))
                        .build()
        );
        cover.setStrokeColor(ColorStateList.valueOf(0x1F26312A));
        cover.setStrokeWidth(dp(1));

        if (TextUtils.isEmpty(upload.getCoverUrl())) {
            Glide.with(cover).clear(cover);
            cover.setImageResource(R.drawable.bg_cover_placeholder);
            return cover;
        }

        Glide.with(cover)
                .load(upload.getCoverUrl())
                .centerCrop()
                .placeholder(R.drawable.bg_cover_placeholder)
                .error(R.drawable.bg_cover_placeholder)
                .into(cover);
        return cover;
    }

    private String formatUploadMeta(UserGeneratedAudiobook upload) {
        return formatVoiceLabel(upload) + " - " + formatTimestamp(upload);
    }

    private String formatVisibilityLabel(UserGeneratedAudiobook upload) {
        return upload.isPublished() && !upload.isHiddenByCreator() ? "Public" : "Private";
    }

    private String formatVoiceLabel(UserGeneratedAudiobook upload) {
        String gender = trimToNull(upload.getVoiceGender());
        String voiceLabel = gender == null
                ? "Voice"
                : gender.substring(0, 1).toUpperCase(Locale.US) + gender.substring(1).toLowerCase(Locale.US) + " voice";
        String voiceName = trimToNull(upload.getPollyVoiceId());
        return voiceName == null ? voiceLabel : voiceName + " - " + voiceLabel;
    }

    private TextView createErrorText(String message) {
        TextView error = new TextView(this);
        error.setText(message);
        error.setTextColor(getColor(R.color.error_text));
        error.setTextSize(13);
        error.setLineSpacing(dp(2), 1f);
        return error;
    }

    private TextView createStatusChip(AudiobookGenerationStatus status) {
        TextView chip = new TextView(this);
        chip.setText(status.getDisplayLabel());
        chip.setTextSize(11);
        chip.setTypeface(null, Typeface.BOLD);
        chip.setGravity(Gravity.CENTER);
        chip.setMaxLines(1);
        chip.setEllipsize(TextUtils.TruncateAt.END);
        chip.setIncludeFontPadding(false);
        chip.setPadding(dp(10), dp(6), dp(10), dp(6));
        chip.setTextColor(statusTextColor(status));

        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(dp(14));
        background.setColor(statusBackgroundColor(status));
        chip.setBackground(background);
        return chip;
    }

    private TextView createVisibilityChip(UserGeneratedAudiobook upload) {
        boolean publicBook = upload.isPublished() && !upload.isHiddenByCreator();
        TextView chip = new TextView(this);
        chip.setText(formatVisibilityLabel(upload));
        chip.setTextSize(11);
        chip.setTypeface(null, Typeface.BOLD);
        chip.setGravity(Gravity.CENTER);
        chip.setMaxLines(1);
        chip.setIncludeFontPadding(false);
        chip.setPadding(dp(10), dp(6), dp(10), dp(6));
        chip.setTextColor(publicBook ? getColor(R.color.accent) : getColor(R.color.warning_text));

        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(dp(14));
        background.setColor(publicBook ? getColor(R.color.accent_soft) : 0xFFFFF4CF);
        chip.setBackground(background);
        return chip;
    }

    private View createChapterPanel(UserGeneratedAudiobook upload) {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);

        List<UserGeneratedChapter> chapters = chaptersByBookId.get(upload.getId());
        LinearLayout headingRow = new LinearLayout(this);
        headingRow.setGravity(Gravity.CENTER_VERTICAL);
        headingRow.setOrientation(LinearLayout.HORIZONTAL);

        TextView heading = new TextView(this);
        heading.setText("Chapters");
        heading.setTextColor(getColor(R.color.text_main));
        heading.setTextSize(15);
        heading.setTypeface(null, Typeface.BOLD);
        heading.setIncludeFontPadding(false);
        headingRow.addView(heading, new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        ));

        LinearLayout.LayoutParams addChapterParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(36)
        );
        addChapterParams.setMargins(dp(12), 0, 0, 0);
        headingRow.addView(createAddChapterButton(upload), addChapterParams);
        panel.addView(headingRow);

        if (chapters == null) {
            panel.addView(createMutedText("Loading chapters..."));
            return panel;
        }
        if (chapters.isEmpty()) {
            panel.addView(createMutedText("No active chapters."));
            return panel;
        }

        panel.addView(createMutedText(formatChapterSummary(chapters)));

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
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        row.setBackgroundResource(R.drawable.bg_upload_chapter_row);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);

        TextView title = new TextView(this);
        title.setText(chapter.getTitle());
        title.setTextColor(getColor(R.color.text_main));
        title.setTextSize(14);
        title.setTypeface(null, Typeface.BOLD);
        title.setMaxLines(2);
        title.setEllipsize(TextUtils.TruncateAt.END);
        title.setIncludeFontPadding(false);
        header.addView(title, new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        ));

        TextView chip = createStatusChip(chapter.getGenerationStatus());
        LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        chipParams.setMargins(dp(10), 0, 0, 0);
        header.addView(chip, chipParams);
        row.addView(header);

        if (chapter.getGenerationError() != null) {
            TextView error = createErrorText(chapter.getGenerationError());
            LinearLayout.LayoutParams errorParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            errorParams.setMargins(0, dp(8), 0, 0);
            row.addView(error, errorParams);
        }

        View actions = createChapterActions(upload, chapter);
        if (actions != null) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, dp(10), 0, 0);
            row.addView(actions, params);
        }
        return row;
    }

    private View createChapterActions(UserGeneratedAudiobook upload, UserGeneratedChapter chapter) {
        List<MaterialButton> buttons = new ArrayList<>();
        if (chapter.canEdit()) {
            buttons.add(createSmallActionButton(
                    "Edit",
                    getColor(R.color.accent),
                    getColor(R.color.accent_soft),
                    v -> openChapterEditor(upload, chapter)
            ));
        }
        if (chapter.canRequestGeneration()) {
            boolean loading = chapterKey(upload.getId(), chapter.getId()).equals(loadingChapterKey);
            buttons.add(createSmallActionButton(
                    loading ? "Requesting..." : "Request",
                    getColor(R.color.white),
                    getColor(R.color.accent),
                    v -> requestChapterGeneration(upload, chapter)
            ));
        }
        if (chapter.canPreview()) {
            buttons.add(createChapterPreviewButton(upload, chapter));
        }
        if (canPublishChapter(chapter)) {
            boolean loading = upload.getId().equals(publishingBookId);
            buttons.add(createSmallActionButton(
                    loading ? "Publishing..." : "Publish",
                    getColor(R.color.white),
                    getColor(R.color.accent),
                    v -> publishUploadUpdate(upload)
            ));
        }

        boolean hasOverflow = chapter.canDelete();
        if (buttons.isEmpty() && !hasOverflow) {
            return null;
        }

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < buttons.size(); i++) {
            actions.addView(buttons.get(i), chapterActionButtonParams(i > 0, buttons.size()));
        }
        if (hasOverflow) {
            View spacer = new View(this);
            actions.addView(spacer, new LinearLayout.LayoutParams(0, dp(40), 1f));
            actions.addView(createChapterOverflowButton(upload, chapter), overflowButtonParams(false));
        }
        return actions;
    }

    private MaterialButton createChapterPreviewButton(
            UserGeneratedAudiobook upload,
            UserGeneratedChapter chapter
    ) {
        MaterialButton button = createOutlineActionButton(
                "Preview",
                13,
                12,
                v -> openChapterPreview(upload, chapter)
        );
        button.setIconResource(R.drawable.ic_play);
        button.setIconTint(ColorStateList.valueOf(getColor(R.color.accent)));
        button.setIconSize(dp(16));
        button.setIconPadding(dp(4));
        return button;
    }

    private boolean canPublishChapter(UserGeneratedChapter chapter) {
        return chapter != null
                && chapter.getGenerationStatus() == AudiobookGenerationStatus.READY_FOR_REVIEW
                && chapter.canPreview();
    }

    private View createChapterOverflowButton(UserGeneratedAudiobook upload, UserGeneratedChapter chapter) {
        ImageView button = new ImageView(this);
        button.setImageResource(R.drawable.ic_more_vert);
        button.setColorFilter(getColor(R.color.text_muted));
        button.setContentDescription("Chapter actions");
        button.setClickable(true);
        button.setFocusable(true);
        button.setPadding(dp(10), dp(10), dp(10), dp(10));

        TypedValue outValue = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true);
        button.setBackgroundResource(outValue.resourceId);

        button.setEnabled(!requestInProgress());
        button.setAlpha(button.isEnabled() ? 1f : 0.45f);
        button.setOnClickListener(v -> showChapterOverflowMenu(v, upload, chapter));
        return button;
    }

    private void showChapterOverflowMenu(View anchor, UserGeneratedAudiobook upload, UserGeneratedChapter chapter) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add(chapter.getDeleteActionLabel());
        menu.setOnMenuItemClickListener(item -> {
            showDeleteChapterConfirmation(upload, chapter);
            return true;
        });
        menu.show();
    }

    private TextView createMutedText(String message) {
        TextView text = new TextView(this);
        text.setText(message);
        text.setTextColor(getColor(R.color.text_muted));
        text.setTextSize(13);
        text.setLineSpacing(dp(2), 1f);
        text.setPadding(0, dp(10), 0, 0);
        return text;
    }

    private String formatChapterProgress(List<UserGeneratedChapter> chapters) {
        int total = chapters == null ? 0 : chapters.size();
        if (total == 0) {
            return "No chapters";
        }
        return publishedChapterCount(chapters) + "/" + total + " published";
    }

    private String formatChapterSummary(List<UserGeneratedChapter> chapters) {
        int total = chapters == null ? 0 : chapters.size();
        if (total == 0) {
            return "No active chapters";
        }

        List<String> parts = new ArrayList<>();
        parts.add(total == 1 ? "1 chapter" : total + " chapters");
        appendCount(parts, publishedChapterCount(chapters), "published");
        appendCount(parts, statusCount(chapters, AudiobookGenerationStatus.READY_FOR_REVIEW), "ready for review");
        appendCount(parts, statusCount(chapters, AudiobookGenerationStatus.PENDING_GENERATION), "pending");
        appendCount(parts, statusCount(chapters, AudiobookGenerationStatus.DRAFT), "draft");
        appendCount(parts, statusCount(chapters, AudiobookGenerationStatus.FAILED), "failed");
        return TextUtils.join(" \u00B7 ", parts);
    }

    private void appendCount(List<String> parts, int count, String label) {
        if (count > 0) {
            parts.add(count + " " + label);
        }
    }

    private int publishedChapterCount(List<UserGeneratedChapter> chapters) {
        int count = 0;
        if (chapters == null) {
            return count;
        }
        for (UserGeneratedChapter chapter : chapters) {
            if (chapter != null
                    && (chapter.isPublished()
                    || chapter.getGenerationStatus() == AudiobookGenerationStatus.PUBLISHED)) {
                count++;
            }
        }
        return count;
    }

    private int statusCount(List<UserGeneratedChapter> chapters, AudiobookGenerationStatus status) {
        int count = 0;
        if (chapters == null) {
            return count;
        }
        for (UserGeneratedChapter chapter : chapters) {
            if (chapter != null && chapter.getGenerationStatus() == status) {
                count++;
            }
        }
        return count;
    }

    private View createActionButtons(UserGeneratedAudiobook upload) {
        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        actions.setOrientation(LinearLayout.HORIZONTAL);

        View primaryAction = createPrimaryActionButton(upload);
        View visibilityAction = createVisibilityButton(upload);
        if (primaryAction == null && visibilityAction == null) {
            return null;
        }
        if (primaryAction != null) {
            actions.addView(primaryAction, bookActionParams(false, visibilityAction != null));
        }

        if (visibilityAction != null) {
            actions.addView(visibilityAction, bookActionParams(primaryAction != null, primaryAction != null));
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
            configureActionButton(button, 15, 14);
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
            configureActionButton(button, 15, 14);
            button.setOnClickListener(v -> requestGeneration(upload));
            return button;
        }

        if (upload.getGenerationStatus() == AudiobookGenerationStatus.READY_FOR_REVIEW
                && !upload.isPublished()) {
            MaterialButton button = new MaterialButton(this);
            button.setAllCaps(false);
            button.setText("Preview Audiobook");
            button.setTextColor(getColor(R.color.accent));
            button.setTextSize(15);
            button.setEnabled(!requestInProgress());
            button.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.accent_soft)));
            configureActionButton(button, 15, 14);
            button.setOnClickListener(v -> openBookDetail(upload, true));
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
                : (hidden ? "Make Public" : "Make Private"));
        button.setTextColor(hidden ? getColor(R.color.accent) : 0xFF9E3A32);
        button.setTextSize(14);
        button.setEnabled(!requestInProgress());
        button.setBackgroundTintList(ColorStateList.valueOf(hidden ? getColor(R.color.accent_soft) : getColor(R.color.danger_soft)));
        configureActionButton(button, 14, 14);
        button.setOnClickListener(v -> toggleVisibility(upload));
        return button;
    }

    private MaterialButton createAddChapterButton(UserGeneratedAudiobook upload) {
        return createOutlineActionButton(
                getString(R.string.my_uploads_add_chapter),
                13,
                14,
                v -> openAddChapter(upload)
        );
    }

    private MaterialButton createOutlineActionButton(
            String text,
            int textSize,
            int cornerRadius,
            View.OnClickListener listener
    ) {
        MaterialButton button = new MaterialButton(this);
        button.setAllCaps(false);
        button.setText(text);
        button.setTextColor(getColor(R.color.accent));
        button.setEnabled(!requestInProgress());
        button.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.surface)));
        button.setStrokeColor(ColorStateList.valueOf(getColor(R.color.stroke_soft)));
        button.setStrokeWidth(dp(1));
        button.setPadding(dp(12), 0, dp(12), 0);
        configureActionButton(button, textSize, cornerRadius);
        button.setOnClickListener(listener);
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
        button.setTextColor(textColor);
        button.setEnabled(!requestInProgress());
        button.setBackgroundTintList(ColorStateList.valueOf(backgroundColor));
        configureActionButton(button, 13, 12);
        button.setOnClickListener(listener);
        return button;
    }

    private void configureActionButton(MaterialButton button, int textSize, int cornerRadius) {
        button.setTextSize(textSize);
        button.setMaxLines(1);
        button.setEllipsize(TextUtils.TruncateAt.END);
        button.setGravity(Gravity.CENTER);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setInsetTop(0);
        button.setInsetBottom(0);
        button.setCornerRadius(dp(cornerRadius));
    }

    private LinearLayout.LayoutParams bookActionParams(boolean hasStartMargin, boolean split) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                split ? 0 : ViewGroup.LayoutParams.MATCH_PARENT,
                dp(46),
                split ? 1f : 0f
        );
        if (hasStartMargin) {
            params.setMargins(dp(10), 0, 0, 0);
        }
        return params;
    }

    private LinearLayout.LayoutParams chapterActionButtonParams(boolean hasStartMargin, int visibleActionCount) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(40)
        );
        if (hasStartMargin) {
            params.setMargins(dp(8), 0, 0, 0);
        }
        return params;
    }

    private LinearLayout.LayoutParams overflowButtonParams(boolean hasStartMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(44), dp(40));
        if (hasStartMargin) {
            params.setMargins(dp(8), 0, 0, 0);
        }
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
                    .setTitle("Make Private")
                    .setMessage("Make \"" + upload.getTitle() + "\" private? It will stay here in My Uploads.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Make Private", (dialog, which) -> setAudiobookVisibility(upload, true))
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
                        hiddenByCreator ? "Made private." : "Made public.",
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

    private void publishUploadUpdate(UserGeneratedAudiobook upload) {
        if (upload == null || requestInProgress()) {
            return;
        }
        publishingBookId = upload.getId();
        renderUploads(currentUploads);
        repository.publishAudiobook(upload.getId(), new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                publishingBookId = null;
                Toast.makeText(MyUploadsActivity.this, "Updates published.", Toast.LENGTH_SHORT).show();
                renderUploads(currentUploads);
            }

            @Override
            public void onError(Exception exception) {
                publishingBookId = null;
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

    private void openChapterPreview(UserGeneratedAudiobook upload, UserGeneratedChapter chapter) {
        if (upload == null || chapter == null) {
            return;
        }
        Intent intent = new Intent(this, ActivityReader.class);
        intent.putExtra(ActivityReader.EXTRA_BOOK_ID, upload.getId());
        intent.putExtra(ActivityReader.EXTRA_CHAPTER_ID, chapter.getId());
        intent.putExtra(ActivityReader.EXTRA_AUTO_PLAY, true);
        intent.putExtra(ActivityReader.EXTRA_CREATOR_PREVIEW, true);
        intent.putExtra(ActivityReader.EXTRA_SINGLE_CHAPTER_PREVIEW, true);
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

    private boolean publishingBookIdActive() {
        return publishingBookId != null && !publishingBookId.trim().isEmpty();
    }

    private boolean requestInProgress() {
        return loadingBookIdActive()
                || loadingChapterKeyActive()
                || visibilityBookIdActive()
                || deletingChapterKeyActive()
                || publishingBookIdActive();
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
        if (statePanel == null || stateTitle == null || stateMessage == null || stateAction == null) {
            return;
        }
        if (message == null || message.trim().isEmpty()) {
            statePanel.setVisibility(View.GONE);
            stateAction.setVisibility(View.GONE);
            stateAction.setOnClickListener(null);
            stateTitle.setText("");
            stateMessage.setText("");
            return;
        }

        String trimmed = message.trim();
        if ("Loading uploads...".equals(trimmed)) {
            showStatePanel(
                    "Loading uploads...",
                    "Checking the latest generation status for your audiobooks.",
                    null,
                    null
            );
            return;
        }
        if ("No uploads yet.".equals(trimmed)) {
            showStatePanel(
                    "No uploads yet.",
                    "Create an audiobook draft and it will appear here for review, publishing, and chapter updates.",
                    "+ Create New Audiobook",
                    v -> startActivity(new Intent(this, CreateAudiobookActivity.class))
            );
            return;
        }
        if ("Could not load your uploads.".equals(trimmed)) {
            showStatePanel(
                    "Could not load your uploads.",
                    "Check your connection and try again.",
                    "Try Again",
                    v -> startObservingUploads()
            );
        } else {
            showStatePanel(trimmed, "", null, null);
        }
    }

    private void showStatePanel(
            String title,
            String message,
            String actionText,
            View.OnClickListener action
    ) {
        statePanel.setVisibility(View.VISIBLE);
        stateTitle.setText(title);
        stateMessage.setText(message);
        stateMessage.setVisibility(TextUtils.isEmpty(message) ? View.GONE : View.VISIBLE);
        if (TextUtils.isEmpty(actionText) || action == null) {
            stateAction.setVisibility(View.GONE);
            stateAction.setOnClickListener(null);
        } else {
            stateAction.setText(actionText);
            stateAction.setVisibility(View.VISIBLE);
            stateAction.setOnClickListener(action);
        }
    }

    private String formatTimestamp(UserGeneratedAudiobook upload) {
        long timestampMillis = upload.getUpdatedAtMillis() > 0
                ? upload.getUpdatedAtMillis()
                : upload.getCreatedAtMillis();
        if (timestampMillis <= 0) {
            return "Recently updated";
        }
        SimpleDateFormat formatter = new SimpleDateFormat("MMM d, yyyy", Locale.US);
        return "Updated " + formatter.format(new java.util.Date(timestampMillis));
    }

    private int statusBackgroundColor(AudiobookGenerationStatus status) {
        switch (status) {
            case PENDING_GENERATION:
                return 0xFFFFF4CF;
            case FAILED:
                return getColor(R.color.danger_soft);
            case READY_FOR_REVIEW:
                return getColor(R.color.info_soft);
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

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
