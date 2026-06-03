package com.example.fonos_group13;

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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;

import com.example.fonos_group13.audio.AudioSourceResolver;
import com.example.fonos_group13.data.BookRepository;
import com.example.fonos_group13.data.DownloadedAudioRepository;
import com.example.fonos_group13.data.ProgressRepository;
import com.example.fonos_group13.data.RepositoryCallback;
import com.example.fonos_group13.model.Book;
import com.example.fonos_group13.model.UserProgress;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.Locale;

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

    private BookRepository bookRepository;
    private ProgressRepository progressRepository;
    private DownloadedAudioRepository downloadedAudioRepository;
    private AudioSourceResolver audioSourceResolver;
    private ExoPlayer player;
    private Book currentBook;
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

        String bookId = getIntent().getStringExtra(EXTRA_BOOK_ID);
        if (bookId == null || bookId.trim().isEmpty()) {
            bookId = Book.fallbackBooks().get(0).getId();
        }
        loadBook(bookId);
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
                    if (player != null) {
                        player.seekTo(seekBar.getProgress());
                    }
                }
            });
        }
    }

    private void loadBook(String bookId) {
        bookRepository.getBook(bookId, new RepositoryCallback<Book>() {
            @Override
            public void onSuccess(Book book) {
                bindBook(book);
                prepareAudio(book);
            }

            @Override
            public void onError(Exception exception) {
                Book fallback = Book.fallbackById(bookId);
                bindBook(fallback);
                prepareAudio(fallback);
                Toast.makeText(ActivityReader.this, "Could not load Firestore book. Showing local demo data.", Toast.LENGTH_SHORT).show();
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
        releasePlayer();
        Uri audioUri = audioSourceResolver.resolve(book);
        if (audioUri == null) {
            setPlayerEnabled(false);
            Toast.makeText(this, missingAudioMessage(book), Toast.LENGTH_LONG).show();
            return;
        }

        setPlayerEnabled(true);
        player = new ExoPlayer.Builder(this).build();
        player.setPlaybackParameters(new PlaybackParameters(playbackSpeeds[speedIndex]));
        player.addListener(new Player.Listener() {
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
                updatePlayButton();
            }
        });
        player.setMediaItem(MediaItem.fromUri(audioUri));
        player.prepare();
        restoreProgress(book.getId());
        progressHandler.post(progressRunnable);
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
                        prepareAudio(currentBook);
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

    private String missingAudioMessage(Book book) {
        String localName = book == null ? null : book.getAudioLocalResName();
        String localHint = localName == null || localName.trim().isEmpty()
                ? "a matching res/raw MP3"
                : "res/raw/" + localName.trim() + ".mp3";
        return "Missing audio: add Firestore audioUrl with an S3 MP3 URL or add " + localHint;
    }

    private void restoreProgress(String bookId) {
        progressRepository.getProgress(bookId, new RepositoryCallback<UserProgress>() {
            @Override
            public void onSuccess(UserProgress progress) {
                if (player != null && progress.getPositionMs() > 0) {
                    player.seekTo(progress.getPositionMs());
                    updateProgressUi();
                }
            }

            @Override
            public void onError(Exception exception) {
            }
        });
    }

    private void togglePlayback() {
        if (player == null) {
            return;
        }
        if (player.isPlaying()) {
            player.pause();
            saveProgress();
        } else {
            player.play();
        }
        updatePlayButton();
    }

    private void seekBy(long deltaMs) {
        if (player == null) {
            return;
        }
        long durationMs = getDurationMs();
        long nextPosition = player.getCurrentPosition() + deltaMs;
        if (durationMs > 0) {
            nextPosition = Math.min(nextPosition, durationMs);
        }
        player.seekTo(Math.max(0, nextPosition));
        updateProgressUi();
    }

    private void cyclePlaybackSpeed() {
        speedIndex = (speedIndex + 1) % playbackSpeeds.length;
        if (tvPlaybackSpeed != null) {
            tvPlaybackSpeed.setText(formatSpeed());
        }
        if (player != null) {
            player.setPlaybackParameters(new PlaybackParameters(playbackSpeeds[speedIndex]));
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
        long positionMs = player == null ? 0 : player.getCurrentPosition();

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
        long playerDuration = player == null ? C.TIME_UNSET : player.getDuration();
        if (playerDuration != C.TIME_UNSET && playerDuration > 0) {
            return playerDuration;
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
        btnPlayPause.setImageResource(player != null && player.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);
    }

    private void setPlayerEnabled(boolean enabled) {
        if (btnPlayPause != null) {
            btnPlayPause.setEnabled(enabled);
        }
        if (btnSkipBack != null) {
            btnSkipBack.setEnabled(enabled);
        }
        if (btnSkipForward != null) {
            btnSkipForward.setEnabled(enabled);
        }
        if (seekBar != null) {
            seekBar.setEnabled(enabled);
        }
        updatePlayButton();
    }

    private void saveProgress() {
        if (currentBook == null || player == null) {
            return;
        }
        progressRepository.saveProgress(currentBook.getId(), player.getCurrentPosition(), getDurationMs());
    }

    private void releasePlayer() {
        progressHandler.removeCallbacks(progressRunnable);
        if (player != null) {
            saveProgress();
            player.release();
            player = null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) {
            saveProgress();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateDownloadButton();
    }

    @Override
    protected void onDestroy() {
        releasePlayer();
        super.onDestroy();
    }
}
