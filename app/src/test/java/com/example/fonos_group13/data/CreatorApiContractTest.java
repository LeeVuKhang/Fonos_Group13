package com.example.fonos_group13.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.fonos_group13.model.CreateAudiobookDraftInput;
import com.example.fonos_group13.model.CreateChapterDraftInput;
import com.example.fonos_group13.model.CreatorVoiceOption;
import com.example.fonos_group13.model.EditableAudiobookDraft;
import com.example.fonos_group13.model.EditableChapterDraft;

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
    public void maleNarratorUsesPatrickAndExcludesBackendOwnedSynthesisSettings() throws Exception {
        CreateAudiobookDraftInput input = new CreateAudiobookDraftInput(
                "Title",
                "Author",
                null,
                "Chapter 1",
                "Plain chapter text",
                "en-US",
                CreatorVoiceOption.PATRICK
        );

        JSONObject json = new JSONObject(CreatorApiContract.createDraftJson(input));

        assertEquals("Patrick", json.getString("voiceId"));
        assertEquals("Plain chapter text", json.getString("chapterText"));
        assertFalse(json.has("engine"));
        assertFalse(json.has("region"));
        assertFalse(json.has("ss" + "ml"));
        assertFalse(json.has("TextType"));
        assertFalse(json.has("outputFormat"));
        assertFalse(json.has("s3Bucket"));
        assertFalse(json.has("OutputS3BucketName"));
    }

    @Test
    public void updateDraftJsonUsesTheSameSafePayloadAsCreate() throws Exception {
        CreateAudiobookDraftInput input = new CreateAudiobookDraftInput(
                "Updated",
                "Author",
                null,
                "Chapter 1",
                "Edited chapter text",
                "en-US",
                CreatorVoiceOption.PATRICK
        );

        JSONObject json = new JSONObject(CreatorApiContract.createDraftJson(input));

        assertEquals("Updated", json.getString("title"));
        assertEquals("Edited chapter text", json.getString("chapterText"));
        assertFalse(json.has("bookId"));
        assertFalse(json.has("creatorUid"));
        assertFalse(json.has("generationStatus"));
    }

    @Test
    public void createChapterDraftJsonExcludesBookIdentityFields() throws Exception {
        CreateChapterDraftInput input = new CreateChapterDraftInput(
                " Chapter 2 ",
                " More text ",
                "en-US",
                CreatorVoiceOption.RUTH
        );

        JSONObject json = new JSONObject(CreatorApiContract.createChapterDraftJson(input));

        assertEquals("Chapter 2", json.getString("chapterTitle"));
        assertEquals("More text", json.getString("chapterText"));
        assertEquals("Ruth", json.getString("voiceId"));
        assertFalse(json.has("title"));
        assertFalse(json.has("author"));
        assertFalse(json.has("bookId"));
        assertFalse(json.has("creatorUid"));
    }

    @Test
    public void parsesEditableDraftEnvelope() throws Exception {
        String response = "{"
                + "\"data\":{"
                + "\"bookId\":\"book-1\","
                + "\"title\":\"Title\","
                + "\"author\":\"Author\","
                + "\"coverUrl\":null,"
                + "\"chapterTitle\":\"Chapter 1\","
                + "\"chapterText\":\"Full text\","
                + "\"languageCode\":\"en-US\","
                + "\"voiceId\":\"Ruth\","
                + "\"generationStatus\":\"draft\""
                + "}"
                + "}";

        EditableAudiobookDraft draft = CreatorApiContract.parseEditableDraft(200, response);

        assertEquals("book-1", draft.getBookId());
        assertEquals("Title", draft.getTitle());
        assertEquals(null, draft.getCoverUrl());
        assertEquals("Full text", draft.getChapterText());
        assertEquals("Ruth", draft.getVoiceOption().getVoiceId());
        assertEquals("draft", draft.getGenerationStatus().getValue());
    }

    @Test
    public void parsesEditableChapterDraftEnvelope() throws Exception {
        String response = "{"
                + "\"data\":{"
                + "\"bookId\":\"book-1\","
                + "\"chapterId\":\"chapter_2\","
                + "\"bookTitle\":\"Title\","
                + "\"chapterTitle\":\"Chapter 2\","
                + "\"chapterText\":\"Full chapter text\","
                + "\"languageCode\":\"en-US\","
                + "\"voiceId\":\"Patrick\","
                + "\"generationStatus\":\"draft\""
                + "}"
                + "}";

        EditableChapterDraft draft = CreatorApiContract.parseEditableChapterDraft(200, response);

        assertEquals("book-1", draft.getBookId());
        assertEquals("chapter_2", draft.getChapterId());
        assertEquals("Chapter 2", draft.getChapterTitle());
        assertEquals("Full chapter text", draft.getChapterText());
        assertEquals("Patrick", draft.getVoiceOption().getVoiceId());
    }

    @Test
    public void parsesSuccessAndErrorEnvelopes() throws Exception {
        String success = "{\"data\":{\"bookId\":\"book-1\",\"generationStatus\":\"draft\"}}";
        assertEquals("book-1", CreatorApiContract.parseBookId(201, success));

        String chapterSuccess = "{\"data\":{\"bookId\":\"book-1\",\"chapterId\":\"chapter_2\",\"generationStatus\":\"draft\"}}";
        assertEquals("chapter_2", CreatorApiContract.parseChapterId(201, chapterSuccess));

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
