package com.example.fonos_group13.data.creator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.fonos_group13.model.CreateAudiobookDraftInput;
import com.example.fonos_group13.model.CreatorVoiceOption;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class CreatorArchitectureTest {
    @Test
    public void validatorAcceptsValidDraftAndRejectsMissingTitle() {
        CreatorDraftValidator validator = new CreatorDraftValidator();
        CreateAudiobookDraftInput valid = new CreateAudiobookDraftInput(
                "Title", "Author", null, "Chapter 1", "Text", "en-US", CreatorVoiceOption.PATRICK
        );
        CreateAudiobookDraftInput missingTitle = new CreateAudiobookDraftInput(
                " ", "Author", null, "Chapter 1", "Text", "en-US", CreatorVoiceOption.PATRICK
        );

        assertNull(validator.validate(valid));
        assertTrue(validator.validate(missingTitle) instanceof IllegalArgumentException);
    }

    @Test
    public void uiUsesNeutralSubscriptionsAndHttpUsesSharedExecutor() throws Exception {
        String activity = readFile("src/main/java/com/example/fonos_group13/MyUploadsActivity.java");
        String apiClient = readFile("src/main/java/com/example/fonos_group13/data/creator/CreatorApiClient.java");

        assertFalse(activity.contains("ListenerRegistration"));
        assertFalse(apiClient.contains("Executors.newSingleThreadExecutor"));
    }

    private String readFile(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
