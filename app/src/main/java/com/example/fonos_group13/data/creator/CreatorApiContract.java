package com.example.fonos_group13.data.creator;

import com.example.fonos_group13.model.CreateAudiobookDraftInput;
import com.example.fonos_group13.model.AudiobookGenerationStatus;
import com.example.fonos_group13.model.CreateChapterDraftInput;
import com.example.fonos_group13.model.CreatorVoiceOption;
import com.example.fonos_group13.model.EditableAudiobookDraft;
import com.example.fonos_group13.model.EditableChapterDraft;

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

    static String createChapterDraftJson(CreateChapterDraftInput input) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("chapterTitle", input.getChapterTitle());
        json.put("chapterText", input.getChapterText());
        json.put("languageCode", input.getLanguageCode());
        json.put("voiceId", input.getVoiceOption().getVoiceId());
        return json.toString();
    }

    static String visibilityJson(boolean hiddenByCreator) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("hiddenByCreator", hiddenByCreator);
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

    static String parseChapterId(int statusCode, String responseBody) throws BackendApiException {
        if (statusCode >= 200 && statusCode < 300) {
            try {
                JSONObject root = new JSONObject(valueOrEmptyJson(responseBody));
                String chapterId = root.getJSONObject("data").getString("chapterId");
                if (chapterId == null || chapterId.trim().isEmpty()) {
                    throw new JSONException("Missing chapterId");
                }
                return chapterId;
            } catch (JSONException exception) {
                throw new BackendApiException(statusCode, "invalid_response", "Backend returned an invalid response.", null);
            }
        }
        throw parseError(statusCode, responseBody);
    }

    static EditableAudiobookDraft parseEditableDraft(int statusCode, String responseBody) throws BackendApiException {
        if (statusCode >= 200 && statusCode < 300) {
            try {
                JSONObject root = new JSONObject(valueOrEmptyJson(responseBody));
                JSONObject data = root.getJSONObject("data");
                return new EditableAudiobookDraft(
                        data.getString("bookId"),
                        data.optString("title", ""),
                        data.optString("author", ""),
                        optionalString(data, "coverUrl"),
                        data.optString("chapterTitle", CreateAudiobookDraftInput.DEFAULT_CHAPTER_TITLE),
                        data.optString("chapterText", ""),
                        data.optString("languageCode", CreateAudiobookDraftInput.DEFAULT_LANGUAGE_CODE),
                        CreatorVoiceOption.fromVoiceId(data.optString("voiceId", CreatorVoiceOption.PATRICK.getVoiceId())),
                        AudiobookGenerationStatus.fromValue(data.optString("generationStatus", "draft"))
                );
            } catch (JSONException exception) {
                throw new BackendApiException(statusCode, "invalid_response", "Backend returned an invalid response.", null);
            }
        }
        throw parseError(statusCode, responseBody);
    }

    static EditableChapterDraft parseEditableChapterDraft(int statusCode, String responseBody) throws BackendApiException {
        if (statusCode >= 200 && statusCode < 300) {
            try {
                JSONObject root = new JSONObject(valueOrEmptyJson(responseBody));
                JSONObject data = root.getJSONObject("data");
                return new EditableChapterDraft(
                        data.getString("bookId"),
                        data.getString("chapterId"),
                        data.optString("bookTitle", ""),
                        data.optString("chapterTitle", CreateChapterDraftInput.DEFAULT_CHAPTER_TITLE),
                        data.optString("chapterText", ""),
                        data.optString("languageCode", CreateChapterDraftInput.DEFAULT_LANGUAGE_CODE),
                        CreatorVoiceOption.fromVoiceId(data.optString("voiceId", CreatorVoiceOption.PATRICK.getVoiceId())),
                        AudiobookGenerationStatus.fromValue(data.optString("generationStatus", "draft"))
                );
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

    private static String optionalString(JSONObject json, String key) throws JSONException {
        if (!json.has(key) || json.isNull(key)) {
            return null;
        }
        String value = json.getString(key);
        return value == null || value.trim().isEmpty() ? null : value;
    }
}
