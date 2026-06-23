package com.example.fonos_group13.model;

public enum AudiobookGenerationStatus {
    DRAFT("draft"),
    PENDING_GENERATION("pending_generation"),
    FAILED("failed"),
    READY_FOR_REVIEW("ready_for_review"),
    PUBLISHED("published"),
    REJECTED("rejected");

    private final String value;

    AudiobookGenerationStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static AudiobookGenerationStatus fromValue(String value) {
        if (value != null) {
            for (AudiobookGenerationStatus status : values()) {
                if (status.value.equals(value.trim())) {
                    return status;
                }
            }
        }
        return DRAFT;
    }
}
