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

import com.example.fonos_group13.audio.AudioSourceResolver;
import com.example.fonos_group13.audio.PlaybackService;
import com.example.fonos_group13.data.BookRepository;
import com.example.fonos_group13.data.DownloadedAudioRepository;
import com.example.fonos_group13.data.ProgressRepository;
import com.example.fonos_group13.data.RepositoryCallback;
import com.example.fonos_group13.model.Book;
import com.example.fonos_group13.model.UserProgress;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.Locale;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

@OptIn(markerClass = UnstableApi.class)
public class ActivityReader extends AppCompatActivity {
    public static final String EXTRA_BOOK_ID = "book_id";

    private static final long SKIP_MS = 15000L;
    private final float[] playbackSpeeds = {1.0f, 1.2f, 1.5f, 2.0f};
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
        }

        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            if (!isPlaying) {
                saveProgress();
            }
            updatePlayButton();
        }

        @Override
        public void onMediaItemTransition(MediaItem mediaItem, int reason) {
            updateProgressUi();
            updatePlayButton();
        }
    };

    private BookRepository bookRepository;
    private ProgressRepository progressRepository;
    private DownloadedAudioRepository downloadedAudioRepository;
    private AudioSourceResolver audioSourceResolver;
    private ListenableFuture<MediaController> controllerFuture;
    private MediaController mediaController;
    private Book currentBook;
    private String requestedBookId;
    private boolean userSeeking;
    private boolean downloadingAudio;
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
            btnSkipBack.setOnClickListener(v -> seekBy(-SKIP_MS));
        }
        if (btnSkipForward != null) {
            btnSkipForward.setOnClickListener(v -> seekBy(SKIP_MS));
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
                mediaController.setPlaybackParameters(new PlaybackParameters(playbackSpeeds[speedIndex]));
                setPlayerEnabled(currentBook != null && hasAudio(currentBook));
                if (currentBook != null) {
                    prepareAudio(currentBook);
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

        if (currentBook != null && bookId.equals(currentBook.getId())) {
            requestedBookId = bookId;
            refreshCurrentBook();
            return;
        }

        if (currentBook == null && bookId.equals(requestedBookId)) {
            return;
        }

        requestedBookId = bookId;
        loadBook(bookId);
    }

    private void refreshCurrentBook() {
        updateDownloadButton();
        if (mediaController == null) {
            connectController();
            return;
        }
        prepareAudio(currentBook);
    }

    private void loadBook(String bookId) {
        String loadingBookId = bookId;
        bookRepository.getBook(bookId, new RepositoryCallback<Book>() {
            @Override
            public void onSuccess(Book book) {
                if (!loadingBookId.equals(requestedBookId)) {
                    return;
                }
                bindBook(book);
                prepareAudio(book);
            }

            @Override
            public void onError(Exception exception) {
                if (!loadingBookId.equals(requestedBookId)) {
                    return;
                }
                Toast.makeText(ActivityReader.this, "Could not load this book from Firestore.", Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void bindBook(Book book) {
        currentBook = book;
        if (tvChapter != null) {
            tvChapter.setText(book.getChapterTitle());
        }
        if (tvBookTitle != null) {
            tvBookTitle.setText(book.getTitle());
        }
        if (tvBookContent != null) {
            tvBookContent.setText(book.getContentSample());
        }
        if (tvCurrentTime != null) {
            tvCurrentTime.setText(formatTime(0));
        }
        if (tvDuration != null) {
            tvDuration.setText(formatTime(book.getDurationSec() * 1000L));
        }
        if (seekBar != null) {
            seekBar.setProgress(0);
            seekBar.setMax(safeDuration(book.getDurationSec() * 1000L));
        }
        updateDownloadButton();
    }

    private void prepareAudio(Book book) {
        prepareAudio(book, false, C.TIME_UNSET, false);
    }

    private void prepareAudio(Book book, boolean forceReload, long startPositionMs, boolean playWhenReady) {
        if (book == null) {
            return;
        }
        if (mediaController == null) {
            setPlayerEnabled(false);
            return;
        }

        Uri audioUri = audioSourceResolver.resolve(book);
        if (audioUri == null) {
            setPlayerEnabled(false);
            Toast.makeText(this, missingAudioMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        setPlayerEnabled(true);
        mediaController.setPlaybackParameters(new PlaybackParameters(playbackSpeeds[speedIndex]));

        MediaItem currentItem = mediaController.getCurrentMediaItem();
        boolean sameBook = currentItem != null && book.getId().equals(currentItem.mediaId);
        boolean sameSource = sameBook && isSameResolvedSource(currentItem, audioUri);
        if (sameBook && !sameSource && startPositionMs == C.TIME_UNSET) {
            startPositionMs = mediaController.getCurrentPosition();
            playWhenReady = mediaController.isPlaying();
        }
        if (sameBook && sameSource && !forceReload) {
            updateProgressUi();
            updatePlayButton();
            progressHandler.removeCallbacks(progressRunnable);
            progressHandler.post(progressRunnable);
            return;
        }

        progressHandler.removeCallbacks(progressRunnable);
        MediaItem mediaItem = buildMediaItem(book, audioUri);
        if (startPositionMs != C.TIME_UNSET && startPositionMs > 0) {
            mediaController.setMediaItem(mediaItem, startPositionMs);
        } else {
            mediaController.setMediaItem(mediaItem);
        }
        mediaController.prepare();
        if (startPositionMs == C.TIME_UNSET) {
            restoreProgress(book.getId());
        }
        if (playWhenReady) {
            mediaController.play();
        }
        updateProgressUi();
        updatePlayButton();
        progressHandler.post(progressRunnable);
    }

    private MediaItem buildMediaItem(Book book, Uri audioUri) {
        MediaMetadata.Builder metadataBuilder = new MediaMetadata.Builder()
                .setTitle(book.getTitle())
                .setArtist(book.getAuthor())
                .setSubtitle(book.getChapterTitle());
        if (book.getDurationSec() > 0) {
            metadataBuilder.setDurationMs(book.getDurationSec() * 1000L);
        }
        String coverUrl = trimToNull(book.getCoverUrl());
        if (coverUrl != null) {
            metadataBuilder.setArtworkUri(Uri.parse(coverUrl));
        }

        return new MediaItem.Builder()
                .setMediaId(book.getId())
                .setUri(audioUri)
                .setMediaMetadata(metadataBuilder.build())
                .build();
    }

    private void downloadCurrentBook() {
        if (currentBook == null || downloadingAudio) {
            return;
        }
        if (downloadedAudioRepository.isDownloaded(currentBook.getId())) {
            Toast.makeText(this, "Audio is already downloaded.", Toast.LENGTH_SHORT).show();
            updateDownloadButton();
            return;
        }
        if (currentBook.getAudioUrl() == null || currentBook.getAudioUrl().trim().isEmpty()) {
            Toast.makeText(this, "This book does not have an audioUrl to download.", Toast.LENGTH_LONG).show();
            updateDownloadButton();
            return;
        }

        boolean wasPlaying = mediaController != null && mediaController.isPlaying();
        long currentPositionMs = mediaController == null ? 0 : mediaController.getCurrentPosition();
        downloadingAudio = true;
        updateDownloadButton();
        Toast.makeText(this, "Downloading audio...", Toast.LENGTH_SHORT).show();
        String downloadingBookId = currentBook.getId();
        downloadedAudioRepository.download(currentBook, new RepositoryCallback<File>() {
            @Override
            public void onSuccess(File data) {
                runOnUiThread(() -> {
                    downloadingAudio = false;
                    updateDownloadButton();
                    Toast.makeText(ActivityReader.this, "Audio downloaded.", Toast.LENGTH_SHORT).show();
                    if (currentBook != null && currentBook.getId().equals(downloadingBookId)) {
                        prepareAudio(currentBook, true, currentPositionMs, wasPlaying);
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
        boolean downloaded = currentBook != null && downloadedAudioRepository.isDownloaded(currentBook.getId());
        boolean hasRemoteAudio = currentBook != null
                && currentBook.getAudioUrl() != null
                && !currentBook.getAudioUrl().trim().isEmpty();

        btnDownloadAudio.setImageResource(downloaded ? R.drawable.ic_download_done : R.drawable.ic_download);
        btnDownloadAudio.setEnabled(!downloadingAudio && !downloaded && hasRemoteAudio);
        btnDownloadAudio.setAlpha(btnDownloadAudio.isEnabled() || downloaded ? 1f : 0.35f);
    }

    private String missingAudioMessage() {
        return "Missing audio: add a Firestore audioUrl for this book.";
    }

    private void restoreProgress(String bookId) {
        progressRepository.getProgress(bookId, new RepositoryCallback<UserProgress>() {
            @Override
            public void onSuccess(UserProgress progress) {
                if (mediaController != null
                        && isControllerOnBook(bookId)
                        && progress.getPositionMs() > 0) {
                    mediaController.seekTo(progress.getPositionMs());
                    updateProgressUi();
                }
            }

            @Override
            public void onError(Exception exception) {
            }
        });
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

    private void seekBy(long deltaMs) {
        if (mediaController == null) {
            return;
        }
        if (deltaMs < 0) {
            mediaController.seekBack();
        } else {
            mediaController.seekForward();
        }
        updateProgressUi();
    }

    private void cyclePlaybackSpeed() {
        speedIndex = (speedIndex + 1) % playbackSpeeds.length;
        if (tvPlaybackSpeed != null) {
            tvPlaybackSpeed.setText(formatSpeed());
        }
        if (mediaController != null) {
            mediaController.setPlaybackParameters(new PlaybackParameters(playbackSpeeds[speedIndex]));
        }
    }

    private String formatSpeed() {
        return String.format(Locale.US, "%.1fx", playbackSpeeds[speedIndex]);
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
        return currentBook == null ? 0 : currentBook.getDurationSec() * 1000L;
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
        if (btnPlayPause != null) {
            btnPlayPause.setEnabled(enabled);
            btnPlayPause.setAlpha(enabled ? 1f : 0.45f);
        }
        if (btnSkipBack != null) {
            btnSkipBack.setEnabled(enabled);
            btnSkipBack.setAlpha(enabled ? 1f : 0.45f);
        }
        if (btnSkipForward != null) {
            btnSkipForward.setEnabled(enabled);
            btnSkipForward.setAlpha(enabled ? 1f : 0.45f);
        }
        if (seekBar != null) {
            seekBar.setEnabled(enabled);
            seekBar.setAlpha(enabled ? 1f : 0.45f);
        }
        updatePlayButton();
    }

    private void saveProgress() {
        if (mediaController == null) {
            return;
        }
        String bookId = currentBook == null ? null : currentBook.getId();
        MediaItem currentItem = mediaController.getCurrentMediaItem();
        if (currentItem != null && currentItem.mediaId != null && !currentItem.mediaId.trim().isEmpty()) {
            bookId = currentItem.mediaId;
        }
        if (bookId == null || bookId.trim().isEmpty()) {
            return;
        }
        progressRepository.saveProgress(bookId, mediaController.getCurrentPosition(), getDurationMs());
    }

    private boolean isControllerOnBook(String bookId) {
        if (mediaController == null || bookId == null) {
            return false;
        }
        MediaItem currentItem = mediaController.getCurrentMediaItem();
        return currentItem != null && bookId.equals(currentItem.mediaId);
    }

    private boolean isSameResolvedSource(MediaItem currentItem, Uri resolvedUri) {
        return currentItem != null
                && currentItem.localConfiguration != null
                && currentItem.localConfiguration.uri != null
                && currentItem.localConfiguration.uri.equals(resolvedUri);
    }

    private boolean hasAudio(Book book) {
        return audioSourceResolver.resolve(book) != null;
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
