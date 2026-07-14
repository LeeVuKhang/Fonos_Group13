package com.example.fonos_group13.model;

public enum AiStatus {
    UNAVAILABLE,
    INDEXING,
    READY,
    FAILED;

    public static AiStatus fromValue(String value) {
        if (value == null) return UNAVAILABLE;
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return UNAVAILABLE;
        }
    }
}
