package com.example.fonos_group13;

import android.content.Intent;
import android.graphics.Typeface;
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

import com.example.fonos_group13.data.AuthRepository;
import com.example.fonos_group13.data.CreatorAudiobookRepository;
import com.example.fonos_group13.data.DraftSavedGenerationRequestException;
import com.example.fonos_group13.data.RepositoryCallback;
import com.example.fonos_group13.model.CreateAudiobookDraftInput;
import com.example.fonos_group13.model.CreatorVoiceOption;
import com.google.android.material.button.MaterialButton;

public class CreateAudiobookActivity extends AppCompatActivity {
    private CreatorAudiobookRepository repository;
    private EditText inputTitle;
    private EditText inputAuthor;
    private EditText inputCoverUrl;
    private EditText inputChapterText;
    private TextView voicePatrick;
    private TextView voiceRuth;
    private MaterialButton saveDraftButton;
    private MaterialButton requestGenerationButton;
    private CreatorVoiceOption selectedVoice = CreatorVoiceOption.PATRICK;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_audiobook);

        repository = new CreatorAudiobookRepository(this);
        bindViews();
        setupInsets();
        setupControls();
        selectVoice(CreatorVoiceOption.PATRICK);
    }

    private void bindViews() {
        inputTitle = findViewById(R.id.input_audiobook_title);
        inputAuthor = findViewById(R.id.input_audiobook_author);
        inputCoverUrl = findViewById(R.id.input_cover_url);
        inputChapterText = findViewById(R.id.input_chapter_text);
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
                        requestGeneration ? "Generation request queued." : "Audiobook draft saved.",
                        Toast.LENGTH_SHORT
                ).show();
                openMyUploadsAndFinish();
            }

            @Override
            public void onError(Exception exception) {
                setLoading(false, requestGeneration);
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
                        AuthRepository.friendlyError(exception),
                        Toast.LENGTH_LONG
                ).show();
            }
        };

        if (requestGeneration) {
            repository.createDraftAndRequestGeneration(input, callback);
        } else {
            repository.createDraft(input, callback);
        }
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
            inputChapterText.setError("Chapter text is required");
            inputChapterText.requestFocus();
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
        if (CreateAudiobookDraftInput.countWords(chapterText) > CreateAudiobookDraftInput.MAX_CHAPTER_TEXT_WORDS) {
            inputChapterText.setError("Chapter text must be 3500 words or fewer");
            inputChapterText.requestFocus();
            return null;
        }

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
        if (saveDraftButton != null) {
            saveDraftButton.setEnabled(!loading);
            saveDraftButton.setText(loading && !requestGeneration ? "Saving..." : "Save Draft");
        }
        if (requestGenerationButton != null) {
            requestGenerationButton.setEnabled(!loading);
            requestGenerationButton.setText(loading && requestGeneration ? "Requesting..." : "Request Generation");
        }
        if (voicePatrick != null) {
            voicePatrick.setEnabled(!loading);
        }
        if (voiceRuth != null) {
            voiceRuth.setEnabled(!loading);
        }
    }

    private String textFrom(EditText input) {
        return input == null || input.getText() == null ? "" : input.getText().toString().trim();
    }

    private void openMyUploadsAndFinish() {
        Intent intent = new Intent(CreateAudiobookActivity.this, MyUploadsActivity.class);
        startActivity(intent);
        finish();
    }
}
