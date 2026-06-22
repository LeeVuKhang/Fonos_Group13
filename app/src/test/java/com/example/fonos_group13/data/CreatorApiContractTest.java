package com.example.fonos_group13.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.fonos_group13.model.CreateAudiobookDraftInput;
import com.example.fonos_group13.model.CreatorVoiceOption;

import org.json.JSONObject;
import org.junit.Test;

public class CreatorApiContractTest {
    @Test
    public void createDraftJsonExcludesIdentityFields() throws Exception {
        CreateAudiobookDraftInput input = new CreateAudiobookDraftInput(
                " Title ",
                " Author ",
                "",
                "Chapter 1",
                "Hello",
                "en-US",
                CreatorVoiceOption.RUTH
        );

        JSONObject json = new JSONObject(CreatorApiContract.createDraftJson(input));

        assertEquals("Title", json.getString("title"));
        assertEquals("Author", json.getString("author"));
        assertEquals("Ruth", json.getString("voiceId"));
        assertFalse(json.has("creatorUid"));
        assertFalse(json.has("createdByUser"));
        assertFalse(json.has("coverUrl"));
    }

    @Test
    public void parsesSuccessAndErrorEnvelopes() throws Exception {
        String success = "{\"data\":{\"bookId\":\"book-1\",\"generationStatus\":\"draft\"}}";
        assertEquals("book-1", CreatorApiContract.parseBookId(201, success));

        String error = "{\"error\":{\"code\":\"validation_error\",\"message\":\"Request validation failed\",\"details\":[{\"field\":\"chapterText\",\"message\":\"Too long\"}]}}";
        try {
            CreatorApiContract.parseBookId(422, error);
        } catch (BackendApiException exception) {
            assertEquals(422, exception.getStatusCode());
            assertEquals("validation_error", exception.getErrorCode());
            assertTrue(exception.getDetails().contains("chapterText"));
            return;
        }

        throw new AssertionError("Expected BackendApiException");
    }
}
