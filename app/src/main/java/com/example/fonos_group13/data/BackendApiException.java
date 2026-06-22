package com.example.fonos_group13.data;

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
}
