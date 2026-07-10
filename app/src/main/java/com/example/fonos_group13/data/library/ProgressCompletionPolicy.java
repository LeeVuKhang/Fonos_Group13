package com.example.fonos_group13.data.library;

public final class ProgressCompletionPolicy {
    private static final double COMPLETION_RATIO = 0.95d;

    private ProgressCompletionPolicy() {
    }

    public static boolean isCompleted(long positionMs, long durationMs) {
        return durationMs > 0 && Math.max(positionMs, 0) >= durationMs * COMPLETION_RATIO;
    }
}
