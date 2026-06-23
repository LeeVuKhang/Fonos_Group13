package com.example.fonos_group13.model;

public class CreatorVoiceOption {
    public static final CreatorVoiceOption RUTH = new CreatorVoiceOption("Ruth", "female");
    public static final CreatorVoiceOption PATRICK = new CreatorVoiceOption("Patrick", "male");

    private final String voiceId;
    private final String gender;

    public CreatorVoiceOption(String voiceId, String gender) {
        this.voiceId = valueOrDefault(voiceId, "Patrick");
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
