package com.example.fonos_group13.data.firestore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.firebase.Timestamp;

import org.junit.Test;

public class FirestoreValueReaderTest {
    @Test
    public void malformedValuesFallBackWithoutThrowing() {
        assertNull(FirestoreValueReader.stringValue(42));
        assertEquals(0L, FirestoreValueReader.longValue("not-a-number"));
        assertFalse(FirestoreValueReader.booleanValue("not-a-boolean", false));
        assertEquals(0L, FirestoreValueReader.timestampMillisValue("yesterday"));
    }

    @Test
    public void compatibleValuesAreCoercedSafely() {
        Timestamp timestamp = new Timestamp(123, 0);

        assertEquals("Title", FirestoreValueReader.stringValue("  Title  "));
        assertEquals(12L, FirestoreValueReader.longValue(12.8));
        assertTrue(FirestoreValueReader.booleanValue("true", false));
        assertEquals(timestamp.toDate().getTime(), FirestoreValueReader.timestampMillisValue(timestamp));
    }
}
