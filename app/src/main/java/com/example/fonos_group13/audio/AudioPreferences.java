package com.example.fonos_group13.audio;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Locale;

public final class AudioPreferences {
    public static final float[] PLAYBACK_SPEEDS = {1.0f, 1.2f, 1.5f, 2.0f};

    private static final String PREFS_NAME = "audio_preferences";
    private static final String KEY_DEFAULT_SPEED_INDEX = "default_speed_index";

    private AudioPreferences() {
    }

    public static int getDefaultSpeedIndex(Context context) {
        if (context == null) {
            return 0;
        }
        SharedPreferences preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return clampSpeedIndex(preferences.getInt(KEY_DEFAULT_SPEED_INDEX, 0));
    }

    public static void setDefaultSpeedIndex(Context context, int speedIndex) {
        if (context == null) {
            return;
        }
        SharedPreferences preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        preferences.edit()
                .putInt(KEY_DEFAULT_SPEED_INDEX, clampSpeedIndex(speedIndex))
                .apply();
    }

    public static int speedCount() {
        return PLAYBACK_SPEEDS.length;
    }

    public static float getSpeedAt(int speedIndex) {
        return PLAYBACK_SPEEDS[clampSpeedIndex(speedIndex)];
    }

    public static String formatSpeed(int speedIndex) {
        return String.format(Locale.US, "%.1fx", getSpeedAt(speedIndex));
    }

    public static int clampSpeedIndex(int speedIndex) {
        if (speedIndex < 0 || speedIndex >= PLAYBACK_SPEEDS.length) {
            return 0;
        }
        return speedIndex;
    }
}
