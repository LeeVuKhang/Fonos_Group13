package com.example.fonos_group13.data;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BackendApiExceptionTest {
    @Test
    public void validationMessageForFieldReadsBackendDetailsArray() {
        BackendApiException exception = new BackendApiException(
                422,
                "validation_error",
                "Request validation failed",
                "[{\"field\":\"chapterText\",\"message\":\"Chapter text must be 3500 words or fewer\"}]"
        );

        assertEquals(
                "Chapter text must be 3500 words or fewer",
                exception.getValidationMessageForField("chapterText")
        );
    }

    @Test
    public void validationMessageForFieldReadsNestedDetails() {
        BackendApiException exception = new BackendApiException(
                422,
                "validation_error",
                "Request validation failed",
                "{\"details\":[{\"path\":\"data.chapterText\",\"message\":\"Too long\"}]}"
        );

        assertEquals("Too long", exception.getValidationMessageForField("chapterText"));
    }

    @Test
    public void validationMessageForFieldIgnoresOtherFieldsAndMalformedDetails() {
        BackendApiException otherField = new BackendApiException(
                422,
                "validation_error",
                "Request validation failed",
                "[{\"field\":\"title\",\"message\":\"Title is required\"}]"
        );
        BackendApiException malformed = new BackendApiException(
                422,
                "validation_error",
                "Request validation failed",
                "chapterText: Too long"
        );

        assertEquals(null, otherField.getValidationMessageForField("chapterText"));
        assertEquals(null, malformed.getValidationMessageForField("chapterText"));
    }
}
