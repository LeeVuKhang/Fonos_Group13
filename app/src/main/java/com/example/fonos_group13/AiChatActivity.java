package com.example.fonos_group13;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.fonos_group13.controller.ai.AiChatController;
import com.example.fonos_group13.data.core.RepositoryCallback;
import com.example.fonos_group13.data.creator.BackendApiException;
import com.example.fonos_group13.model.AiChatMessage;
import com.example.fonos_group13.model.AiCitation;
import com.example.fonos_group13.model.AiResponse;
import com.example.fonos_group13.model.AiScope;
import com.example.fonos_group13.model.Book;
import com.example.fonos_group13.model.BookChapter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AiChatActivity extends AppCompatActivity {
    public static final String EXTRA_BOOK_ID = "ai_book_id";
    public static final String EXTRA_BOOK_TITLE = "ai_book_title";
    public static final String EXTRA_INITIAL_CHAPTER_ID = "ai_initial_chapter_id";
    public static final String EXTRA_CHAPTER_IDS = "ai_chapter_ids";
    public static final String EXTRA_CHAPTER_TITLES = "ai_chapter_titles";

    private static final String STATE_MESSAGES = "ai_messages";
    private static final String STATE_SCOPE_INDEX = "ai_scope_index";
    private static final String STATE_SPOILER_CONFIRMED = "ai_spoiler_confirmed";
    private static final String STATE_PENDING = "ai_pending";
    private static final String STATE_WAS_LOADING = "ai_was_loading";

    private final ArrayList<AiChatMessage> messages = new ArrayList<>();
    private final ArrayList<String> chapterIds = new ArrayList<>();
    private final ArrayList<String> chapterTitles = new ArrayList<>();

    private AiChatController controller;
    private String bookId;
    private boolean spoilerConfirmed;
    private boolean loading;
    private PendingRequest retryRequest;

    private Spinner scopeSpinner;
    private androidx.core.widget.NestedScrollView messagesScroll;
    private LinearLayout messagesContainer;
    private EditText questionInput;
    private MaterialButton sendButton;
    private MaterialButton retryButton;
    private MaterialButton clearButton;
    private MaterialButton starterSummary;
    private MaterialButton starterThemes;
    private MaterialButton starterCharacters;
    private ProgressBar loadingView;
    private TextView statusMessage;

    public static Intent newIntent(Context context, Book book, List<BookChapter> chapters, String initialChapterId) {
        Intent intent = new Intent(context, AiChatActivity.class)
                .putExtra(EXTRA_BOOK_ID, book.getId())
                .putExtra(EXTRA_BOOK_TITLE, book.getTitle())
                .putExtra(EXTRA_INITIAL_CHAPTER_ID, initialChapterId);
        ArrayList<String> ids = new ArrayList<>();
        ArrayList<String> titles = new ArrayList<>();
        if (chapters != null) {
            for (BookChapter chapter : chapters) {
                if (chapter != null && chapter.isPublished()) {
                    ids.add(chapter.getId());
                    titles.add(chapter.getTitle());
                }
            }
        }
        intent.putStringArrayListExtra(EXTRA_CHAPTER_IDS, ids);
        intent.putStringArrayListExtra(EXTRA_CHAPTER_TITLES, titles);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ai_chat);

        controller = new AiChatController(FonosApplication.container(this).aiChatRepository());
        readIntent();
        bindViews();
        setupInsets();
        setupScope(savedInstanceState);
        restoreState(savedInstanceState);
        setupControls();
        renderMessages();
    }

    @Override protected void onStart() {
        super.onStart();
        controller.start();
    }

    @Override protected void onStop() {
        controller.stop();
        if (loading && !isFinishing()) {
            setLoading(false);
            showError(getString(R.string.ai_request_cancelled), true);
        }
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(STATE_MESSAGES, messages);
        outState.putInt(STATE_SCOPE_INDEX, scopeSpinner.getSelectedItemPosition());
        outState.putBoolean(STATE_SPOILER_CONFIRMED, spoilerConfirmed);
        outState.putBoolean(STATE_WAS_LOADING, loading);
        if (retryRequest != null) outState.putSerializable(STATE_PENDING, retryRequest);
    }

    private void readIntent() {
        Intent intent = getIntent();
        bookId = clean(intent.getStringExtra(EXTRA_BOOK_ID));
        if (bookId == null) {
            Toast.makeText(this, R.string.ai_error_not_ready, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        ArrayList<String> ids = intent.getStringArrayListExtra(EXTRA_CHAPTER_IDS);
        ArrayList<String> titles = intent.getStringArrayListExtra(EXTRA_CHAPTER_TITLES);
        if (ids != null && titles != null) {
            int size = Math.min(ids.size(), titles.size());
            for (int index = 0; index < size; index++) {
                String id = clean(ids.get(index));
                if (id != null) {
                    chapterIds.add(id);
                    chapterTitles.add(titles.get(index));
                }
            }
        }
    }

    private void bindViews() {
        scopeSpinner = findViewById(R.id.ai_scope_spinner);
        messagesScroll = findViewById(R.id.ai_messages_scroll);
        messagesContainer = findViewById(R.id.ai_messages_container);
        questionInput = findViewById(R.id.ai_question_input);
        sendButton = findViewById(R.id.ai_send);
        retryButton = findViewById(R.id.ai_retry);
        clearButton = findViewById(R.id.ai_chat_clear);
        starterSummary = findViewById(R.id.ai_starter_summary);
        starterThemes = findViewById(R.id.ai_starter_themes);
        starterCharacters = findViewById(R.id.ai_starter_characters);
        loadingView = findViewById(R.id.ai_loading);
        statusMessage = findViewById(R.id.ai_status_message);
        TextView title = findViewById(R.id.ai_chat_book_title);
        title.setText(getIntent().getStringExtra(EXTRA_BOOK_TITLE));
    }

    private void setupInsets() {
        View header = findViewById(R.id.ai_chat_header);
        int headerPaddingTop = header.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(header, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(view.getPaddingLeft(), headerPaddingTop + bars.top,
                    view.getPaddingRight(), view.getPaddingBottom());
            return insets;
        });
        View input = findViewById(R.id.ai_input_panel);
        int inputPaddingBottom = input.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(input, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(view.getPaddingLeft(), view.getPaddingTop(),
                    view.getPaddingRight(), inputPaddingBottom + bars.bottom);
            return insets;
        });
    }

    private void setupScope(Bundle savedInstanceState) {
        ArrayList<String> labels = new ArrayList<>();
        labels.add(getString(R.string.ai_scope_book));
        for (String title : chapterTitles) labels.add(getString(R.string.ai_scope_chapter, title));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        scopeSpinner.setAdapter(adapter);

        int selection = 0;
        if (savedInstanceState != null) {
            selection = savedInstanceState.getInt(STATE_SCOPE_INDEX, 0);
        } else {
            String initialChapter = clean(getIntent().getStringExtra(EXTRA_INITIAL_CHAPTER_ID));
            int chapterIndex = chapterIds.indexOf(initialChapter);
            if (chapterIndex >= 0) selection = chapterIndex + 1;
        }
        scopeSpinner.setSelection(Math.min(selection, labels.size() - 1));
    }

    @SuppressWarnings("unchecked")
    private void restoreState(Bundle state) {
        if (state == null) return;
        Serializable savedMessages = state.getSerializable(STATE_MESSAGES);
        if (savedMessages instanceof ArrayList) messages.addAll((ArrayList<AiChatMessage>) savedMessages);
        spoilerConfirmed = state.getBoolean(STATE_SPOILER_CONFIRMED, false);
        Serializable pending = state.getSerializable(STATE_PENDING);
        if (pending instanceof PendingRequest) retryRequest = (PendingRequest) pending;
        if (state.getBoolean(STATE_WAS_LOADING, false) && retryRequest != null) {
            showError(getString(R.string.ai_rotation_cancelled), true);
        }
    }

    private void setupControls() {
        ImageView back = findViewById(R.id.ai_chat_back);
        back.setOnClickListener(view -> finish());
        clearButton.setOnClickListener(view -> {
            messages.clear();
            retryRequest = null;
            hideStatus();
            renderMessages();
        });
        sendButton.setOnClickListener(view -> submitQuestion());
        questionInput.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                submitQuestion();
                return true;
            }
            return false;
        });
        starterSummary.setOnClickListener(view -> beginRequest("summary", null));
        starterThemes.setOnClickListener(view -> beginRequest("question", getString(R.string.ai_prompt_themes)));
        starterCharacters.setOnClickListener(view -> beginRequest("question", getString(R.string.ai_prompt_characters)));
        retryButton.setOnClickListener(view -> {
            if (retryRequest != null) dispatch(retryRequest);
        });
    }

    private void submitQuestion() {
        String question = clean(questionInput.getText().toString());
        if (question == null) {
            questionInput.setError(getString(R.string.ai_question_hint));
            return;
        }
        questionInput.setText("");
        beginRequest("question", question);
    }

    private void beginRequest(String mode, String question) {
        if (loading) return;
        AiScope scope = selectedScope();
        Runnable action = () -> {
            ArrayList<AiChatMessage> priorHistory = historySnapshot();
            String displayText = "summary".equals(mode) ? getString(R.string.ai_display_summary) : question;
            messages.add(new AiChatMessage(AiChatMessage.ROLE_USER, displayText));
            renderMessages();
            retryRequest = new PendingRequest(mode, question, scope, priorHistory);
            dispatch(retryRequest);
        };
        if (scope.isBook() && !spoilerConfirmed) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.ai_spoiler_title)
                    .setMessage(R.string.ai_spoiler_message)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.ai_spoiler_continue, (dialog, which) -> {
                        spoilerConfirmed = true;
                        action.run();
                    })
                    .show();
        } else {
            action.run();
        }
    }

    private void dispatch(PendingRequest request) {
        setLoading(true);
        hideStatus();
        String locale = "summary".equals(request.mode) ? appLocale() : "auto";
        controller.requestResponse(bookId, request.mode, request.scope, request.question, locale, request.history,
                new RepositoryCallback<AiResponse>() {
                    @Override public void onSuccess(AiResponse response) {
                        setLoading(false);
                        messages.add(new AiChatMessage(
                                AiChatMessage.ROLE_ASSISTANT,
                                response.getAnswer(),
                                response.getCitations()
                        ));
                        retryRequest = null;
                        renderMessages();
                    }

                    @Override public void onError(Exception exception) {
                        setLoading(false);
                        showError(errorMessage(exception), true);
                    }
                });
    }

    private AiScope selectedScope() {
        int position = scopeSpinner.getSelectedItemPosition();
        if (position <= 0 || position - 1 >= chapterIds.size()) return AiScope.book();
        return AiScope.chapter(chapterIds.get(position - 1));
    }

    private ArrayList<AiChatMessage> historySnapshot() {
        ArrayList<AiChatMessage> history = new ArrayList<>();
        int first = Math.max(0, messages.size() - 12);
        for (int index = first; index < messages.size(); index++) {
            AiChatMessage message = messages.get(index);
            history.add(new AiChatMessage(message.getRole(), message.getText()));
        }
        return history;
    }

    private void renderMessages() {
        messagesContainer.removeAllViews();
        if (messages.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.ai_empty_message);
            empty.setTextColor(ContextCompat.getColor(this, R.color.text_muted));
            empty.setTextSize(15);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(24), dp(56), dp(24), dp(24));
            messagesContainer.addView(empty, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            return;
        }
        for (AiChatMessage message : messages) addMessageCard(message);
        messagesScroll.post(() -> messagesScroll.fullScroll(View.FOCUS_DOWN));
    }

    private void addMessageCard(AiChatMessage message) {
        boolean user = AiChatMessage.ROLE_USER.equals(message.getRole());
        LinearLayout row = new LinearLayout(this);
        row.setGravity(user ? Gravity.END : Gravity.START);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.topMargin = dp(8);

        MaterialCardView card = new MaterialCardView(this);
        card.setCardBackgroundColor(ContextCompat.getColor(this, user ? R.color.accent_soft : R.color.surface));
        card.setRadius(dp(16));
        card.setCardElevation(dp(user ? 0 : 1));
        card.setStrokeColor(ContextCompat.getColor(this, R.color.stroke_soft));
        card.setStrokeWidth(user ? 0 : dp(1));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.88f);
        if (user) cardParams.leftMargin = dp(40); else cardParams.rightMargin = dp(24);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(12), dp(16), dp(12));

        TextView role = new TextView(this);
        role.setText(user ? R.string.ai_you : R.string.ai_assistant);
        role.setTextColor(ContextCompat.getColor(this, R.color.accent_text));
        role.setTextSize(12);
        role.setTypeface(role.getTypeface(), android.graphics.Typeface.BOLD);
        content.addView(role);

        TextView answer = new TextView(this);
        answer.setText(message.getText());
        answer.setTextColor(ContextCompat.getColor(this, R.color.text_dark));
        answer.setTextSize(16);
        answer.setTextIsSelectable(true);
        LinearLayout.LayoutParams answerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        answerParams.topMargin = dp(4);
        content.addView(answer, answerParams);

        if (!user) {
            for (AiCitation citation : message.getCitations()) addCitation(content, citation);
        }
        card.addView(content);
        row.addView(card, cardParams);
        messagesContainer.addView(row, rowParams);
    }

    private void addCitation(LinearLayout content, AiCitation citation) {
        TextView source = new TextView(this);
        String title = clean(citation.getChapterTitle());
        source.setText(getString(R.string.ai_citation_format,
                title == null ? getString(R.string.ai_sources) : title,
                citation.getExcerpt()));
        source.setTextColor(ContextCompat.getColor(this, R.color.text_muted));
        source.setTextSize(13);
        source.setBackgroundColor(ContextCompat.getColor(this, R.color.surface_soft));
        source.setPadding(dp(10), dp(8), dp(10), dp(8));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(8);
        content.addView(source, params);
    }

    private void setLoading(boolean value) {
        loading = value;
        loadingView.setVisibility(value ? View.VISIBLE : View.GONE);
        if (value) {
            statusMessage.setText(R.string.ai_loading);
            statusMessage.setTextColor(ContextCompat.getColor(this, R.color.text_muted));
            statusMessage.setVisibility(View.VISIBLE);
        }
        sendButton.setEnabled(!value);
        scopeSpinner.setEnabled(!value);
        starterSummary.setEnabled(!value);
        starterThemes.setEnabled(!value);
        starterCharacters.setEnabled(!value);
        clearButton.setEnabled(!value);
        retryButton.setVisibility(View.GONE);
    }

    private void showError(String message, boolean retry) {
        statusMessage.setText(message);
        statusMessage.setTextColor(ContextCompat.getColor(this, R.color.error_text));
        statusMessage.setVisibility(View.VISIBLE);
        retryButton.setVisibility(retry && retryRequest != null ? View.VISIBLE : View.GONE);
    }

    private void hideStatus() {
        if (!loading) {
            statusMessage.setVisibility(View.GONE);
            retryButton.setVisibility(View.GONE);
        }
    }

    private String errorMessage(Exception exception) {
        if (exception instanceof BackendApiException) {
            String code = ((BackendApiException) exception).getErrorCode();
            if ("ai_rate_limit_exceeded".equals(code)) return getString(R.string.ai_error_rate_limit);
            if ("ai_provider_unavailable".equals(code)) return getString(R.string.ai_error_provider);
            if ("ai_not_ready".equals(code)) return getString(R.string.ai_error_not_ready);
        }
        return getString(R.string.ai_error_generic);
    }

    private String appLocale() {
        Locale locale = getResources().getConfiguration().getLocales().get(0);
        return "vi".equalsIgnoreCase(locale.getLanguage()) ? "vi" : "en";
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static String clean(String value) {
        if (value == null) return null;
        String clean = value.trim();
        return clean.isEmpty() ? null : clean;
    }

    private static final class PendingRequest implements Serializable {
        final String mode;
        final String question;
        final AiScope scope;
        final ArrayList<AiChatMessage> history;

        PendingRequest(String mode, String question, AiScope scope, ArrayList<AiChatMessage> history) {
            this.mode = mode;
            this.question = question;
            this.scope = scope;
            this.history = history;
        }
    }
}
