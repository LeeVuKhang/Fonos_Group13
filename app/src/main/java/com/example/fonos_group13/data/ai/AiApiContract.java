package com.example.fonos_group13.data.ai;

import com.example.fonos_group13.data.creator.BackendApiException;
import com.example.fonos_group13.model.AiChatMessage;
import com.example.fonos_group13.model.AiCitation;
import com.example.fonos_group13.model.AiResponse;
import com.example.fonos_group13.model.AiScope;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

final class AiApiContract {
    private AiApiContract() {}

    static String requestJson(
            String mode,
            AiScope scope,
            String question,
            String locale,
            List<AiChatMessage> history
    ) throws JSONException {
        JSONObject body = new JSONObject();
        body.put("mode", mode);
        JSONObject scopeJson = new JSONObject().put("type", scope.getType());
        if (!scope.isBook()) scopeJson.put("chapterId", scope.getChapterId());
        body.put("scope", scopeJson);
        body.put("locale", locale);
        if (question != null) body.put("question", question);

        JSONArray historyJson = new JSONArray();
        if (history != null) {
            int first = Math.max(0, history.size() - 12);
            for (int index = first; index < history.size(); index++) {
                AiChatMessage message = history.get(index);
                if (message == null) continue;
                historyJson.put(new JSONObject()
                        .put("role", message.getRole())
                        .put("text", message.getText()));
            }
        }
        body.put("history", historyJson);
        return body.toString();
    }

    static AiResponse parseResponse(int statusCode, String body) throws BackendApiException {
        JSONObject data = successData(statusCode, body);
        try {
            JSONObject scopeJson = data.getJSONObject("scope");
            AiScope scope = AiScope.TYPE_CHAPTER.equals(scopeJson.optString("type"))
                    ? AiScope.chapter(scopeJson.optString("chapterId"))
                    : AiScope.book();
            List<AiCitation> citations = new ArrayList<>();
            JSONArray citationJson = data.optJSONArray("citations");
            if (citationJson != null) {
                for (int index = 0; index < citationJson.length(); index++) {
                    JSONObject item = citationJson.getJSONObject(index);
                    citations.add(new AiCitation(
                            item.optString("chapterId", ""),
                            item.optString("chapterTitle", ""),
                            item.optString("excerpt", "")
                    ));
                }
            }
            return new AiResponse(
                    data.getString("answer"),
                    data.optBoolean("notFound", false),
                    scope,
                    data.optString("contentVersion", ""),
                    citations
            );
        } catch (JSONException exception) {
            throw invalidResponse(statusCode);
        }
    }

    static BackendApiException parseError(int statusCode, String body) {
        return parseError(statusCode, body, null);
    }

    static BackendApiException parseError(int statusCode, String body, Integer retryAfterSeconds) {
        try {
            JSONObject error = new JSONObject(emptyJson(body)).getJSONObject("error");
            Object details = error.opt("details");
            return new BackendApiException(
                    statusCode,
                    error.optString("code", "http_error"),
                    error.optString("message", "AI request failed."),
                    details == null || details == JSONObject.NULL ? null : String.valueOf(details),
                    retryAfterSeconds
            );
        } catch (JSONException exception) {
            return new BackendApiException(
                    statusCode,
                    "http_error",
                    "AI request failed.",
                    null,
                    retryAfterSeconds
            );
        }
    }

    static Integer parseRetryAfterSeconds(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try {
            long seconds = Long.parseLong(value.trim());
            if (seconds <= 0) return null;
            return (int) Math.min(seconds, 86_400L);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static JSONObject successData(int statusCode, String body) throws BackendApiException {
        if (statusCode < 200 || statusCode >= 300) throw parseError(statusCode, body);
        try {
            return new JSONObject(emptyJson(body)).getJSONObject("data");
        } catch (JSONException exception) {
            throw invalidResponse(statusCode);
        }
    }

    private static BackendApiException invalidResponse(int statusCode) {
        return new BackendApiException(statusCode, "invalid_response", "Backend returned an invalid AI response.", null);
    }

    private static String emptyJson(String value) {
        return value == null || value.trim().isEmpty() ? "{}" : value;
    }
}
