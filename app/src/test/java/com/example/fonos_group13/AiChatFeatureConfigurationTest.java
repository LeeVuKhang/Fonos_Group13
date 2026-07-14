package com.example.fonos_group13;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class AiChatFeatureConfigurationTest {
    @Test public void chatIsSessionOnlyAndAvailableFromDetailAndReader() throws Exception {
        String activity = read("src/main/java/com/example/fonos_group13/AiChatActivity.java");
        String detail = read("src/main/java/com/example/fonos_group13/BookDetailActivity.java");
        String reader = read("src/main/java/com/example/fonos_group13/ActivityReader.java");
        String manifest = read("src/main/AndroidManifest.xml");

        assertTrue(activity.contains("STATE_MESSAGES"));
        assertTrue(activity.contains("STATE_SPOILER_CONFIRMED"));
        assertTrue(activity.contains("controller.stop()"));
        assertTrue(activity.contains("response.getCitations()"));
        assertTrue(detail.contains("AiChatActivity.newIntent(this, currentBook, chapters, null)"));
        assertTrue(reader.contains("currentChapter.getId()"));
        assertTrue(manifest.contains("android:name=\".AiChatActivity\""));
    }

    @Test public void chatControlsFitTheViewportAndCopyActionIsRemoved() throws Exception {
        String activity = read("src/main/java/com/example/fonos_group13/AiChatActivity.java");
        String layout = read("src/main/res/layout/activity_ai_chat.xml");

        assertFalse(activity.contains("ClipboardManager"));
        assertFalse(activity.contains("copyAnswer("));
        assertFalse(layout.contains("<HorizontalScrollView"));
        assertTrue(layout.contains("android:id=\"@+id/ai_question_container\""));
        assertTrue(layout.contains("app:strokeColor=\"@color/control_stroke\""));
    }

    private static String read(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
