package com.example.fonos_group13.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AudiobookGenerationStatusTest {
    @Test
    public void displayLabelsAreHumanReadable() {
        for (AudiobookGenerationStatus status : AudiobookGenerationStatus.values()) {
            assertFalse(status.getDisplayLabel().contains("_"));
            assertFalse(status.getDisplayLabel().equals(status.getValue()));
        }
        assertTrue(AudiobookGenerationStatus.PENDING_GENERATION.getDisplayLabel().contains(" "));
        assertTrue(AudiobookGenerationStatus.READY_FOR_REVIEW.getDisplayLabel().contains(" "));
    }
}
