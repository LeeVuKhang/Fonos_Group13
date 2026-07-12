package com.example.fonos_group13;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.fonos_group13.data.auth.AuthErrorFormatter;
import com.example.fonos_group13.controller.reader.BookDetailDataController;
import com.example.fonos_group13.data.catalog.BookAccessMode;
import com.example.fonos_group13.data.core.RepositoryCallback;
import com.example.fonos_group13.model.Book;
import com.example.fonos_group13.model.BookChapter;
import com.example.fonos_group13.model.AudiobookGenerationStatus;
import com.example.fonos_group13.model.UserProgress;
import com.example.fonos_group13.model.BookReview;
import com.example.fonos_group13.model.BookReviewPage;
import com.example.fonos_group13.model.ReviewMutationResult;
import com.example.fonos_group13.model.SaveMutationResult;
import com.example.fonos_group13.ui.BookCoverLoader;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import android.app.AlertDialog;

public class BookDetailActivity extends AppCompatActivity {
    public static final String EXTRA_BOOK_ID = "book_id";
    public static final String EXTRA_CREATOR_PREVIEW = "creator_review_preview";

    private BookDetailDataController dataController;
    private Book currentBook;
    private String requestedBookId;
    private String downloadingChapterId;
    private boolean currentBookSaved;
    private boolean saveBookLoading;
    private boolean publishLoading;
    private boolean creatorPreviewRequested;
    private boolean creatorPreviewActive;
    private boolean reviewsLoading;
    private boolean reviewSubmitting;
    private boolean reviewsLoadFailed;
    private double displayedRatingAverage;
    private int displayedRatingCount;
    private int displayedSaveCount;
    private String nextReviewCursor;
    private BookReview viewerReview;

    private final List<BookChapter> chapters = new ArrayList<>();
    private final Map<String, UserProgress> progressByChapterId = new HashMap<>();
    private final List<BookReview> reviews = new ArrayList<>();

    private ImageView coverView;
    private TextView titleView;
    private TextView authorView;
    private TextView summaryView;
    private TextView messageView;
    private FloatingActionButton playResumeButton;
    private ImageView saveBookButton;
    private ImageView downloadAllButton;
    private MaterialButton publishButton;
    private MaterialButton addChapterButton;
    private LinearLayout chaptersContainer;
    private TextView ratingSummaryView;
    private TextView saveCountView;
    private LinearLayout communitySection;
    private RatingBar reviewRatingInput;
    private EditText reviewCommentInput;
    private TextView reviewCharacterCount;
    private MaterialButton submitReviewButton;
    private MaterialButton deleteReviewButton;
    private MaterialButton loadMoreReviewsButton;
    private TextView reviewsMessageView;
    private LinearLayout reviewsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_book_detail);

        AppContainer container = FonosApplication.container(this);
        dataController = new BookDetailDataController(
                container.catalogRepository(),
                container.progressRepository(),
                container.audioDownloadRepository(),
                container.savedBooksRepository(),
                container.creatorCommandRepository(),
                container.authRepository(),
                container.bookCommunityRepository()
        );

        bindViews();
        setupInsets();
        setupControls();
    }

    @Override
    protected void onStart() {
        super.onStart();
        dataController.start();
        if (currentBook == null) {
            handleIntent(getIntent());
        }
    }

    @Override
    protected void onStop() {
        dataController.stop();
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshProgress();
        renderChapters();
    }

    private void bindViews() {
        coverView = findViewById(R.id.detail_cover);
        titleView = findViewById(R.id.detail_title);
        authorView = findViewById(R.id.detail_author);
        summaryView = findViewById(R.id.detail_summary);
        messageView = findViewById(R.id.detail_message);
        playResumeButton = findViewById(R.id.btn_play_resume);
        saveBookButton = findViewById(R.id.btn_save_book);
        downloadAllButton = findViewById(R.id.btn_download_all);
        publishButton = findViewById(R.id.btn_publish_audiobook);
        addChapterButton = findViewById(R.id.btn_add_chapter);
        chaptersContainer = findViewById(R.id.chapters_container);
        ratingSummaryView = findViewById(R.id.detail_rating_summary);
        saveCountView = findViewById(R.id.detail_save_count);
        communitySection = findViewById(R.id.community_section);
        reviewRatingInput = findViewById(R.id.review_rating_input);
        reviewCommentInput = findViewById(R.id.review_comment_input);
        reviewCharacterCount = findViewById(R.id.review_character_count);
        submitReviewButton = findViewById(R.id.btn_submit_review);
        deleteReviewButton = findViewById(R.id.btn_delete_review);
        loadMoreReviewsButton = findViewById(R.id.btn_load_more_reviews);
        reviewsMessageView = findViewById(R.id.reviews_message);
        reviewsContainer = findViewById(R.id.reviews_container);
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
        if (playResumeButton != null) {
            playResumeButton.setOnClickListener(v -> {
                BookChapter chapter = findResumeChapter();
                if (chapter != null) {
                    openReader(chapter, true);
                }
            });
        }
        if (saveBookButton != null) {
            saveBookButton.setOnClickListener(v -> toggleSavedBook());
            updateSaveBookButton();
        }
        if (downloadAllButton != null) {
            downloadAllButton.setEnabled(false);
            downloadAllButton.setAlpha(0.35f);
        }
        if (publishButton != null) {
            publishButton.setOnClickListener(v -> publishCurrentBook());
            updatePublishButton();
        }
        if (addChapterButton != null) {
            addChapterButton.setOnClickListener(v -> openAddChapter());
            updateAddChapterButton();
        }
        if (reviewCommentInput != null) {
            reviewCommentInput.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) { updateReviewCounter(); }
                @Override public void afterTextChanged(android.text.Editable s) { }
            });
        }
        if (submitReviewButton != null) submitReviewButton.setOnClickListener(v -> submitReview());
        if (deleteReviewButton != null) deleteReviewButton.setOnClickListener(v -> confirmDeleteReview());
        if (loadMoreReviewsButton != null) {
            loadMoreReviewsButton.setOnClickListener(v -> loadReviews(reviews.isEmpty()));
        }
    }

    private void handleIntent(Intent intent) {
        String bookId = intent == null ? null : trimToNull(intent.getStringExtra(EXTRA_BOOK_ID));
        if (bookId == null) {
            Toast.makeText(this, "Missing book id.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        creatorPreviewRequested = intent.getBooleanExtra(EXTRA_CREATOR_PREVIEW, false);
        creatorPreviewActive = false;
        publishLoading = false;
        requestedBookId = bookId;
        updateSaveBookButton();
        updatePublishButton();
        updateAddChapterButton();
        loadBook(bookId);
    }

    private void loadBook(String bookId) {
        showMessage("Loading chapters...");
        BookAccessMode accessMode = creatorPreviewRequested
                ? BookAccessMode.CREATOR_REVIEW_PREVIEW
                : BookAccessMode.PUBLISHED_ONLY;
        dataController.getBook(bookId, accessMode, new RepositoryCallback<Book>() {
            @Override
            public void onSuccess(Book book) {
                if (!bookId.equals(requestedBookId)) {
                    return;
                }
                currentBook = book;
                displayedRatingAverage = book.getRatingAverage();
                displayedRatingCount = book.getRatingCount();
                displayedSaveCount = book.getSaveCount();
                creatorPreviewActive = creatorPreviewRequested && isCurrentCreator(book);
                bindBookHeader(book);
                if (creatorPreviewActive) {
                    currentBookSaved = false;
                    saveBookLoading = false;
                    updateSaveBookButton();
                } else {
                    loadSavedState(bookId);
                }
                updatePublishButton();
                updateAddChapterButton();
                updateCommunitySummary();
                updateCommunityVisibility();
                loadChapters(bookId);
                if (book.isPublished()) loadReviews(true);
            }

            @Override
            public void onError(Exception exception) {
                if (!bookId.equals(requestedBookId)) {
                    return;
                }
                showMessage("This audiobook is unavailable.");
                currentBook = null;
                creatorPreviewActive = false;
                publishLoading = false;
                updatePublishButton();
                updateAddChapterButton();
                Toast.makeText(BookDetailActivity.this, "This audiobook is unavailable.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindBookHeader(Book book) {
        if (coverView != null) {
            BookCoverLoader.load(coverView, book);
        }
        if (titleView != null) {
            titleView.setText(book.getTitle());
        }
        if (authorView != null) {
            authorView.setText(book.getAuthor());
        }
    }

    private void loadSavedState(String bookId) {
        saveBookLoading = true;
        updateSaveBookButton();
        dataController.isSaved(bookId, new RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean saved) {
                if (!bookId.equals(requestedBookId)) {
                    return;
                }
                currentBookSaved = saved != null && saved;
                saveBookLoading = false;
                updateSaveBookButton();
            }

            @Override
            public void onError(Exception exception) {
                if (!bookId.equals(requestedBookId)) {
                    return;
                }
                currentBookSaved = false;
                saveBookLoading = false;
                updateSaveBookButton();
            }
        });
    }

    private void loadChapters(String bookId) {
        BookAccessMode accessMode = creatorPreviewActive
                ? BookAccessMode.CREATOR_REVIEW_PREVIEW
                : BookAccessMode.PUBLISHED_ONLY;
        dataController.getChapters(bookId, accessMode, new RepositoryCallback<List<BookChapter>>() {
            @Override
            public void onSuccess(List<BookChapter> data) {
                if (!bookId.equals(requestedBookId)) {
                    return;
                }
                setChapters(data);
            }

            @Override
            public void onError(Exception exception) {
                if (!bookId.equals(requestedBookId)) {
                    return;
                }
                chapters.clear();
                progressByChapterId.clear();
                updateSummary();
                renderChapters();
                showMessage("Could not load chapters.");
            }
        });
    }

    private void setChapters(List<BookChapter> data) {
        chapters.clear();
        progressByChapterId.clear();
        if (data != null) {
            chapters.addAll(data);
        }
        for (BookChapter chapter : chapters) {
            progressByChapterId.put(chapter.getId(), UserProgress.empty(chapter.getBookId(), chapter.getId()));
            loadProgress(chapter);
        }
        updateSummary();
        renderChapters();
        showMessage(chapters.isEmpty() ? "No chapters are available yet." : null);
        updatePlayButtonState();
    }

    private void refreshProgress() {
        for (BookChapter chapter : chapters) {
            loadProgress(chapter);
        }
    }

    private void loadProgress(BookChapter chapter) {
        dataController.getProgress(chapter.getBookId(), chapter.getId(), new RepositoryCallback<UserProgress>() {
            @Override
            public void onSuccess(UserProgress progress) {
                progressByChapterId.put(chapter.getId(), progress);
                updateSummary();
                renderChapters();
            }

            @Override
            public void onError(Exception exception) {
            }
        });
    }

    private void updateSummary() {
        if (summaryView == null) {
            return;
        }
        long totalDurationMs = 0;
        int completed = 0;
        for (BookChapter chapter : chapters) {
            totalDurationMs += Math.max(chapter.getDurationSec(), 0) * 1000L;
            UserProgress progress = progressByChapterId.get(chapter.getId());
            if (progress != null && progress.isCompleted()) {
                completed++;
            }
        }
        if (chapters.isEmpty()) {
            summaryView.setText("No chapters");
            return;
        }
        summaryView.setText(String.format(
                Locale.US,
                "Audio - %s - %d/%d finished",
                formatDuration(totalDurationMs),
                completed,
                chapters.size()
        ));
    }

    private void toggleSavedBook() {
        if (currentBook == null || saveBookLoading || creatorPreviewActive) {
            return;
        }

        String bookId = currentBook.getId();
        boolean targetSaved = !currentBookSaved;
        saveBookLoading = true;
        updateSaveBookButton();

        RepositoryCallback<SaveMutationResult> callback = new RepositoryCallback<SaveMutationResult>() {
            @Override
            public void onSuccess(SaveMutationResult data) {
                currentBookSaved = targetSaved;
                if (data != null) displayedSaveCount = data.getSaveCount();
                saveBookLoading = false;
                updateSaveBookButton();
                updateCommunitySummary();
                Toast.makeText(
                        BookDetailActivity.this,
                        targetSaved ? "Saved to library." : "Removed from library.",
                        Toast.LENGTH_SHORT
                ).show();
            }

            @Override
            public void onError(Exception exception) {
                saveBookLoading = false;
                updateSaveBookButton();
                String message = exception == null || exception.getMessage() == null
                        ? "Could not update your library."
                        : exception.getMessage();
                Toast.makeText(BookDetailActivity.this, message, Toast.LENGTH_LONG).show();
            }
        };

        dataController.setSavedWithResult(bookId, targetSaved, callback);
    }

    private void loadReviews(boolean reset) {
        if (currentBook == null || !currentBook.isPublished() || reviewsLoading) return;
        reviewsLoading = true;
        reviewsLoadFailed = false;
        if (reset) {
            reviews.clear();
            nextReviewCursor = null;
        }
        updateReviewUi();
        dataController.getReviews(currentBook.getId(), reset ? null : nextReviewCursor,
                new RepositoryCallback<BookReviewPage>() {
                    @Override public void onSuccess(BookReviewPage page) {
                        reviewsLoading = false;
                        reviewsLoadFailed = false;
                        if (page != null) {
                            reviews.addAll(page.getReviews());
                            if (reset) viewerReview = page.getViewerReview();
                            nextReviewCursor = page.getNextCursor();
                        }
                        bindViewerReview();
                        renderReviews();
                        updateReviewUi();
                    }

                    @Override public void onError(Exception exception) {
                        reviewsLoading = false;
                        reviewsLoadFailed = true;
                        showReviewsMessage(getString(R.string.community_load_error));
                        updateReviewUi();
                    }
                });
    }

    private void submitReview() {
        if (currentBook == null || reviewSubmitting || reviewRatingInput == null) return;
        int rating = Math.round(reviewRatingInput.getRating());
        if (rating < 1) {
            Toast.makeText(this, R.string.community_choose_rating, Toast.LENGTH_SHORT).show();
            return;
        }
        String comment = reviewCommentInput == null ? null : reviewCommentInput.getText().toString();
        reviewSubmitting = true;
        updateReviewUi();
        dataController.upsertReview(currentBook.getId(), rating, comment,
                new RepositoryCallback<ReviewMutationResult>() {
                    @Override public void onSuccess(ReviewMutationResult result) {
                        reviewSubmitting = false;
                        viewerReview = result.getReview();
                        displayedRatingAverage = result.getRatingAverage();
                        displayedRatingCount = result.getRatingCount();
                        updateCommunitySummary();
                        bindViewerReview();
                        loadReviews(true);
                        Toast.makeText(BookDetailActivity.this, R.string.community_review_saved, Toast.LENGTH_SHORT).show();
                    }

                    @Override public void onError(Exception exception) {
                        reviewSubmitting = false;
                        updateReviewUi();
                        Toast.makeText(BookDetailActivity.this, AuthErrorFormatter.friendlyMessage(exception), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void confirmDeleteReview() {
        if (viewerReview == null || reviewSubmitting) return;
        new AlertDialog.Builder(this)
                .setTitle(R.string.community_delete_title)
                .setMessage(R.string.community_delete_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.community_delete_action, (dialog, which) -> deleteReview())
                .show();
    }

    private void deleteReview() {
        reviewSubmitting = true;
        updateReviewUi();
        dataController.deleteReview(currentBook.getId(), new RepositoryCallback<ReviewMutationResult>() {
            @Override public void onSuccess(ReviewMutationResult result) {
                reviewSubmitting = false;
                viewerReview = null;
                displayedRatingAverage = result.getRatingAverage();
                displayedRatingCount = result.getRatingCount();
                updateCommunitySummary();
                bindViewerReview();
                loadReviews(true);
            }

            @Override public void onError(Exception exception) {
                reviewSubmitting = false;
                updateReviewUi();
                Toast.makeText(BookDetailActivity.this, AuthErrorFormatter.friendlyMessage(exception), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void bindViewerReview() {
        if (reviewRatingInput != null) reviewRatingInput.setRating(viewerReview == null ? 0 : viewerReview.getRating());
        if (reviewCommentInput != null) reviewCommentInput.setText(viewerReview == null || viewerReview.getComment() == null
                ? "" : viewerReview.getComment());
        updateReviewCounter();
    }

    private void updateReviewCounter() {
        if (reviewCharacterCount == null || reviewCommentInput == null) return;
        reviewCharacterCount.setText(getString(R.string.community_character_count, reviewCommentInput.length()));
    }

    private void updateCommunityVisibility() {
        if (communitySection == null) return;
        boolean visible = currentBook != null && currentBook.isPublished();
        communitySection.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void updateCommunitySummary() {
        if (ratingSummaryView != null) {
            ratingSummaryView.setText(displayedRatingCount <= 0
                    ? getString(R.string.community_no_ratings)
                    : getString(R.string.community_rating_summary, displayedRatingAverage, displayedRatingCount));
        }
        if (saveCountView != null) saveCountView.setText(getString(R.string.community_save_count, displayedSaveCount));
    }

    private void updateReviewUi() {
        boolean selfCreator = currentBook != null && dataController.isCurrentCreator(currentBook);
        if (reviewRatingInput != null) reviewRatingInput.setEnabled(!reviewSubmitting && !selfCreator);
        if (reviewCommentInput != null) reviewCommentInput.setEnabled(!reviewSubmitting && !selfCreator);
        if (submitReviewButton != null) {
            submitReviewButton.setVisibility(selfCreator ? View.GONE : View.VISIBLE);
            submitReviewButton.setEnabled(!reviewSubmitting);
            submitReviewButton.setText(viewerReview == null ? R.string.community_submit_review : R.string.community_update_review);
        }
        if (deleteReviewButton != null) {
            deleteReviewButton.setVisibility(!selfCreator && viewerReview != null ? View.VISIBLE : View.GONE);
            deleteReviewButton.setEnabled(!reviewSubmitting);
        }
        if (loadMoreReviewsButton != null) {
            loadMoreReviewsButton.setVisibility(nextReviewCursor != null || reviewsLoadFailed ? View.VISIBLE : View.GONE);
            loadMoreReviewsButton.setEnabled(!reviewsLoading);
        }
        if (reviewsLoading && reviews.isEmpty()) showReviewsMessage(getString(R.string.community_loading_reviews));
    }

    private void renderReviews() {
        if (reviewsContainer == null) return;
        reviewsContainer.removeAllViews();
        for (BookReview review : reviews) reviewsContainer.addView(createReviewCard(review));
        if (reviews.isEmpty() && !reviewsLoading) showReviewsMessage(getString(R.string.community_no_comments));
        else if (!reviewsLoading) showReviewsMessage(null);
    }

    private View createReviewCard(BookReview review) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackgroundResource(R.drawable.bg_card_white);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(params);
        TextView name = new TextView(this);
        name.setText(review.getReviewerDisplayName() + "  " + stars(review.getRating()));
        name.setTextColor(getColor(R.color.text_dark));
        name.setTypeface(null, Typeface.BOLD);
        card.addView(name);
        TextView date = new TextView(this);
        date.setText(reviewDate(review));
        date.setTextColor(getColor(R.color.text_muted));
        date.setTextSize(12);
        card.addView(date);
        TextView comment = new TextView(this);
        comment.setText(review.getComment());
        comment.setTextColor(getColor(R.color.text_main));
        comment.setTextSize(14);
        comment.setPadding(0, dp(8), 0, 0);
        card.addView(comment);
        return card;
    }

    private String stars(int rating) {
        StringBuilder value = new StringBuilder();
        for (int i = 0; i < 5; i++) value.append(i < rating ? '★' : '☆');
        return value.toString();
    }

    private String reviewDate(BookReview review) {
        String value = review.getCreatedAt();
        String date = value != null && value.length() >= 10 ? value.substring(0, 10) : "";
        return review.isEdited() ? getString(R.string.community_edited_date, date) : date;
    }

    private void showReviewsMessage(String message) {
        if (reviewsMessageView == null) return;
        reviewsMessageView.setText(message == null ? "" : message);
        reviewsMessageView.setVisibility(message == null ? View.GONE : View.VISIBLE);
    }

    private void publishCurrentBook() {
        if (!canPublishCurrentBook() || publishLoading) {
            return;
        }

        String bookId = currentBook.getId();
        publishLoading = true;
        updatePublishButton();
        dataController.publish(bookId, new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                publishLoading = false;
                creatorPreviewRequested = false;
                creatorPreviewActive = false;
                Toast.makeText(BookDetailActivity.this, "Audiobook published.", Toast.LENGTH_SHORT).show();
                updatePublishButton();
                updateAddChapterButton();
                loadBook(bookId);
            }

            @Override
            public void onError(Exception exception) {
                publishLoading = false;
                updatePublishButton();
                updateAddChapterButton();
                Toast.makeText(
                        BookDetailActivity.this,
                        AuthErrorFormatter.friendlyMessage(exception),
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    private void updateSaveBookButton() {
        if (saveBookButton == null) {
            return;
        }
        boolean hideForPreview = creatorPreviewRequested
                && (currentBook == null || creatorPreviewActive);
        saveBookButton.setVisibility(hideForPreview ? View.GONE : View.VISIBLE);
        if (hideForPreview) {
            return;
        }
        boolean enabled = currentBook != null && !saveBookLoading;
        saveBookButton.setEnabled(enabled);
        saveBookButton.setAlpha(saveBookLoading ? 0.65f : (currentBookSaved ? 1f : 0.45f));
        saveBookButton.setColorFilter(ContextCompat.getColor(
                this,
                currentBookSaved ? R.color.accent_green : R.color.accent
        ));
        saveBookButton.setContentDescription(currentBookSaved ? "Remove audiobook from library" : "Save audiobook");
    }

    private void updatePublishButton() {
        if (publishButton == null) {
            return;
        }
        boolean visible = canPublishCurrentBook();
        publishButton.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (!visible) {
            return;
        }
        publishButton.setEnabled(!publishLoading);
        publishButton.setAlpha(publishLoading ? 0.65f : 1f);
        publishButton.setText(publishLoading ? "Publishing..." : "Publish Audiobook");
    }

    private void updateAddChapterButton() {
        if (addChapterButton == null) {
            return;
        }
        boolean visible = creatorPreviewActive && currentBook != null;
        addChapterButton.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (!visible) {
            return;
        }
        addChapterButton.setEnabled(!publishLoading);
        addChapterButton.setAlpha(publishLoading ? 0.65f : 1f);
    }

    private boolean canPublishCurrentBook() {
        return creatorPreviewActive
                && currentBook != null
                && currentBook.getGenerationStatus() == AudiobookGenerationStatus.READY_FOR_REVIEW;
    }

    private boolean isCurrentCreator(Book book) {
        if (book == null) {
            return false;
        }
        return dataController.isCurrentCreator(book);
    }

    private void openAddChapter() {
        if (currentBook == null) {
            return;
        }
        Intent intent = new Intent(this, ManageChapterActivity.class);
        intent.putExtra(ManageChapterActivity.EXTRA_BOOK_ID, currentBook.getId());
        startActivity(intent);
    }

    private void updatePlayButtonState() {
        if (playResumeButton == null) {
            return;
        }
        boolean enabled = !chapters.isEmpty();
        playResumeButton.setEnabled(enabled);
        playResumeButton.setAlpha(enabled ? 1f : 0.45f);
    }

    private void renderChapters() {
        if (chaptersContainer == null) {
            return;
        }
        chaptersContainer.removeAllViews();
        for (int i = 0; i < chapters.size(); i++) {
            chaptersContainer.addView(createChapterRow(chapters.get(i), i + 1));
        }
    }

    private View createChapterRow(BookChapter chapter, int number) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setBackgroundResource(R.drawable.bg_card_white);
        row.setClickable(true);
        row.setFocusable(true);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        row.setOnClickListener(v -> openReader(chapter, false));

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        rowParams.setMargins(0, 0, 0, dp(12));
        row.setLayoutParams(rowParams);

        LinearLayout contentRow = new LinearLayout(this);
        contentRow.setGravity(Gravity.CENTER_VERTICAL);
        contentRow.setOrientation(LinearLayout.HORIZONTAL);

        TextView chapterNumber = new TextView(this);
        chapterNumber.setText(String.valueOf(number));
        chapterNumber.setTextColor(getColor(R.color.text_muted));
        chapterNumber.setTextSize(14);
        chapterNumber.setGravity(Gravity.CENTER);
        contentRow.addView(chapterNumber, new LinearLayout.LayoutParams(dp(28), ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout textContainer = new LinearLayout(this);
        textContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textParams.setMargins(dp(10), 0, dp(8), 0);

        TextView title = new TextView(this);
        title.setText(chapter.getTitle());
        title.setTextColor(getColor(R.color.text_dark));
        title.setTextSize(16);
        title.setTypeface(null, Typeface.BOLD);
        title.setMaxLines(2);

        TextView subtitle = new TextView(this);
        subtitle.setText(chapterSubtitle(chapter));
        subtitle.setTextColor(getColor(R.color.text_muted));
        subtitle.setTextSize(13);
        subtitle.setPadding(0, dp(4), 0, 0);

        textContainer.addView(title);
        textContainer.addView(subtitle);
        contentRow.addView(textContainer, textParams);

        ImageView download = new ImageView(this);
        boolean downloaded = dataController.isDownloaded(chapter.getBookId(), chapter.getId());
        boolean downloading = chapter.getId().equals(downloadingChapterId);
        download.setImageResource(downloaded ? R.drawable.ic_download_done : R.drawable.ic_download);
        download.setColorFilter(ContextCompat.getColor(this, R.color.accent));
        download.setPadding(dp(10), dp(10), dp(10), dp(10));
        download.setContentDescription(
                downloaded ? chapter.getTitle() + " downloaded" : "Download " + chapter.getTitle()
        );
        download.setFocusable(true);
        download.setEnabled(!downloaded && !downloading && chapter.hasAudio());
        download.setAlpha(download.isEnabled() || downloaded ? 1f : 0.35f);
        download.setOnClickListener(v -> downloadChapter(chapter));
        contentRow.addView(download, new LinearLayout.LayoutParams(dp(48), dp(48)));

        row.addView(contentRow);
        ProgressBar progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgress(chapterPercent(chapter));
        progressBar.setProgressDrawable(ContextCompat.getDrawable(this, R.drawable.layer_progress));
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(4)
        );
        progressParams.setMargins(dp(38), dp(10), 0, 0);
        row.addView(progressBar, progressParams);
        return row;
    }

    private String chapterSubtitle(BookChapter chapter) {
        UserProgress progress = progressByChapterId.get(chapter.getId());
        String duration = formatDuration(chapterDurationMs(chapter, progress));
        int percent = chapterPercent(chapter);
        boolean downloaded = dataController.isDownloaded(chapter.getBookId(), chapter.getId());
        String downloadLabel = downloaded ? "Downloaded" : "Streaming";
        return percent + "% complete - " + duration + " - " + downloadLabel;
    }

    private int chapterPercent(BookChapter chapter) {
        UserProgress progress = progressByChapterId.get(chapter.getId());
        if (progress == null) {
            return 0;
        }
        if (progress.isCompleted()) {
            return 100;
        }
        long durationMs = chapterDurationMs(chapter, progress);
        if (durationMs <= 0) {
            return 0;
        }
        return Math.min(100, Math.round(Math.max(progress.getPositionMs(), 0) * 100f / durationMs));
    }

    private long chapterDurationMs(BookChapter chapter, UserProgress progress) {
        if (progress != null && progress.getDurationMs() > 0) {
            return progress.getDurationMs();
        }
        return Math.max(chapter.getDurationSec(), 0) * 1000L;
    }

    private void downloadChapter(BookChapter chapter) {
        if (currentBook == null || chapter == null || downloadingChapterId != null) {
            return;
        }
        if (dataController.isDownloaded(chapter.getBookId(), chapter.getId())) {
            Toast.makeText(this, "Chapter is already downloaded.", Toast.LENGTH_SHORT).show();
            renderChapters();
            return;
        }
        if (!chapter.hasAudio()) {
            Toast.makeText(this, "This chapter does not have an audioUrl to download.", Toast.LENGTH_LONG).show();
            renderChapters();
            return;
        }

        downloadingChapterId = chapter.getId();
        renderChapters();
        Toast.makeText(this, "Downloading chapter...", Toast.LENGTH_SHORT).show();
        dataController.download(currentBook, chapter, new RepositoryCallback<File>() {
            @Override
            public void onSuccess(File data) {
                runOnUiThread(() -> {
                    downloadingChapterId = null;
                    renderChapters();
                    Toast.makeText(BookDetailActivity.this, "Chapter downloaded.", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(Exception exception) {
                runOnUiThread(() -> {
                    downloadingChapterId = null;
                    renderChapters();
                    String message = exception == null || exception.getMessage() == null
                            ? "Could not download chapter."
                            : exception.getMessage();
                    Toast.makeText(BookDetailActivity.this, message, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private BookChapter findResumeChapter() {
        for (BookChapter chapter : chapters) {
            UserProgress progress = progressByChapterId.get(chapter.getId());
            if (progress != null && progress.getPositionMs() > 0 && !progress.isCompleted()) {
                return chapter;
            }
        }
        for (BookChapter chapter : chapters) {
            UserProgress progress = progressByChapterId.get(chapter.getId());
            if (progress == null || !progress.isCompleted()) {
                return chapter;
            }
        }
        return chapters.isEmpty() ? null : chapters.get(0);
    }

    private void openReader(BookChapter chapter, boolean autoPlay) {
        if (currentBook == null || chapter == null) {
            return;
        }
        Intent intent = new Intent(this, ActivityReader.class);
        intent.putExtra(ActivityReader.EXTRA_BOOK_ID, currentBook.getId());
        intent.putExtra(ActivityReader.EXTRA_CHAPTER_ID, chapter.getId());
        intent.putExtra(ActivityReader.EXTRA_AUTO_PLAY, autoPlay);
        intent.putExtra(ActivityReader.EXTRA_CREATOR_PREVIEW, creatorPreviewActive);
        startActivity(intent);
    }

    private void showMessage(String message) {
        if (messageView == null) {
            return;
        }
        if (message == null || message.trim().isEmpty()) {
            messageView.setVisibility(View.GONE);
            messageView.setText("");
        } else {
            messageView.setVisibility(View.VISIBLE);
            messageView.setText(message);
        }
    }

    private String formatDuration(long millis) {
        long totalMinutes = Math.max(millis, 0) / 60000L;
        long hours = totalMinutes / 60L;
        long minutes = totalMinutes % 60L;
        if (hours > 0) {
            return String.format(Locale.US, "%dh %02dm", hours, minutes);
        }
        return String.format(Locale.US, "%dm", minutes);
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
