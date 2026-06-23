package com.example.fonos_group13;

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;

import com.example.fonos_group13.audio.AudioPreferences;
import com.example.fonos_group13.audio.AudioSourceResolver;
import com.example.fonos_group13.audio.PlaybackService;
import com.example.fonos_group13.data.BookRepository;
import com.example.fonos_group13.data.BookAccessMode;
import com.example.fonos_group13.data.DownloadedAudioRepository;
import com.example.fonos_group13.data.ProgressRepository;
import com.example.fonos_group13.data.RepositoryCallback;
import com.example.fonos_group13.model.Book;
import com.example.fonos_group13.model.BookChapter;
import com.example.fonos_group13.model.UserProgress;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

@OptIn(markerClass = UnstableApi.class)
public class ActivityReader extends AppCompatActivity {
    public static final String EXTRA_BOOK_ID = "book_id";
    public static final String EXTRA_CHAPTER_ID = "chapter_id";
    public static final String EXTRA_AUTO_PLAY = "auto_play";
    public static final String EXTRA_CREATOR_PREVIEW = "creator_review_preview";
    public static final String METADATA_BOOK_ID = "metadata_book_id";
    public static final String METADATA_CHAPTER_ID = "metadata_chapter_id";
    public static final String METADATA_CREATOR_PREVIEW = "metadata_creator_review_preview";

    private final List<BookChapter> currentChapters = new ArrayList<>();
    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            updateProgressUi();
            progressHandler.postDelayed(this, 500);
        }
    };
    private final Player.Listener playerListener = new Player.Listener() {
        @Override
        public void onPlaybackStateChanged(int playbackState) {
            if (playbackState == Player.STATE_READY) {
                updateProgressUi();
            } else if (playbackState == Player.STATE_ENDED) {
                saveProgress();
            }
            updatePlayButton();
            updateChapterNavigationButtons();
        }

        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            if (!isPlaying) {
                saveProgress();
            }
            updatePlayButton();
            updateChapterNavigationButtons();
        }

        @Override
        public void onMediaItemTransition(MediaItem mediaItem, int reason) {
            syncCurrentChapter(mediaItem);
            updateProgressUi();
            updatePlayButton();
            updateChapterNavigationButtons();
        }
    };

    private BookRepository bookRepository;
    private ProgressRepository progressRepository;
    private DownloadedAudioRepository downloadedAudioRepository;
    private AudioSourceResolver audioSourceResolver;
    private ListenableFuture<MediaController> controllerFuture;
    private MediaController mediaController;
    private Book currentBook;
    private BookChapter currentChapter;
    private String requestedBookId;
    private String requestedChapterId;
    private boolean requestedAutoPlay;
    private boolean requestedCreatorPreview;
    private boolean creatorPreviewActive;
    private boolean userSeeking;
    private boolean downloadingAudio;
    private boolean playerEnabled;
    private int speedIndex;

    private TextView tvChapter;
    private TextView tvBookTitle;
    private TextView tvBookContent;
    private TextView tvCurrentTime;
    private TextView tvDuration;
    private TextView tvPlaybackSpeed;
    private SeekBar seekBar;
    private FloatingActionButton btnPlayPause;
    private ImageView btnSkipBack;
    private ImageView btnSkipForward;
    private ImageView btnDownloadAudio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reader);
        bookRepository = new BookRepository(this);
        progressRepository = new ProgressRepository(this);
        downloadedAudioRepository = new DownloadedAudioRepository(this);
        audioSourceResolver = new AudioSourceResolver(this);
        speedIndex = AudioPreferences.getDefaultSpeedIndex(this);

        bindViews();
        setupInsets();
        setupControls();
        setPlayerEnabled(false);

        handleBookIntent(getIntent());
    }

    @Override
    protected void onStart() {
        super.onStart();
        connectController();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleBookIntent(intent);
    }

    private void bindViews() {
        tvChapter = findViewById(R.id.tvChapter);
        tvBookTitle = findViewById(R.id.tvBookTitle);
        tvBookContent = findViewById(R.id.tvBookContent);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvDuration = findViewById(R.id.tvDuration);
        tvPlaybackSpeed = findViewById(R.id.tvPlaybackSpeed);
        seekBar = findViewById(R.id.seekBar);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnSkipBack = findViewById(R.id.btnSkipBack);
        btnSkipForward = findViewById(R.id.btnSkipForward);
        btnDownloadAudio = findViewById(R.id.ivDownloadAudio);
    }

    private void setupInsets() {
        View topBar = findViewById(R.id.topBar);
        if (topBar != null) {
            int initialPaddingTop = topBar.getPaddingTop();
            int initialMinHeight = topBar.getMinimumHeight();
            ViewCompat.setOnApplyWindowInsetsListener(topBar, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), initialPaddingTop + systemBars.top, v.getPaddingRight(), v.getPaddingBottom());
                v.setMinimumHeight(initialMinHeight + systemBars.top);
                ViewGroup.LayoutParams layoutParams = v.getLayoutParams();
                if (layoutParams != null && layoutParams.height > 0) {
                    layoutParams.height += systemBars.top;
                    v.setLayoutParams(layoutParams);
                }
                return insets;
            });
        }

        View playerControls = findViewById(R.id.playerControlsContainer);
        if (playerControls != null) {
            int initialPaddingBottom = playerControls.getPaddingBottom();
            ViewCompat.setOnApplyWindowInsetsListener(playerControls, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), initialPaddingBottom + systemBars.bottom);
                return insets;
            });
        }

        View exit = findViewById(R.id.ivExit);
        if (exit != null) {
            exit.setOnClickListener(v -> finish());
        }
    }

    private void setupControls() {
        if (btnPlayPause != null) {
            btnPlayPause.setOnClickListener(v -> togglePlayback());
        }
        if (btnSkipBack != null) {
            btnSkipBack.setOnClickListener(v -> skipToPreviousChapter());
        }
        if (btnSkipForward != null) {
            btnSkipForward.setOnClickListener(v -> skipToNextChapter());
        }
        if (tvPlaybackSpeed != null) {
            tvPlaybackSpeed.setText(formatSpeed());
            tvPlaybackSpeed.setOnClickListener(v -> cyclePlaybackSpeed());
        }
        if (btnDownloadAudio != null) {
            btnDownloadAudio.setOnClickListener(v -> downloadCurrentBook());
        }
        if (seekBar != null) {
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && tvCurrentTime != null) {
                        tvCurrentTime.setText(formatTime(progress));
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    userSeeking = true;
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    userSeeking = false;
                    if (mediaController != null) {
                        mediaController.seekTo(seekBar.getProgress());
                    }
                }
            });
        }
    }

    private void connectController() {
        if (controllerFuture != null || mediaController != null) {
            return;
        }

        SessionToken sessionToken = new SessionToken(this, new ComponentName(this, PlaybackService.class));
        controllerFuture = new MediaController.Builder(this, sessionToken).buildAsync();
        ListenableFuture<MediaController> future = controllerFuture;
        future.addListener(() -> {
            if (controllerFuture != future) {
                return;
            }
            try {
                mediaController = future.get();
                mediaController.addListener(playerListener);
                mediaController.setPlaybackParameters(new PlaybackParameters(AudioPreferences.getSpeedAt(speedIndex)));
                setPlayerEnabled(currentBook != null && currentChapter != null && hasAudio(currentBook, currentChapter));
                if (currentBook != null && currentChapter != null) {
                    prepareAudio(currentBook, currentChapter);
                } else {
                    updateProgressUi();
                    updatePlayButton();
                }
            } catch (CancellationException ignored) {
            } catch (ExecutionException exception) {
                Toast.makeText(this, "Could not connect to playback service.", Toast.LENGTH_LONG).show();
                setPlayerEnabled(false);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                Toast.makeText(this, "Playback service connection was interrupted.", Toast.LENGTH_LONG).show();
                setPlayerEnabled(false);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void handleBookIntent(Intent intent) {
        String bookId = intent == null ? null : trimToNull(intent.getStringExtra(EXTRA_BOOK_ID));
        if (bookId == null) {
            Toast.makeText(this, "Missing book id.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        String chapterId = intent == null ? null : trimToNull(intent.getStringExtra(EXTRA_CHAPTER_ID));
        boolean autoPlay = intent != null && intent.getBooleanExtra(EXTRA_AUTO_PLAY, false);
        boolean creatorPreview = intent != null && intent.getBooleanExtra(EXTRA_CREATOR_PREVIEW, false);

        if (currentBook != null
                && currentChapter != null
                && bookId.equals(currentBook.getId())
                && (chapterId == null || chapterId.equals(currentChapter.getId()))
                && (currentBook.isPublished() || creatorPreview == creatorPreviewActive)) {
            requestedBookId = bookId;
            requestedChapterId = currentChapter.getId();
            requestedAutoPlay = autoPlay;
            requestedCreatorPreview = creatorPreview;
            refreshCurrentChapter();
            return;
        }

        if (currentBook == null
                && bookId.equals(requestedBookId)
                && ((chapterId == null && requestedChapterId == null)
                || (chapterId != null && chapterId.equals(requestedChapterId)))
                && creatorPreview == requestedCreatorPreview) {
            return;
        }

        requestedBookId = bookId;
        requestedChapterId = chapterId;
        requestedAutoPlay = autoPlay;
        requestedCreatorPreview = creatorPreview;
        loadBookAndChapter(bookId, chapterId);
    }

    private void refreshCurrentChapter() {
        updateDownloadButton();
        if (mediaController == null) {
            connectController();
            return;
        }
        prepareAudio(currentBook, currentChapter, false, C.TIME_UNSET, requestedAutoPlay);
        requestedAutoPlay = false;
    }

    private void loadBookAndChapter(String bookId, String chapterId) {
        String loadingBookId = bookId;
        boolean loadingCreatorPreview = requestedCreatorPreview;
        BookAccessMode accessMode = loadingCreatorPreview
                ? BookAccessMode.CREATOR_REVIEW_PREVIEW
                : BookAccessMode.PUBLISHED_ONLY;
        bookRepository.getBook(bookId, accessMode, new RepositoryCallback<Book>() {
            @Override
            public void onSuccess(Book book) {
                if (!loadingBookId.equals(requestedBookId)
                        || loadingCreatorPreview != requestedCreatorPreview) {
                    return;
                }
                creatorPreviewActive = loadingCreatorPreview && !book.isPublished();
                loadChapter(book, chapterId);
            }

            @Override
            public void onError(Exception exception) {
                if (!loadingBookId.equals(requestedBookId)
                        || loadingCreatorPreview != requestedCreatorPreview) {
                    return;
                }
                Toast.makeText(ActivityReader.this, "This audiobook is unavailable.", Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void loadChapter(Book book, String chapterId) {
        String loadingBookId = book.getId();
        BookAccessMode accessMode = creatorPreviewActive
                ? BookAccessMode.CREATOR_REVIEW_PREVIEW
                : BookAccessMode.PUBLISHED_ONLY;
        bookRepository.getChapters(book.getId(), accessMode, new RepositoryCallback<List<BookChapter>>() {
            @Override
            public void onSuccess(List<BookChapter> chapters) {
                if (!loadingBookId.equals(requestedBookId)) {
                    return;
                }
                currentChapters.clear();
                if (chapters != null) {
                    currentChapters.addAll(chapters);
                }
                BookChapter selectedChapter = selectChapter(currentChapters, chapterId);
                if (selectedChapter == null) {
                    Toast.makeText(ActivityReader.this, "Could not load this chapter.", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
                bindBook(book, selectedChapter);
                prepareAudio(book, selectedChapter, false, C.TIME_UNSET, requestedAutoPlay);
                requestedAutoPlay = false;
            }

            @Override
            public void onError(Exception exception) {
                if (!loadingBookId.equals(requestedBookId)) {
                    return;
                }
                currentChapters.clear();
                Toast.makeText(ActivityReader.this, "Could not load chapters from Firestore.", Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private BookChapter selectChapter(List<BookChapter> chapters, String chapterId) {
        if (chapters == null || chapters.isEmpty()) {
            return null;
        }
        if (chapterId != null) {
            for (BookChapter chapter : chapters) {
                if (chapterId.equals(chapter.getId())) {
                    return chapter;
                }
            }
        }
        return chapters.get(0);
    }

    private void bindBook(Book book, BookChapter chapter) {
        currentBook = book;
        currentChapter = chapter;
        requestedChapterId = chapter.getId();
        if (tvChapter != null) {
            tvChapter.setText(chapter.getTitle());
        }
        if (tvBookTitle != null) {
            tvBookTitle.setText(book.getTitle());
        }
        if (tvBookContent != null) {
            tvBookContent.setText(chapter.getContentSample());
        }
        if (tvCurrentTime != null) {
            tvCurrentTime.setText(formatTime(0));
        }
        if (tvDuration != null) {
            tvDuration.setText(formatTime(chapter.getDurationSec() * 1000L));
        }
        if (seekBar != null) {
            seekBar.setProgress(0);
            seekBar.setMax(safeDuration(chapter.getDurationSec() * 1000L));
        }
        updateDownloadButton();
    }

    private void prepareAudio(Book book, BookChapter chapter) {
        prepareAudio(book, chapter, false, C.TIME_UNSET, false);
    }

    private void prepareAudio(Book book, BookChapter chapter, boolean forceReload, long startPositionMs, boolean playWhenReady) {
        if (book == null || chapter == null) {
            return;
        }
        if (mediaController == null) {
            setPlayerEnabled(false);
            return;
        }

        Uri audioUri = audioSourceResolver.resolve(book, chapter);
        if (audioUri == null) {
            setPlayerEnabled(false);
            Toast.makeText(this, missingAudioMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        setPlayerEnabled(true);
        mediaController.setPlaybackParameters(new PlaybackParameters(AudioPreferences.getSpeedAt(speedIndex)));
        List<MediaItem> playlist = buildMediaPlaylist(book, chapter, audioUri);
        int selectedIndex = findMediaItemIndex(playlist, book.getId(), chapter.getId());
        if (playlist.isEmpty() || selectedIndex == C.INDEX_UNSET) {
            setPlayerEnabled(false);
            Toast.makeText(this, missingAudioMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        MediaItem currentItem = mediaController.getCurrentMediaItem();
        boolean sameChapter = isCurrentMediaItem(currentItem, book.getId(), chapter.getId());
        boolean sameSource = sameChapter && isSameResolvedSource(currentItem, audioUri);
        boolean samePlaylist = isSamePlaylist(playlist);
        if (sameChapter && (!sameSource || !samePlaylist) && startPositionMs == C.TIME_UNSET) {
            startPositionMs = mediaController.getCurrentPosition();
            playWhenReady = mediaController.isPlaying();
        }
        if (sameChapter && sameSource && samePlaylist && !forceReload) {
            if (playWhenReady) {
                mediaController.play();
            }
            updateProgressUi();
            updatePlayButton();
            updateChapterNavigationButtons();
            progressHandler.removeCallbacks(progressRunnable);
            progressHandler.post(progressRunnable);
            return;
        }
        if (startPositionMs == C.TIME_UNSET) {
            prepareAudioFromSavedProgress(book, chapter, forceReload, playWhenReady);
            return;
        }

        progressHandler.removeCallbacks(progressRunnable);
        long startPosition = Math.max(startPositionMs, 0);
        mediaController.setMediaItems(playlist, selectedIndex, startPosition);
        mediaController.prepare();
        if (playWhenReady) {
            mediaController.play();
        }
        updateProgressUi();
        updatePlayButton();
        updateChapterNavigationButtons();
        progressHandler.post(progressRunnable);
    }

    private void prepareAudioFromSavedProgress(
            Book book,
            BookChapter chapter,
            boolean forceReload,
            boolean playWhenReady
    ) {
        String bookId = book.getId();
        String chapterId = chapter.getId();
        progressRepository.getProgress(bookId, chapterId, new RepositoryCallback<UserProgress>() {
            @Override
            public void onSuccess(UserProgress progress) {
                if (!isSelectedChapter(bookId, chapterId)) {
                    return;
                }
                long positionMs = progress == null ? 0 : Math.max(progress.getPositionMs(), 0);
                prepareAudio(book, chapter, forceReload, positionMs, playWhenReady);
            }

            @Override
            public void onError(Exception exception) {
                if (!isSelectedChapter(bookId, chapterId)) {
                    return;
                }
                prepareAudio(book, chapter, forceReload, 0, playWhenReady);
            }
        });
    }

    private List<MediaItem> buildMediaPlaylist(Book book, BookChapter selectedChapter, Uri selectedAudioUri) {
        List<MediaItem> playlist = new ArrayList<>();
        boolean selectedChapterAdded = false;
        for (BookChapter chapter : currentChapters) {
            Uri chapterUri = selectedChapter.getId().equals(chapter.getId())
                    ? selectedAudioUri
                    : audioSourceResolver.resolve(book, chapter);
            if (chapterUri == null) {
                continue;
            }
            if (selectedChapter.getId().equals(chapter.getId())) {
                selectedChapterAdded = true;
            }
            playlist.add(buildMediaItem(book, chapter, chapterUri));
        }
        if (!selectedChapterAdded) {
            playlist.add(buildMediaItem(book, selectedChapter, selectedAudioUri));
        }
        return playlist;
    }

    private int findMediaItemIndex(List<MediaItem> playlist, String bookId, String chapterId) {
        for (int i = 0; i < playlist.size(); i++) {
            if (isCurrentMediaItem(playlist.get(i), bookId, chapterId)) {
                return i;
            }
        }
        return C.INDEX_UNSET;
    }

    private boolean isSamePlaylist(List<MediaItem> playlist) {
        if (mediaController == null || mediaController.getMediaItemCount() != playlist.size()) {
            return false;
        }
        for (int i = 0; i < playlist.size(); i++) {
            if (!isSameMediaItem(mediaController.getMediaItemAt(i), playlist.get(i))) {
                return false;
            }
        }
        return true;
    }

    private boolean isSameMediaItem(MediaItem left, MediaItem right) {
        if (left == null || right == null) {
            return left == right;
        }
        String leftId = left.mediaId == null ? "" : left.mediaId;
        String rightId = right.mediaId == null ? "" : right.mediaId;
        if (!leftId.equals(rightId)) {
            return false;
        }
        if (left.localConfiguration == null || right.localConfiguration == null) {
            return left.localConfiguration == right.localConfiguration;
        }
        Uri leftUri = left.localConfiguration.uri;
        Uri rightUri = right.localConfiguration.uri;
        return leftUri == null ? rightUri == null : leftUri.equals(rightUri);
    }

    private MediaItem buildMediaItem(Book book, BookChapter chapter, Uri audioUri) {
        Bundle extras = new Bundle();
        extras.putString(METADATA_BOOK_ID, book.getId());
        extras.putString(METADATA_CHAPTER_ID, chapter.getId());
        extras.putBoolean(METADATA_CREATOR_PREVIEW, creatorPreviewActive);

        MediaMetadata.Builder metadataBuilder = new MediaMetadata.Builder()
                .setTitle(chapter.getTitle())
                .setArtist(book.getTitle())
                .setSubtitle(book.getAuthor())
                .setExtras(extras);
        if (chapter.getDurationSec() > 0) {
            metadataBuilder.setDurationMs(chapter.getDurationSec() * 1000L);
        }
        String coverUrl = trimToNull(book.getCoverUrl());
        if (coverUrl != null) {
            metadataBuilder.setArtworkUri(Uri.parse(coverUrl));
        }

        return new MediaItem.Builder()
                .setMediaId(ProgressRepository.progressDocumentId(book.getId(), chapter.getId()))
                .setUri(audioUri)
                .setMediaMetadata(metadataBuilder.build())
                .build();
    }

    private void downloadCurrentBook() {
        if (currentBook == null || currentChapter == null || downloadingAudio) {
            return;
        }
        if (downloadedAudioRepository.isDownloaded(currentBook.getId(), currentChapter.getId())) {
            Toast.makeText(this, "Chapter audio is already downloaded.", Toast.LENGTH_SHORT).show();
            updateDownloadButton();
            return;
        }
        if (!currentChapter.hasAudio()) {
            Toast.makeText(this, "This chapter does not have an audioUrl to download.", Toast.LENGTH_LONG).show();
            updateDownloadButton();
            return;
        }

        boolean wasPlaying = mediaController != null && mediaController.isPlaying();
        long currentPositionMs = mediaController == null ? 0 : mediaController.getCurrentPosition();
        downloadingAudio = true;
        updateDownloadButton();
        Toast.makeText(this, "Downloading chapter audio...", Toast.LENGTH_SHORT).show();
        String downloadingBookId = currentBook.getId();
        String downloadingChapterId = currentChapter.getId();
        downloadedAudioRepository.download(currentBook, currentChapter, new RepositoryCallback<File>() {
            @Override
            public void onSuccess(File data) {
                runOnUiThread(() -> {
                    downloadingAudio = false;
                    updateDownloadButton();
                    Toast.makeText(ActivityReader.this, "Chapter audio downloaded.", Toast.LENGTH_SHORT).show();
                    if (currentBook != null
                            && currentChapter != null
                            && currentBook.getId().equals(downloadingBookId)
                            && currentChapter.getId().equals(downloadingChapterId)) {
                        prepareAudio(currentBook, currentChapter, true, currentPositionMs, wasPlaying);
                    }
                });
            }

            @Override
            public void onError(Exception exception) {
                runOnUiThread(() -> {
                    downloadingAudio = false;
                    updateDownloadButton();
                    String message = exception == null || exception.getMessage() == null
                            ? "Could not download audio."
                            : exception.getMessage();
                    Toast.makeText(ActivityReader.this, message, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void updateDownloadButton() {
        if (btnDownloadAudio == null) {
            return;
        }
        boolean downloaded = currentBook != null
                && currentChapter != null
                && downloadedAudioRepository.isDownloaded(currentBook.getId(), currentChapter.getId());
        boolean hasRemoteAudio = currentChapter != null && currentChapter.hasAudio();

        btnDownloadAudio.setImageResource(downloaded ? R.drawable.ic_download_done : R.drawable.ic_download);
        btnDownloadAudio.setEnabled(!downloadingAudio && !downloaded && hasRemoteAudio);
        btnDownloadAudio.setAlpha(btnDownloadAudio.isEnabled() || downloaded ? 1f : 0.35f);
    }

    private String missingAudioMessage() {
        return "Missing audio: add a Firestore audioUrl for this chapter.";
    }

    private void syncCurrentChapter(MediaItem mediaItem) {
        if (mediaItem == null || mediaItem.mediaMetadata.extras == null || currentBook == null) {
            return;
        }
        String bookId = trimToNull(mediaItem.mediaMetadata.extras.getString(METADATA_BOOK_ID));
        String chapterId = trimToNull(mediaItem.mediaMetadata.extras.getString(METADATA_CHAPTER_ID));
        if (bookId == null || chapterId == null || !bookId.equals(currentBook.getId())) {
            return;
        }
        BookChapter chapter = findChapterById(chapterId);
        if (chapter == null) {
            return;
        }
        boolean changedChapter = currentChapter == null || !chapterId.equals(currentChapter.getId());
        if (changedChapter) {
            bindBook(currentBook, chapter);
        } else {
            requestedChapterId = chapter.getId();
            updateDownloadButton();
        }
    }

    private BookChapter findChapterById(String chapterId) {
        if (chapterId == null) {
            return null;
        }
        for (BookChapter chapter : currentChapters) {
            if (chapterId.equals(chapter.getId())) {
                return chapter;
            }
        }
        return null;
    }

    private void togglePlayback() {
        if (mediaController == null) {
            return;
        }
        if (mediaController.isPlaying()) {
            mediaController.pause();
            saveProgress();
        } else {
            mediaController.play();
        }
        updatePlayButton();
    }

    private void skipToPreviousChapter() {
        if (mediaController == null || !mediaController.hasPreviousMediaItem()) {
            return;
        }
        saveProgress();
        mediaController.seekToPreviousMediaItem();
        updateProgressUi();
        updateChapterNavigationButtons();
    }

    private void skipToNextChapter() {
        if (mediaController == null || !mediaController.hasNextMediaItem()) {
            return;
        }
        saveProgress();
        mediaController.seekToNextMediaItem();
        updateProgressUi();
        updateChapterNavigationButtons();
    }

    private void cyclePlaybackSpeed() {
        speedIndex = (speedIndex + 1) % AudioPreferences.speedCount();
        if (tvPlaybackSpeed != null) {
            tvPlaybackSpeed.setText(formatSpeed());
        }
        if (mediaController != null) {
            mediaController.setPlaybackParameters(new PlaybackParameters(AudioPreferences.getSpeedAt(speedIndex)));
        }
    }

    private String formatSpeed() {
        return AudioPreferences.formatSpeed(speedIndex);
    }

    private void updateProgressUi() {
        if (currentBook == null) {
            return;
        }

        long durationMs = getDurationMs();
        long positionMs = mediaController == null ? 0 : mediaController.getCurrentPosition();

        if (tvCurrentTime != null) {
            tvCurrentTime.setText(formatTime(positionMs));
        }
        if (tvDuration != null) {
            tvDuration.setText(formatTime(durationMs));
        }
        if (seekBar != null) {
            seekBar.setMax(safeDuration(durationMs));
            if (!userSeeking) {
                seekBar.setProgress(safeDuration(positionMs));
            }
        }
    }

    private long getDurationMs() {
        long controllerDuration = mediaController == null ? C.TIME_UNSET : mediaController.getDuration();
        if (controllerDuration != C.TIME_UNSET && controllerDuration > 0) {
            return controllerDuration;
        }
        if (mediaController != null && mediaController.getMediaMetadata().durationMs != null) {
            return mediaController.getMediaMetadata().durationMs;
        }
        return currentChapter == null ? 0 : currentChapter.getDurationSec() * 1000L;
    }

    private int safeDuration(long durationMs) {
        if (durationMs <= 0) {
            return 0;
        }
        return durationMs > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) durationMs;
    }

    private String formatTime(long millis) {
        long totalSeconds = Math.max(millis, 0) / 1000L;
        long seconds = totalSeconds % 60L;
        long minutes = (totalSeconds / 60L) % 60L;
        long hours = totalSeconds / 3600L;
        if (hours > 0) {
            return String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format(Locale.US, "%02d:%02d", minutes, seconds);
    }

    private void updatePlayButton() {
        if (btnPlayPause == null) {
            return;
        }
        btnPlayPause.setImageResource(mediaController != null && mediaController.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);
    }

    private void setPlayerEnabled(boolean enabled) {
        playerEnabled = enabled;
        if (btnPlayPause != null) {
            btnPlayPause.setEnabled(enabled);
            btnPlayPause.setAlpha(enabled ? 1f : 0.45f);
        }
        if (seekBar != null) {
            seekBar.setEnabled(enabled);
            seekBar.setAlpha(enabled ? 1f : 0.45f);
        }
        updatePlayButton();
        updateChapterNavigationButtons();
    }

    private void updateChapterNavigationButtons() {
        boolean canGoPrevious = playerEnabled && mediaController != null && mediaController.hasPreviousMediaItem();
        boolean canGoNext = playerEnabled && mediaController != null && mediaController.hasNextMediaItem();
        setNavigationButtonEnabled(btnSkipBack, canGoPrevious);
        setNavigationButtonEnabled(btnSkipForward, canGoNext);
    }

    private void setNavigationButtonEnabled(ImageView button, boolean enabled) {
        if (button == null) {
            return;
        }
        button.setEnabled(enabled);
        button.setAlpha(enabled ? 1f : 0.45f);
    }

    private void saveProgress() {
        if (mediaController == null) {
            return;
        }
        String bookId = currentBook == null ? null : currentBook.getId();
        String chapterId = currentChapter == null ? null : currentChapter.getId();
        MediaItem currentItem = mediaController.getCurrentMediaItem();
        if (currentItem != null && currentItem.mediaMetadata.extras != null) {
            String itemBookId = trimToNull(currentItem.mediaMetadata.extras.getString(METADATA_BOOK_ID));
            String itemChapterId = trimToNull(currentItem.mediaMetadata.extras.getString(METADATA_CHAPTER_ID));
            if (itemBookId != null) {
                bookId = itemBookId;
            }
            if (itemChapterId != null) {
                chapterId = itemChapterId;
            }
        }
        if (bookId == null || bookId.trim().isEmpty() || chapterId == null || chapterId.trim().isEmpty()) {
            return;
        }
        progressRepository.saveProgress(bookId, chapterId, mediaController.getCurrentPosition(), getDurationMs());
    }

    private boolean isSelectedChapter(String bookId, String chapterId) {
        return currentBook != null
                && currentChapter != null
                && bookId != null
                && chapterId != null
                && bookId.equals(currentBook.getId())
                && chapterId.equals(currentChapter.getId());
    }

    private boolean isCurrentMediaItem(MediaItem currentItem, String bookId, String chapterId) {
        if (currentItem == null || currentItem.mediaMetadata.extras == null) {
            return false;
        }
        return bookId.equals(currentItem.mediaMetadata.extras.getString(METADATA_BOOK_ID))
                && chapterId.equals(currentItem.mediaMetadata.extras.getString(METADATA_CHAPTER_ID));
    }

    private boolean isSameResolvedSource(MediaItem currentItem, Uri resolvedUri) {
        return currentItem != null
                && currentItem.localConfiguration != null
                && currentItem.localConfiguration.uri != null
                && currentItem.localConfiguration.uri.equals(resolvedUri);
    }

    private boolean hasAudio(Book book, BookChapter chapter) {
        return audioSourceResolver.resolve(book, chapter) != null;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void releaseController() {
        progressHandler.removeCallbacks(progressRunnable);
        if (mediaController != null) {
            saveProgress();
            mediaController.removeListener(playerListener);
            mediaController = null;
        }
        if (controllerFuture != null) {
            MediaController.releaseFuture(controllerFuture);
            controllerFuture = null;
        }
        updatePlayButton();
    }

    @Override
    protected void onStop() {
        releaseController();
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateDownloadButton();
    }

    @Override
    protected void onDestroy() {
        progressHandler.removeCallbacks(progressRunnable);
        super.onDestroy();
    }
}
