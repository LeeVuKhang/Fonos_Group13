package com.example.fonos_group13.audio;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.CommandButton;
import androidx.media3.session.DefaultMediaNotificationProvider;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

import com.example.fonos_group13.ActivityReader;
import com.example.fonos_group13.FonosApplication;
import com.example.fonos_group13.MainActivity;
import com.example.fonos_group13.R;
import com.example.fonos_group13.data.core.RepositoryCallback;
import com.example.fonos_group13.data.repository.ProgressRepository;
import com.example.fonos_group13.model.BookChapter;
import com.google.common.collect.ImmutableList;

@OptIn(markerClass = UnstableApi.class)
public class PlaybackService extends MediaSessionService {
    public static final String NOTIFICATION_CHANNEL_ID = "audiobook_playback";

    private static final int NOTIFICATION_ID = 1001;
    private static final String TAG = "PlaybackService";

    private ExoPlayer player;
    private MediaSession mediaSession;
    private ProgressRepository progressRepository;

    @Override
    public void onCreate() {
        super.onCreate();
        progressRepository = FonosApplication.container(this).progressRepository();

        player = new ExoPlayer.Builder(this)
                .build();
        player.setAudioAttributes(
                new AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                        .build(),
                true
        );
        player.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                if (!isPlaying) {
                    saveCurrentProgress();
                }
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_ENDED) {
                    saveCurrentProgress();
                }
            }

            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                updateSessionActivity();
            }

            @Override
            public void onPositionDiscontinuity(
                    Player.PositionInfo oldPosition,
                    Player.PositionInfo newPosition,
                    int reason
            ) {
                if (oldPosition.mediaItemIndex != newPosition.mediaItemIndex) {
                    saveProgress(oldPosition.mediaItem, oldPosition.positionMs, getDurationMs(oldPosition.mediaItem));
                }
            }
        });

        DefaultMediaNotificationProvider notificationProvider =
                new DefaultMediaNotificationProvider.Builder(this)
                        .setNotificationId(NOTIFICATION_ID)
                        .setChannelId(NOTIFICATION_CHANNEL_ID)
                        .setChannelName(R.string.playback_notification_channel_name)
                        .build();
        notificationProvider.setSmallIcon(R.drawable.ic_book_open);
        setMediaNotificationProvider(notificationProvider);

        mediaSession = new MediaSession.Builder(this, player)
                .setSessionActivity(createSessionActivity())
                .setMediaButtonPreferences(ImmutableList.of(
                        new CommandButton.Builder(CommandButton.ICON_PREVIOUS)
                                .setDisplayName("Previous chapter")
                                .setPlayerCommand(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                                .setSlots(CommandButton.SLOT_BACK)
                                .build(),
                        new CommandButton.Builder(CommandButton.ICON_NEXT)
                                .setDisplayName("Next chapter")
                                .setPlayerCommand(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                                .setSlots(CommandButton.SLOT_FORWARD)
                                .build()
                ))
                .build();
        updateSessionActivity();
    }

    @Nullable
    @Override
    public MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo) {
        return mediaSession;
    }

    @Override
    public void onDestroy() {
        saveCurrentProgress();
        if (mediaSession != null) {
            mediaSession.release();
            mediaSession = null;
        }
        if (player != null) {
            player.release();
            player = null;
        }
        super.onDestroy();
    }

    private PendingIntent createSessionActivity() {
        String bookId = getCurrentBookId();
        String chapterId = getCurrentChapterId();
        Intent intent;
        if (bookId == null) {
            intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        } else {
            intent = new Intent(this, ActivityReader.class);
            intent.putExtra(ActivityReader.EXTRA_BOOK_ID, bookId);
            intent.putExtra(ActivityReader.EXTRA_CREATOR_PREVIEW, isCurrentCreatorPreview());
            intent.putExtra(ActivityReader.EXTRA_SINGLE_CHAPTER_PREVIEW, isCurrentSingleChapterPreview());
            if (chapterId != null) {
                intent.putExtra(ActivityReader.EXTRA_CHAPTER_ID, chapterId);
            }
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        }
        return PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private void updateSessionActivity() {
        if (mediaSession != null) {
            mediaSession.setSessionActivity(createSessionActivity());
        }
    }

    @Nullable
    private String getCurrentBookId() {
        if (player == null) {
            return null;
        }
        MediaItem currentItem = player.getCurrentMediaItem();
        if (currentItem == null) {
            return null;
        }
        Bundle extras = currentItem.mediaMetadata.extras;
        if (extras != null) {
            String bookId = extras.getString(ActivityReader.METADATA_BOOK_ID);
            if (bookId != null && !bookId.trim().isEmpty()) {
                return bookId.trim();
            }
        }
        if (currentItem.mediaId == null) {
            return null;
        }
        String bookId = currentItem.mediaId.trim();
        return bookId.isEmpty() ? null : bookId;
    }

    @Nullable
    private String getCurrentChapterId() {
        if (player == null) {
            return null;
        }
        MediaItem currentItem = player.getCurrentMediaItem();
        if (currentItem == null || currentItem.mediaMetadata.extras == null) {
            return null;
        }
        String chapterId = currentItem.mediaMetadata.extras.getString(ActivityReader.METADATA_CHAPTER_ID);
        if (chapterId == null) {
            return null;
        }
        String trimmed = chapterId.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isCurrentCreatorPreview() {
        if (player == null) {
            return false;
        }
        MediaItem currentItem = player.getCurrentMediaItem();
        if (currentItem == null || currentItem.mediaMetadata.extras == null) {
            return false;
        }
        return currentItem.mediaMetadata.extras.getBoolean(
                ActivityReader.METADATA_CREATOR_PREVIEW,
                false
        );
    }

    private boolean isCurrentSingleChapterPreview() {
        if (player == null) {
            return false;
        }
        MediaItem currentItem = player.getCurrentMediaItem();
        if (currentItem == null || currentItem.mediaMetadata.extras == null) {
            return false;
        }
        return currentItem.mediaMetadata.extras.getBoolean(
                ActivityReader.METADATA_SINGLE_CHAPTER_PREVIEW,
                false
        );
    }

    private void saveCurrentProgress() {
        if (player == null || progressRepository == null) {
            return;
        }
        saveProgress(player.getCurrentMediaItem(), player.getCurrentPosition(), getDurationMs());
    }

    private void saveProgress(@Nullable MediaItem currentItem, long positionMs, long durationMs) {
        if (currentItem == null || progressRepository == null) {
            return;
        }
        Bundle extras = currentItem.mediaMetadata.extras;
        if (extras != null) {
            String bookId = extras.getString(ActivityReader.METADATA_BOOK_ID);
            String chapterId = extras.getString(ActivityReader.METADATA_CHAPTER_ID);
            if (bookId != null && !bookId.trim().isEmpty()
                    && chapterId != null && !chapterId.trim().isEmpty()) {
                progressRepository.saveProgress(
                        bookId,
                        chapterId,
                        positionMs,
                        durationMs,
                        progressSaveCallback()
                );
                return;
            }
        }
        if (currentItem.mediaId == null || currentItem.mediaId.trim().isEmpty()) {
            return;
        }
        progressRepository.saveProgress(
                currentItem.mediaId,
                BookChapter.LEGACY_CHAPTER_ID,
                positionMs,
                durationMs,
                progressSaveCallback()
        );
    }

    private RepositoryCallback<Void> progressSaveCallback() {
        return new RepositoryCallback<Void>() {
            @Override public void onSuccess(Void data) { }

            @Override
            public void onError(Exception exception) {
                Log.w(TAG, "Could not persist playback progress.", exception);
            }
        };
    }

    private long getDurationMs() {
        if (player == null) {
            return 0;
        }
        long durationMs = player.getDuration();
        if (durationMs != C.TIME_UNSET && durationMs > 0) {
            return durationMs;
        }
        MediaMetadata metadata = player.getMediaMetadata();
        return metadata.durationMs == null ? 0 : metadata.durationMs;
    }

    private long getDurationMs(@Nullable MediaItem mediaItem) {
        if (mediaItem != null && mediaItem.mediaMetadata.durationMs != null) {
            return mediaItem.mediaMetadata.durationMs;
        }
        return getDurationMs();
    }
}
