package com.example.fonos_group13;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ActivityReaderPreviewConfigurationTest {
    @Test
    public void readerKeepsCreatorPreviewForPublishedBooksReadyForReview() throws Exception {
        String reader = readFile("src/main/java/com/example/fonos_group13/ActivityReader.java");

        assertTrue(reader.contains("book.getGenerationStatus() == AudiobookGenerationStatus.READY_FOR_REVIEW"));
        assertTrue(reader.contains("creatorPreview == creatorPreviewActive"));
        assertTrue(reader.contains("BookAccessMode accessMode = creatorPreviewActive"));
        assertFalse(reader.contains("loadingCreatorPreview && !book.isPublished()"));
        assertFalse(reader.contains("currentBook.isPublished() || creatorPreview == creatorPreviewActive"));
    }

    private String readFile(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
