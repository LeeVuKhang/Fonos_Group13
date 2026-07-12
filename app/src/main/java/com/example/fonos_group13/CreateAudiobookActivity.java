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
import com.example.fonos_group13.controller.creator.CreateAudiobookController;
import com.example.fonos_group13.data.creator.DraftSavedGenerationRequestException;
import com.example.fonos_group13.model.CreateAudiobookDraftInput;
import com.example.fonos_group13.model.CreatorVoiceOption;
import com.example.fonos_group13.model.EditableAudiobookDraft;
import com.example.fonos_group13.notifications.GenerationNotificationSetup;
import com.example.fonos_group13.ui.ChapterTextCounterState;
import com.google.android.material.button.MaterialButton;

public class CreateAudiobookActivity extends AppCompatActivity {
    public static final String EXTRA_EDIT_BOOK_ID = "com.example.fonos_group13.EXTRA_EDIT_BOOK_ID";

    private CreateAudiobookController repository;
    private GenerationNotificationSetup notificationSetup;
    private TextView headerTitle;
    private EditText inputTitle;
    private EditText inputAuthor;
    private EditText inputCoverUrl;
    private EditText inputChapterText;
    private TextView chapterTextCounter;
    private TextView chapterTextFeedback;
    private TextView voicePatrick;
    private TextView voiceRuth;
    private MaterialButton saveDraftButton;
    private MaterialButton requestGenerationButton;
    private CreatorVoiceOption selectedVoice = CreatorVoiceOption.PATRICK;
    private String editingBookId;
    private boolean loadingDraft;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_audiobook);

        repository = new CreateAudiobookController(
                FonosApplication.container(this).creatorCommandRepository()
        );
        notificationSetup = new GenerationNotificationSetup(this);
        editingBookId = trimToNull(getIntent().getStringExtra(EXTRA_EDIT_BOOK_ID));
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
        headerTitle = findViewById(R.id.create_audiobook_header_title);
        inputTitle = findViewById(R.id.input_audiobook_title);
        inputAuthor = findViewById(R.id.input_audiobook_author);
        inputCoverUrl = findViewById(R.id.input_cover_url);
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
            headerTitle.setText(isEditMode() ? "Edit Draft" : "Create Audiobook");
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
        chip.setSelected(active);
    }

    private void saveDraft(boolean requestGeneration) {
        if (loadingDraft) {
            return;
        }
        CreateAudiobookDraftInput input = readAndValidateInput();
        if (input == null) {
            return;
        }

        setLoading(true, requestGeneration);
        RepositoryCallback<String> callback = new RepositoryCallback<String>() {
            @Override
            public void onSuccess(String bookId) {
                setLoading(false, requestGeneration);
                Toast.makeText(
                        CreateAudiobookActivity.this,
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
                            CreateAudiobookActivity.this,
                            "Please update the chapter text and try again.",
                            Toast.LENGTH_LONG
                    ).show();
                    return;
                }
                if (exception instanceof DraftSavedGenerationRequestException) {
                    Toast.makeText(
                            CreateAudiobookActivity.this,
                            exception.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                    openMyUploadsAndFinish();
                    return;
                }
                Toast.makeText(
                        CreateAudiobookActivity.this,
                        AuthErrorFormatter.friendlyMessage(exception),
                        Toast.LENGTH_LONG
                ).show();
            }
        };

        if (requestGeneration) {
            ensureGenerationNotifications();
            if (isEditMode()) {
                repository.updateDraftAndRequestGeneration(editingBookId, input, callback);
            } else {
                repository.createDraftAndRequestGeneration(input, callback);
            }
        } else if (isEditMode()) {
            repository.updateDraft(editingBookId, input, callback);
        } else {
            repository.createDraft(input, callback);
        }
    }

    private void ensureGenerationNotifications() {
        if (notificationSetup != null) {
            notificationSetup.ensureReady();
        }
    }

    private void loadDraftForEdit() {
        setDraftLoading(true);
        repository.getDraftForEdit(editingBookId, new RepositoryCallback<EditableAudiobookDraft>() {
            @Override
            public void onSuccess(EditableAudiobookDraft draft) {
                setDraftLoading(false);
                bindDraft(draft);
            }

            @Override
            public void onError(Exception exception) {
                setDraftLoading(false);
                Toast.makeText(
                        CreateAudiobookActivity.this,
                        AuthErrorFormatter.friendlyMessage(exception),
                        Toast.LENGTH_LONG
                ).show();
                finish();
            }
        });
    }

    private void bindDraft(EditableAudiobookDraft draft) {
        if (draft == null) {
            return;
        }
        if (inputTitle != null) {
            inputTitle.setText(draft.getTitle());
        }
        if (inputAuthor != null) {
            inputAuthor.setText(draft.getAuthor());
        }
        if (inputCoverUrl != null) {
            inputCoverUrl.setText(draft.getCoverUrl() == null ? "" : draft.getCoverUrl());
        }
        if (inputChapterText != null) {
            inputChapterText.setText(draft.getChapterText());
        }
        bindChapterTextCounter(null);
        selectVoice(draft.getVoiceOption());
    }

    private CreateAudiobookDraftInput readAndValidateInput() {
        String title = textFrom(inputTitle);
        String author = textFrom(inputAuthor);
        String coverUrl = textFrom(inputCoverUrl);
        String chapterText = textFrom(inputChapterText);

        if (title.isEmpty()) {
            inputTitle.setError("Title is required");
            inputTitle.requestFocus();
            return null;
        }
        if (author.isEmpty()) {
            inputAuthor.setError("Author is required");
            inputAuthor.requestFocus();
            return null;
        }
        if (chapterText.isEmpty()) {
            showChapterTextValidationError("Chapter text is required");
            return null;
        }
        if (title.length() > CreateAudiobookDraftInput.MAX_TITLE_CHARS) {
            inputTitle.setError("Title must be 120 characters or fewer");
            inputTitle.requestFocus();
            return null;
        }
        if (author.length() > CreateAudiobookDraftInput.MAX_AUTHOR_CHARS) {
            inputAuthor.setError("Author must be 120 characters or fewer");
            inputAuthor.requestFocus();
            return null;
        }
        ChapterTextCounterState chapterTextState = ChapterTextCounterState.from(chapterText);
        if (chapterTextState.getSeverity() == ChapterTextCounterState.Severity.ERROR) {
            showChapterTextValidationError(ChapterTextCounterState.maxWordsErrorMessage());
            return null;
        }
        bindChapterTextCounter(null);

        return new CreateAudiobookDraftInput(
                title,
                author,
                coverUrl,
                CreateAudiobookDraftInput.DEFAULT_CHAPTER_TITLE,
                chapterText,
                CreateAudiobookDraftInput.DEFAULT_LANGUAGE_CODE,
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
        if (voicePatrick != null) {
            voicePatrick.setEnabled(!loading);
        }
        if (voiceRuth != null) {
            voiceRuth.setEnabled(!loading);
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
        if (inputTitle != null) {
            inputTitle.setEnabled(enabled);
        }
        if (inputAuthor != null) {
            inputAuthor.setEnabled(enabled);
        }
        if (inputCoverUrl != null) {
            inputCoverUrl.setEnabled(enabled);
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
            return "Generation request queued.";
        }
        return isEditMode() ? "Audiobook draft updated." : "Audiobook draft saved.";
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
        return editingBookId != null;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void openMyUploadsAndFinish() {
        Intent intent = new Intent(CreateAudiobookActivity.this, MyUploadsActivity.class);
        startActivity(intent);
        finish();
    }
}
