package com.example.fonos_group13.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

/** Reads Firestore fields without letting a malformed document crash the UI thread. */
public final class FirestoreValueReader {
    private FirestoreValueReader() {
    }

    public static String string(DocumentSnapshot document, String field) {
        return stringValue(rawValue(document, field));
    }

    public static long longValue(DocumentSnapshot document, String field) {
        return longValue(rawValue(document, field));
    }

    public static boolean booleanValue(DocumentSnapshot document, String field, boolean fallback) {
        return booleanValue(rawValue(document, field), fallback);
    }

    public static Timestamp timestamp(DocumentSnapshot document, String field) {
        return timestampValue(rawValue(document, field));
    }

    static String stringValue(Object value) {
        if (!(value instanceof String)) {
            return null;
        }
        String trimmed = ((String) value).trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    static long longValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong(((String) value).trim());
            } catch (NumberFormatException ignored) {
                return 0L;
            }
        }
        return 0L;
    }

    static boolean booleanValue(Object value, boolean fallback) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            String normalized = ((String) value).trim();
            if ("true".equalsIgnoreCase(normalized)) {
                return true;
            }
            if ("false".equalsIgnoreCase(normalized)) {
                return false;
            }
        }
        return fallback;
    }

    static Timestamp timestampValue(Object value) {
        return value instanceof Timestamp ? (Timestamp) value : null;
    }

    private static Object rawValue(DocumentSnapshot document, String field) {
        if (document == null || field == null) {
            return null;
        }
        try {
            return document.get(field);
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
