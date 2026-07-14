package com.example.fonos_group13.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AiStatusTest {
    @Test public void mapsFirestoreValuesAndDefaultsUnknownValuesToUnavailable() {
        assertEquals(AiStatus.READY, AiStatus.fromValue("ready"));
        assertEquals(AiStatus.INDEXING, AiStatus.fromValue(" INDEXING "));
        assertEquals(AiStatus.FAILED, AiStatus.fromValue("failed"));
        assertEquals(AiStatus.UNAVAILABLE, AiStatus.fromValue(null));
        assertEquals(AiStatus.UNAVAILABLE, AiStatus.fromValue("future_status"));
    }
}
