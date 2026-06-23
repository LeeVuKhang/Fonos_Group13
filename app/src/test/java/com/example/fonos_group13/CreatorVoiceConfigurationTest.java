package com.example.fonos_group13;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.fonos_group13.model.CreateAudiobookDraftInput;
import com.example.fonos_group13.model.CreatorVoiceOption;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class CreatorVoiceConfigurationTest {
    @Test
    public void exposesRuthAndPatrickWithPatrickAsTheMaleDefault() {
        assertEquals("Ruth", CreatorVoiceOption.RUTH.getVoiceId());
        assertEquals("female", CreatorVoiceOption.RUTH.getGender());
        assertEquals("Patrick", CreatorVoiceOption.PATRICK.getVoiceId());
        assertEquals("male", CreatorVoiceOption.PATRICK.getGender());
        assertEquals("Patrick - male", CreatorVoiceOption.PATRICK.getLabel());

        CreateAudiobookDraftInput input = new CreateAudiobookDraftInput(
                "Title",
                "Author",
                null,
                null,
                "Text",
                null,
                null
        );
        assertEquals("Patrick", input.getVoiceOption().getVoiceId());
    }

    @Test
    public void selectorSourceContainsPatrickAndNoLegacyNarrator() throws Exception {
        String activity = readFile("src/main/java/com/example/fonos_group13/CreateAudiobookActivity.java");
        String layout = readFile("src/main/res/layout/activity_create_audiobook.xml");
        String combined = activity + layout;

        assertTrue(combined.contains("PATRICK"));
        assertTrue(layout.contains("@+id/voice_patrick"));
        assertTrue(layout.contains("Patrick - male"));
        assertTrue(layout.contains("Ruth - female"));
        assertFalse(combined.contains("Matt" + "hew"));
        assertFalse(combined.contains("MATT" + "HEW"));
        assertFalse(combined.contains("voice_" + "matthew"));
    }

    private String readFile(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
