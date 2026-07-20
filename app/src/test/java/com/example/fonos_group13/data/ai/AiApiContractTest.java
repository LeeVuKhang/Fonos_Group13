package com.example.fonos_group13.data.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import com.example.fonos_group13.data.creator.BackendApiException;
import com.example.fonos_group13.model.AiChatMessage;
import com.example.fonos_group13.model.AiResponse;
import com.example.fonos_group13.model.AiScope;

import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class AiApiContractTest {
    @Test public void serializesChapterQuestionAndCapsHistoryAtTwelveMessages() throws Exception {
        List<AiChatMessage> history = new ArrayList<>();
        for (int index = 0; index < 14; index++) {
            history.add(new AiChatMessage(index % 2 == 0 ? "user" : "assistant", "message-" + index));
        }

        JSONObject json = new JSONObject(AiApiContract.requestJson(
                "question", AiScope.chapter("chapter_2"), "Why?", "auto", history));

        assertEquals("chapter", json.getJSONObject("scope").getString("type"));
        assertEquals("chapter_2", json.getJSONObject("scope").getString("chapterId"));
        assertEquals("Why?", json.getString("question"));
        assertEquals(12, json.getJSONArray("history").length());
        assertEquals("message-2", json.getJSONArray("history").getJSONObject(0).getString("text"));
    }

    @Test public void parsesGroundedResponseAndBackendError() throws Exception {
        AiResponse response = AiApiContract.parseResponse(200, "{\"data\":{" +
                "\"answer\":\"Because the text says so.\",\"notFound\":false," +
                "\"scope\":{\"type\":\"chapter\",\"chapterId\":\"chapter_2\"}," +
                "\"contentVersion\":\"hash\",\"citations\":[{" +
                "\"chapterId\":\"chapter_2\",\"chapterTitle\":\"Chapter 2\"," +
                "\"excerpt\":\"Exact excerpt\"}]}}");

        assertFalse(response.isNotFound());
        assertEquals("hash", response.getContentVersion());
        assertEquals("Exact excerpt", response.getCitations().get(0).getExcerpt());

        BackendApiException error = AiApiContract.parseError(429,
                "{\"error\":{\"code\":\"ai_rate_limit_exceeded\",\"message\":\"Slow down\"}}");
        assertEquals(429, error.getStatusCode());
        assertEquals("ai_rate_limit_exceeded", error.getErrorCode());

        BackendApiException unavailable = AiApiContract.parseError(503,
                "{\"error\":{\"code\":\"ai_provider_unavailable\",\"message\":\"Retry later\"}}",
                AiApiContract.parseRetryAfterSeconds("17"));
        assertEquals(Integer.valueOf(17), unavailable.getRetryAfterSeconds());
        assertEquals(Integer.valueOf(86400), AiApiContract.parseRetryAfterSeconds("999999"));
        assertNull(AiApiContract.parseRetryAfterSeconds("not-a-number"));
    }
}
