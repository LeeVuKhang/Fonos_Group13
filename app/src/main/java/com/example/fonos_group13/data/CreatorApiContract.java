package com.example.fonos_group13.data;

import com.example.fonos_group13.model.CreateAudiobookDraftInput;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class CreatorApiContract {
    private CreatorApiContract() {
    }

    static String createDraftJson(CreateAudiobookDraftInput input) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("title", input.getTitle());
        json.put("author", input.getAuthor());
        if (input.getCoverUrl() != null) {
            json.put("coverUrl", input.getCoverUrl());
        }
        json.put("chapterTitle", input.getChapterTitle());
        json.put("chapterText", input.getChapterText());
        json.put("languageCode", input.getLanguageCode());
        json.put("voiceId", input.getVoiceOption().getVoiceId());
        return json.toString();
    }

    static String parseBookId(int statusCode, String responseBody) throws BackendApiException {
        if (statusCode >= 200 && statusCode < 300) {
            try {
                JSONObject root = new JSONObject(valueOrEmptyJson(responseBody));
                String bookId = root.getJSONObject("data").getString("bookId");
                if (bookId == null || bookId.trim().isEmpty()) {
                    throw new JSONException("Missing bookId");
                }
                return bookId;
            } catch (JSONException exception) {
                throw new BackendApiException(statusCode, "invalid_response", "Backend returned an invalid response.", null);
            }
        }
        throw parseError(statusCode, responseBody);
    }

    static BackendApiException parseError(int statusCode, String responseBody) {
        try {
            JSONObject root = new JSONObject(valueOrEmptyJson(responseBody));
            JSONObject error = root.getJSONObject("error");
            String details = null;
            if (error.has("details")) {
                Object rawDetails = error.get("details");
                details = rawDetails instanceof JSONArray ? rawDetails.toString() : String.valueOf(rawDetails);
            }
            return new BackendApiException(
                    statusCode,
                    error.optString("code", "http_error"),
                    error.optString("message", "Backend request failed."),
                    details
            );
        } catch (JSONException exception) {
            return new BackendApiException(
                    statusCode,
                    "http_error",
                    "Backend request failed with status " + statusCode + ".",
                    null
            );
        }
    }

    private static String valueOrEmptyJson(String responseBody) {
        return responseBody == null || responseBody.trim().isEmpty() ? "{}" : responseBody;
    }
}
