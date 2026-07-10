package com.example.fonos_group13;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class CreatorPublishConfigurationTest {
    @Test
    public void reviewPreviewHasPublishActionBackedByCreatorRepository() throws Exception {
        String activity = readFile("src/main/java/com/example/fonos_group13/BookDetailActivity.java");
        String layout = readFile("src/main/res/layout/activity_book_detail.xml");
        String repository = readFile("src/main/java/com/example/fonos_group13/data/creator/CreatorAudiobookRepository.java");
        String apiClient = readFile("src/main/java/com/example/fonos_group13/data/creator/CreatorApiClient.java");

        assertTrue(layout.contains("btn_publish_audiobook"));
        assertTrue(activity.contains("publishCurrentBook()"));
        assertTrue(activity.contains("creatorPreviewActive"));
        assertTrue(activity.contains("creatorPreviewRequested && isCurrentCreator(book)"));
        assertTrue(activity.contains("canPublishCurrentBook()"));
        assertTrue(activity.contains("currentBook.getGenerationStatus() == AudiobookGenerationStatus.READY_FOR_REVIEW"));
        assertTrue(activity.contains("dataController.isCurrentCreator(book)"));
        assertTrue(activity.contains("dataController.publish(bookId"));
        assertTrue(repository.contains("publishAudiobook(String bookId"));
        assertTrue(apiClient.contains("/publications"));
    }

    private String readFile(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
