package com.example.fonos_group13;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class MiniPlayerConfigurationTest {
    @Test
    public void miniPlayerUsesTheExistingPlaybackSession() throws Exception {
        String view = read("src/main/java/com/example/fonos_group13/ui/MiniPlayerView.java");

        assertTrue(view.contains("new MediaController.Builder"));
        assertTrue(view.contains("PlaybackService.class"));
        assertTrue(view.contains("Player.Listener"));
        assertTrue(view.contains("Player.STATE_ENDED"));
        assertTrue(view.contains("MediaController.releaseFuture"));
    }

    @Test
    public void allMainTabsPlaceMiniPlayerAboveBottomNavigation() throws Exception {
        assertMainTabContainsMiniPlayer("activity_discover.xml");
        assertMainTabContainsMiniPlayer("activity_search.xml");
        assertMainTabContainsMiniPlayer("activity_library.xml");
        assertMainTabContainsMiniPlayer("activity_profile.xml");
    }

    @Test
    public void readerAndSessionPreserveSingleChapterPreviewMetadata() throws Exception {
        String reader = read("src/main/java/com/example/fonos_group13/ActivityReader.java");
        String service = read("src/main/java/com/example/fonos_group13/audio/PlaybackService.java");
        String view = read("src/main/java/com/example/fonos_group13/ui/MiniPlayerView.java");

        assertTrue(reader.contains("METADATA_SINGLE_CHAPTER_PREVIEW"));
        assertTrue(reader.contains("extras.putBoolean(METADATA_SINGLE_CHAPTER_PREVIEW"));
        assertTrue(service.contains("EXTRA_SINGLE_CHAPTER_PREVIEW"));
        assertTrue(view.contains("EXTRA_SINGLE_CHAPTER_PREVIEW"));
    }

    private void assertMainTabContainsMiniPlayer(String fileName) throws Exception {
        String layout = read("src/main/res/layout/" + fileName);
        assertTrue(layout.contains("com.example.fonos_group13.ui.MiniPlayerView"));
        assertTrue(layout.contains("android:id=\"@+id/mini_player\""));
        assertTrue(layout.contains("app:layout_constraintBottom_toTopOf=\"@id/mini_player\""));
    }

    private String read(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
