package com.example.fonos_group13.data.community;

import com.example.fonos_group13.data.creator.BackendApiException;
import com.example.fonos_group13.model.BookReview;
import com.example.fonos_group13.model.BookReviewPage;
import com.example.fonos_group13.model.ReviewMutationResult;
import com.example.fonos_group13.model.SaveMutationResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

final class CommunityApiContract {
    private CommunityApiContract() {
    }

    static String reviewJson(int rating, String comment) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("rating", rating);
        String clean = optionalText(comment);
        json.put("comment", clean == null ? JSONObject.NULL : clean);
        return json.toString();
    }

    static BookReviewPage parseReviewPage(int statusCode, String body) throws BackendApiException {
        JSONObject data = successData(statusCode, body);
        try {
            JSONArray items = data.optJSONArray("reviews");
            List<BookReview> reviews = new ArrayList<>();
            if (items != null) {
                for (int index = 0; index < items.length(); index++) {
                    reviews.add(parseReview(items.getJSONObject(index)));
                }
            }
            BookReview viewer = data.isNull("viewerReview") || !data.has("viewerReview")
                    ? null : parseReview(data.getJSONObject("viewerReview"));
            return new BookReviewPage(
                    reviews,
                    viewer,
                    optionalText(data.optString("nextCursor", null)),
                    data.optBoolean("hasMore", false)
            );
        } catch (JSONException exception) {
            throw invalidResponse(statusCode);
        }
    }

    static ReviewMutationResult parseReviewMutation(int statusCode, String body) throws BackendApiException {
        JSONObject data = successData(statusCode, body);
        try {
            BookReview review = data.isNull("review") || !data.has("review")
                    ? null : parseReview(data.getJSONObject("review"));
            return new ReviewMutationResult(
                    review,
                    data.optBoolean("deleted", false),
                    data.optDouble("ratingAverage", 0),
                    data.optInt("ratingCount", 0)
            );
        } catch (JSONException exception) {
            throw invalidResponse(statusCode);
        }
    }

    static SaveMutationResult parseSaveMutation(int statusCode, String body) throws BackendApiException {
        JSONObject data = successData(statusCode, body);
        return new SaveMutationResult(data.optBoolean("saved", false), data.optInt("saveCount", 0));
    }

    private static JSONObject successData(int statusCode, String body) throws BackendApiException {
        if (statusCode < 200 || statusCode >= 300) throw parseError(statusCode, body);
        try {
            return new JSONObject(emptyJson(body)).getJSONObject("data");
        } catch (JSONException exception) {
            throw invalidResponse(statusCode);
        }
    }

    private static BookReview parseReview(JSONObject json) {
        return new BookReview(
                json.optString("reviewerDisplayName", "Reader"),
                json.optInt("rating", 1),
                json.isNull("comment") ? null : json.optString("comment", null),
                json.isNull("createdAt") ? null : json.optString("createdAt", null),
                json.isNull("updatedAt") ? null : json.optString("updatedAt", null),
                json.optBoolean("edited", false)
        );
    }

    static BackendApiException parseError(int statusCode, String body) {
        try {
            JSONObject error = new JSONObject(emptyJson(body)).getJSONObject("error");
            Object details = error.opt("details");
            return new BackendApiException(
                    statusCode,
                    error.optString("code", "http_error"),
                    error.optString("message", "Backend request failed."),
                    details == null || details == JSONObject.NULL ? null : String.valueOf(details)
            );
        } catch (JSONException exception) {
            return new BackendApiException(statusCode, "http_error", "Backend request failed.", null);
        }
    }

    private static BackendApiException invalidResponse(int statusCode) {
        return new BackendApiException(statusCode, "invalid_response", "Backend returned an invalid response.", null);
    }

    private static String optionalText(String value) {
        if (value == null) return null;
        String clean = value.trim();
        return clean.isEmpty() ? null : clean;
    }

    private static String emptyJson(String value) {
        return value == null || value.trim().isEmpty() ? "{}" : value;
    }
}
