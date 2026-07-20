package com.example.fonos_group13.ui;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;

import com.bumptech.glide.Glide;
import com.example.fonos_group13.ActivityReader;
import com.example.fonos_group13.R;
import com.example.fonos_group13.audio.PlaybackService;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

public final class MiniPlayerView extends FrameLayout {
    private final Player.Listener playerListener = new Player.Listener() {
        @Override
        public void onEvents(@NonNull Player player, @NonNull Player.Events events) {
            render(player);
        }
    };

    private View detailsView;
    private ImageView coverView;
    private TextView titleView;
    private TextView subtitleView;
    private ImageButton playPauseButton;
    private ListenableFuture<MediaController> controllerFuture;
    private MediaController mediaController;
    private boolean attached;

    public MiniPlayerView(@NonNull Context context) {
        this(context, null);
    }

    public MiniPlayerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MiniPlayerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(R.layout.view_mini_player, this, true);
        bindViews();
        setVisibility(GONE);
    }

    private void bindViews() {
        detailsView = findViewById(R.id.mini_player_details);
        coverView = findViewById(R.id.mini_player_cover);
        titleView = findViewById(R.id.mini_player_title);
        subtitleView = findViewById(R.id.mini_player_subtitle);
        playPauseButton = findViewById(R.id.mini_player_play_pause);

        detailsView.setOnClickListener(view -> openReader());
        playPauseButton.setOnClickListener(view -> togglePlayback());
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        attached = true;
        connectController();
    }

    @Override
    protected void onDetachedFromWindow() {
        attached = false;
        releaseController();
        super.onDetachedFromWindow();
    }

    private void connectController() {
        if (controllerFuture != null || mediaController != null) {
            return;
        }

        SessionToken sessionToken = new SessionToken(
                getContext(),
                new ComponentName(getContext(), PlaybackService.class)
        );
        ListenableFuture<MediaController> future = new MediaController.Builder(
                getContext(),
                sessionToken
        ).buildAsync();
        controllerFuture = future;
        future.addListener(
                () -> finishControllerConnection(future),
                ContextCompat.getMainExecutor(getContext())
        );
    }

    private void finishControllerConnection(ListenableFuture<MediaController> future) {
        if (!attached || controllerFuture != future) {
            return;
        }
        try {
            mediaController = future.get();
            mediaController.addListener(playerListener);
            render(mediaController);
        } catch (CancellationException | ExecutionException exception) {
            MediaController.releaseFuture(future);
            controllerFuture = null;
            setVisibility(GONE);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            MediaController.releaseFuture(future);
            controllerFuture = null;
            setVisibility(GONE);
        }
    }

    private void render(@Nullable Player player) {
        MediaItem mediaItem = player == null ? null : player.getCurrentMediaItem();
        Bundle extras = mediaItem == null ? null : mediaItem.mediaMetadata.extras;
        String bookId = extras == null
                ? null
                : trimToNull(extras.getString(ActivityReader.METADATA_BOOK_ID));
        String chapterId = extras == null
                ? null
                : trimToNull(extras.getString(ActivityReader.METADATA_CHAPTER_ID));
        if (mediaItem == null || bookId == null || chapterId == null) {
            setVisibility(GONE);
            return;
        }

        MediaMetadata metadata = mediaItem.mediaMetadata;
        String title = firstNonBlank(text(metadata.title), getContext().getString(R.string.app_name));
        String bookTitle = firstNonBlank(text(metadata.artist), getContext().getString(R.string.app_name));
        String author = text(metadata.subtitle);

        titleView.setText(title);
        subtitleView.setText(author == null
                ? bookTitle
                : getContext().getString(R.string.mini_player_book_author, bookTitle, author));
        detailsView.setContentDescription(
                getContext().getString(R.string.mini_player_open_reader, title, bookTitle)
        );
        bindArtwork(metadata.artworkUri);
        updatePlayPauseButton(player.isPlaying());
        setVisibility(VISIBLE);
    }

    private void bindArtwork(@Nullable Uri artworkUri) {
        if (artworkUri == null) {
            Glide.with(coverView).clear(coverView);
            coverView.setImageResource(R.drawable.bg_cover_placeholder);
            return;
        }
        Glide.with(coverView)
                .load(artworkUri)
                .centerCrop()
                .placeholder(R.drawable.bg_cover_placeholder)
                .error(R.drawable.bg_cover_placeholder)
                .into(coverView);
    }

    private void updatePlayPauseButton(boolean playing) {
        playPauseButton.setImageResource(playing ? R.drawable.ic_pause : R.drawable.ic_play);
        playPauseButton.setContentDescription(getContext().getString(
                playing ? R.string.accessibility_pause_audiobook : R.string.accessibility_play_audiobook
        ));
    }

    private void togglePlayback() {
        if (mediaController == null || mediaController.getCurrentMediaItem() == null) {
            return;
        }
        if (mediaController.isPlaying()) {
            mediaController.pause();
        } else {
            if (mediaController.getPlaybackState() == Player.STATE_ENDED) {
                mediaController.seekToDefaultPosition();
                mediaController.prepare();
            } else if (mediaController.getPlaybackState() == Player.STATE_IDLE) {
                mediaController.prepare();
            }
            mediaController.play();
        }
        render(mediaController);
    }

    private void openReader() {
        if (mediaController == null) {
            return;
        }
        MediaItem mediaItem = mediaController.getCurrentMediaItem();
        Bundle extras = mediaItem == null ? null : mediaItem.mediaMetadata.extras;
        String bookId = extras == null
                ? null
                : trimToNull(extras.getString(ActivityReader.METADATA_BOOK_ID));
        String chapterId = extras == null
                ? null
                : trimToNull(extras.getString(ActivityReader.METADATA_CHAPTER_ID));
        if (bookId == null || chapterId == null) {
            return;
        }

        Intent intent = new Intent(getContext(), ActivityReader.class)
                .putExtra(ActivityReader.EXTRA_BOOK_ID, bookId)
                .putExtra(ActivityReader.EXTRA_CHAPTER_ID, chapterId)
                .putExtra(
                        ActivityReader.EXTRA_CREATOR_PREVIEW,
                        extras.getBoolean(ActivityReader.METADATA_CREATOR_PREVIEW, false)
                )
                .putExtra(
                        ActivityReader.EXTRA_SINGLE_CHAPTER_PREVIEW,
                        extras.getBoolean(ActivityReader.METADATA_SINGLE_CHAPTER_PREVIEW, false)
                )
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (!(getContext() instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        getContext().startActivity(intent);
    }

    private void releaseController() {
        setVisibility(GONE);
        if (mediaController != null) {
            mediaController.removeListener(playerListener);
            mediaController = null;
        }
        if (controllerFuture != null) {
            MediaController.releaseFuture(controllerFuture);
            controllerFuture = null;
        }
    }

    @Nullable
    private static String text(@Nullable CharSequence value) {
        return value == null ? null : trimToNull(value.toString());
    }

    private static String firstNonBlank(@Nullable String value, String fallback) {
        return value == null ? fallback : value;
    }

    @Nullable
    private static String trimToNull(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
