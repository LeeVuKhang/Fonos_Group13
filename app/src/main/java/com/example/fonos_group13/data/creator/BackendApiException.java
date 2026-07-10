package com.example.fonos_group13.data.creator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class BackendApiException extends Exception {
    private final int statusCode;
    private final String errorCode;
    private final String details;

    public BackendApiException(int statusCode, String errorCode, String message, String details) {
        super(message == null || message.trim().isEmpty() ? "Backend request failed." : message);
        this.statusCode = statusCode;
        this.errorCode = errorCode == null || errorCode.trim().isEmpty() ? "http_error" : errorCode;
        this.details = details;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getDetails() {
        return details;
    }

    public String getValidationMessageForField(String fieldName) {
        String safeFieldName = normalize(fieldName);
        if (safeFieldName == null || details == null || details.trim().isEmpty()) {
            return null;
        }
        try {
            String trimmedDetails = details.trim();
            if (trimmedDetails.startsWith("[")) {
                return findValidationMessage(new JSONArray(trimmedDetails), safeFieldName);
            }
            if (trimmedDetails.startsWith("{")) {
                return findValidationMessage(new JSONObject(trimmedDetails), safeFieldName);
            }
        } catch (JSONException ignored) {
            return null;
        }
        return null;
    }

    private String findValidationMessage(JSONArray detailsArray, String fieldName) throws JSONException {
        for (int index = 0; index < detailsArray.length(); index++) {
            Object item = detailsArray.get(index);
            String message = findValidationMessage(item, fieldName);
            if (message != null) {
                return message;
            }
        }
        return null;
    }

    private String findValidationMessage(Object item, String fieldName) throws JSONException {
        if (item instanceof JSONObject) {
            return findValidationMessage((JSONObject) item, fieldName);
        }
        if (item instanceof JSONArray) {
            return findValidationMessage((JSONArray) item, fieldName);
        }
        return null;
    }

    private String findValidationMessage(JSONObject detail, String fieldName) throws JSONException {
        String detailField = normalize(detail.optString("field", detail.optString("path", "")));
        if (fieldMatches(detailField, fieldName)) {
            String message = normalize(detail.optString("message", ""));
            return message == null ? getMessage() : message;
        }
        String nestedMessage = findNestedValidationMessage(detail, "details", fieldName);
        if (nestedMessage != null) {
            return nestedMessage;
        }
        nestedMessage = findNestedValidationMessage(detail, "errors", fieldName);
        if (nestedMessage != null) {
            return nestedMessage;
        }
        return findNestedValidationMessage(detail, "issues", fieldName);
    }

    private String findNestedValidationMessage(JSONObject detail, String key, String fieldName) throws JSONException {
        if (!detail.has(key)) {
            return null;
        }
        Object nested = detail.get(key);
        return findValidationMessage(nested, fieldName);
    }

    private boolean fieldMatches(String detailField, String fieldName) {
        return detailField != null
                && (detailField.equals(fieldName) || detailField.endsWith("." + fieldName));
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
