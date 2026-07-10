package com.example.fonos_group13;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.fonos_group13.data.auth.AuthErrorFormatter;
import com.example.fonos_group13.data.core.RepositoryCallback;
import com.example.fonos_group13.data.creator.BackendApiException;
import com.example.fonos_group13.controller.creator.ManageChapterController;
import com.example.fonos_group13.data.creator.DraftSavedGenerationRequestException;
import com.example.fonos_group13.model.CreateChapterDraftInput;
import com.example.fonos_group13.model.CreatorVoiceOption;
import com.example.fonos_group13.model.EditableChapterDraft;
import com.example.fonos_group13.notifications.GenerationNotificationSetup;
import com.example.fonos_group13.ui.ChapterTextCounterState;
import com.google.android.material.button.MaterialButton;

public class ManageChapterActivity extends AppCompatActivity {
    public static final String EXTRA_BOOK_ID = "com.example.fonos_group13.EXTRA_BOOK_ID";
    public static final String EXTRA_CHAPTER_ID = "com.example.fonos_group13.EXTRA_CHAPTER_ID";

    private ManageChapterController repository;
    private GenerationNotificationSetup notificationSetup;
    private TextView headerTitle;
    private EditText inputChapterTitle;
    private EditText inputChapterText;
    private TextView chapterTextCounter;
    private TextView chapterTextFeedback;
    private TextView voicePatrick;
    private TextView voiceRuth;
    private MaterialButton saveDraftButton;
    private MaterialButton requestGenerationButton;
    private CreatorVoiceOption selectedVoice = CreatorVoiceOption.PATRICK;
    private String bookId;
    private String chapterId;
    private boolean loadingDraft;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_manage_chapter);

        repository = new ManageChapterController(
                FonosApplication.container(this).creatorCommandRepository()
        );
        notificationSetup = new GenerationNotificationSetup(this);
        bookId = trimToNull(getIntent().getStringExtra(EXTRA_BOOK_ID));
        chapterId = trimToNull(getIntent().getStringExtra(EXTRA_CHAPTER_ID));
        if (bookId == null) {
            Toast.makeText(this, "Missing audiobook id.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        bindViews();
        setupInsets();
        setupControls();
        selectVoice(CreatorVoiceOption.PATRICK);
        configureMode();
        if (isEditMode()) {
            loadDraftForEdit();
        }
    }

    private void bindViews() {
        headerTitle = findViewById(R.id.manage_chapter_header_title);
        inputChapterTitle = findViewById(R.id.input_chapter_title);
        inputChapterText = findViewById(R.id.input_chapter_text);
        chapterTextCounter = findViewById(R.id.chapter_text_word_counter);
        chapterTextFeedback = findViewById(R.id.chapter_text_feedback);
        voicePatrick = findViewById(R.id.voice_patrick);
        voiceRuth = findViewById(R.id.voice_ruth);
        saveDraftButton = findViewById(R.id.btn_save_draft);
        requestGenerationButton = findViewById(R.id.btn_request_generation);
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
        if (voicePatrick != null) {
            voicePatrick.setOnClickListener(v -> selectVoice(CreatorVoiceOption.PATRICK));
        }
        if (voiceRuth != null) {
            voiceRuth.setOnClickListener(v -> selectVoice(CreatorVoiceOption.RUTH));
        }
        if (saveDraftButton != null) {
            saveDraftButton.setOnClickListener(v -> saveDraft(false));
        }
        if (requestGenerationButton != null) {
            requestGenerationButton.setOnClickListener(v -> saveDraft(true));
        }
        setupChapterTextCounter();
    }

    private void configureMode() {
        if (headerTitle != null) {
            headerTitle.setText(isEditMode() ? "Edit Chapter" : "Add Chapter");
        }
        if (saveDraftButton != null) {
            saveDraftButton.setText(saveButtonText(false, false));
        }
        if (requestGenerationButton != null) {
            requestGenerationButton.setText(requestButtonText(false, false));
        }
    }

    private void selectVoice(CreatorVoiceOption voiceOption) {
        selectedVoice = voiceOption == null ? CreatorVoiceOption.PATRICK : voiceOption;
        bindVoiceChip(voicePatrick, selectedVoice == CreatorVoiceOption.PATRICK);
        bindVoiceChip(voiceRuth, selectedVoice == CreatorVoiceOption.RUTH);
    }

    private void bindVoiceChip(TextView chip, boolean active) {
        if (chip == null) {
            return;
        }
        chip.setBackgroundResource(active ? R.drawable.bg_chip_active : R.drawable.bg_chip_white);
        chip.setTextColor(getColor(active ? R.color.white : R.color.text_muted));
        chip.setTypeface(null, active ? Typeface.BOLD : Typeface.NORMAL);
    }

    private void saveDraft(boolean requestGeneration) {
        if (loadingDraft) {
            return;
        }
        CreateChapterDraftInput input = readAndValidateInput();
        if (input == null) {
            return;
        }

        setLoading(true, requestGeneration);
        RepositoryCallback<String> callback = new RepositoryCallback<String>() {
            @Override
            public void onSuccess(String savedChapterId) {
                setLoading(false, requestGeneration);
                Toast.makeText(
                        ManageChapterActivity.this,
                        successMessage(requestGeneration),
                        Toast.LENGTH_SHORT
                ).show();
                openMyUploadsAndFinish();
            }

            @Override
            public void onError(Exception exception) {
                setLoading(false, requestGeneration);
                String chapterTextError = chapterTextBackendValidationMessage(exception);
                if (chapterTextError != null) {
                    showChapterTextValidationError(chapterTextError);
                    Toast.makeText(
                            ManageChapterActivity.this,
                            "Please update the chapter text and try again.",
                            Toast.LENGTH_LONG
                    ).show();
                    return;
                }
                if (exception instanceof DraftSavedGenerationRequestException) {
                    Toast.makeText(
                            ManageChapterActivity.this,
                            exception.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                    openMyUploadsAndFinish();
                    return;
                }
                Toast.makeText(
                        ManageChapterActivity.this,
                        AuthErrorFormatter.friendlyMessage(exception),
                        Toast.LENGTH_LONG
                ).show();
            }
        };

        if (requestGeneration) {
            ensureGenerationNotifications();
            if (isEditMode()) {
                repository.updateChapterDraftAndRequestGeneration(bookId, chapterId, input, callback);
            } else {
                repository.createChapterDraftAndRequestGeneration(bookId, input, callback);
            }
        } else if (isEditMode()) {
            repository.updateChapterDraft(bookId, chapterId, input, callback);
        } else {
            repository.createChapterDraft(bookId, input, callback);
        }
    }

    private void ensureGenerationNotifications() {
        if (notificationSetup != null) {
            notificationSetup.ensureReady();
        }
    }

    private void loadDraftForEdit() {
        setDraftLoading(true);
        repository.getChapterDraftForEdit(bookId, chapterId, new RepositoryCallback<EditableChapterDraft>() {
            @Override
            public void onSuccess(EditableChapterDraft draft) {
                setDraftLoading(false);
                bindDraft(draft);
            }

            @Override
            public void onError(Exception exception) {
                setDraftLoading(false);
                Toast.makeText(
                        ManageChapterActivity.this,
                        AuthErrorFormatter.friendlyMessage(exception),
                        Toast.LENGTH_LONG
                ).show();
                finish();
            }
        });
    }

    private void bindDraft(EditableChapterDraft draft) {
        if (draft == null) {
            return;
        }
        if (inputChapterTitle != null) {
            inputChapterTitle.setText(draft.getChapterTitle());
        }
        if (inputChapterText != null) {
            inputChapterText.setText(draft.getChapterText());
        }
        bindChapterTextCounter(null);
        selectVoice(draft.getVoiceOption());
    }

    private CreateChapterDraftInput readAndValidateInput() {
        String chapterTitle = textFrom(inputChapterTitle);
        String chapterText = textFrom(inputChapterText);

        if (chapterTitle.isEmpty()) {
            inputChapterTitle.setError("Chapter title is required");
            inputChapterTitle.requestFocus();
            return null;
        }
        if (chapterText.isEmpty()) {
            showChapterTextValidationError("Chapter text is required");
            return null;
        }
        ChapterTextCounterState chapterTextState = ChapterTextCounterState.from(chapterText);
        if (chapterTextState.getSeverity() == ChapterTextCounterState.Severity.ERROR) {
            showChapterTextValidationError(ChapterTextCounterState.maxWordsErrorMessage());
            return null;
        }
        bindChapterTextCounter(null);

        return new CreateChapterDraftInput(
                chapterTitle,
                chapterText,
                CreateChapterDraftInput.DEFAULT_LANGUAGE_CODE,
                selectedVoice
        );
    }

    private void setLoading(boolean loading, boolean requestGeneration) {
        setInputsEnabled(!loading);
        if (saveDraftButton != null) {
            saveDraftButton.setEnabled(!loading);
            saveDraftButton.setText(saveButtonText(loading, requestGeneration));
        }
        if (requestGenerationButton != null) {
            requestGenerationButton.setEnabled(!loading);
            requestGenerationButton.setText(requestButtonText(loading, requestGeneration));
        }
    }

    private void setDraftLoading(boolean loading) {
        loadingDraft = loading;
        setInputsEnabled(!loading);
        if (saveDraftButton != null) {
            saveDraftButton.setEnabled(!loading);
            saveDraftButton.setText(loading ? "Loading..." : saveButtonText(false, false));
        }
        if (requestGenerationButton != null) {
            requestGenerationButton.setEnabled(!loading);
            requestGenerationButton.setText(loading ? "Loading..." : requestButtonText(false, false));
        }
    }

    private void setInputsEnabled(boolean enabled) {
        if (inputChapterTitle != null) {
            inputChapterTitle.setEnabled(enabled);
        }
        if (inputChapterText != null) {
            inputChapterText.setEnabled(enabled);
        }
        if (voicePatrick != null) {
            voicePatrick.setEnabled(enabled);
        }
        if (voiceRuth != null) {
            voiceRuth.setEnabled(enabled);
        }
    }

    private String successMessage(boolean requestGeneration) {
        if (requestGeneration) {
            return "Chapter generation request queued.";
        }
        return isEditMode() ? "Chapter draft updated." : "Chapter draft saved.";
    }

    private String saveButtonText(boolean loading, boolean requestGeneration) {
        if (loading && !requestGeneration) {
            return "Saving...";
        }
        return isEditMode() ? "Save Changes" : "Save Draft";
    }

    private String requestButtonText(boolean loading, boolean requestGeneration) {
        if (loading && requestGeneration) {
            return "Requesting...";
        }
        return isEditMode() ? "Save & Request Generation" : "Request Generation";
    }

    private void setupChapterTextCounter() {
        if (inputChapterText == null) {
            bindChapterTextCounter(null);
            return;
        }
        inputChapterText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                inputChapterText.setError(null);
                bindChapterTextCounter(null);
            }
        });
        bindChapterTextCounter(null);
    }

    private void bindChapterTextCounter(String fieldErrorMessage) {
        ChapterTextCounterState state = ChapterTextCounterState.from(textFrom(inputChapterText));
        if (chapterTextCounter != null) {
            chapterTextCounter.setText(state.getCounterText());
            chapterTextCounter.setTextColor(getColor(chapterTextCounterColor(state, fieldErrorMessage != null)));
        }

        if (fieldErrorMessage != null) {
            showChapterTextFeedback(fieldErrorMessage, R.color.error_text);
            return;
        }
        if (state.getSeverity() == ChapterTextCounterState.Severity.ERROR) {
            showChapterTextFeedback(state.getMessage(), R.color.error_text);
            return;
        }
        if (state.getSeverity() == ChapterTextCounterState.Severity.WARNING) {
            showChapterTextFeedback(state.getMessage(), R.color.warning_text);
            return;
        }
        hideChapterTextFeedback();
    }

    private int chapterTextCounterColor(ChapterTextCounterState state, boolean hasFieldError) {
        if (hasFieldError || state.getSeverity() == ChapterTextCounterState.Severity.ERROR) {
            return R.color.error_text;
        }
        if (state.getSeverity() == ChapterTextCounterState.Severity.WARNING) {
            return R.color.warning_text;
        }
        return R.color.text_muted;
    }

    private void showChapterTextValidationError(String message) {
        if (inputChapterText != null) {
            inputChapterText.setError(message);
            inputChapterText.requestFocus();
        }
        bindChapterTextCounter(message);
    }

    private void showChapterTextFeedback(String message, int colorRes) {
        if (chapterTextFeedback == null) {
            return;
        }
        chapterTextFeedback.setText(message);
        chapterTextFeedback.setTextColor(getColor(colorRes));
        chapterTextFeedback.setVisibility(View.VISIBLE);
    }

    private void hideChapterTextFeedback() {
        if (chapterTextFeedback == null) {
            return;
        }
        chapterTextFeedback.setText("");
        chapterTextFeedback.setVisibility(View.GONE);
    }

    private String chapterTextBackendValidationMessage(Exception exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof BackendApiException) {
                String message = ((BackendApiException) current).getValidationMessageForField("chapterText");
                if (message != null) {
                    return message;
                }
            }
            current = current.getCause();
        }
        return null;
    }

    private String textFrom(EditText input) {
        return input == null || input.getText() == null ? "" : input.getText().toString().trim();
    }

    @Override
    protected void onStop() {
        repository.stop();
        super.onStop();
    }

    private boolean isEditMode() {
        return chapterId != null;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void openMyUploadsAndFinish() {
        Intent intent = new Intent(ManageChapterActivity.this, MyUploadsActivity.class);
        startActivity(intent);
        finish();
    }
}
