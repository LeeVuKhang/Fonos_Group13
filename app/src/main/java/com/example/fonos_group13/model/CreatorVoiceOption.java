package com.example.fonos_group13.model;

public class CreatorVoiceOption {
    public static final CreatorVoiceOption MATTHEW = new CreatorVoiceOption("Matthew", "male");
    public static final CreatorVoiceOption RUTH = new CreatorVoiceOption("Ruth", "female");

    private final String voiceId;
    private final String gender;

    public CreatorVoiceOption(String voiceId, String gender) {
        this.voiceId = valueOrDefault(voiceId, "Matthew");
        this.gender = valueOrDefault(gender, "male").toLowerCase();
    }

    public String getVoiceId() {
        return voiceId;
    }

    public String getGender() {
        return gender;
    }

    public String getLabel() {
        return voiceId + " - " + gender;
    }

    private static String valueOrDefault(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }
}
