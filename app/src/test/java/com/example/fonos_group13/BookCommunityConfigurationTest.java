package com.example.fonos_group13;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class BookCommunityConfigurationTest {
    @Test
    public void bookDetailExposesReviewComposerAndPaginationControls() throws Exception {
        String layout = read("src/main/res/layout/activity_book_detail.xml");
        String activity = read("src/main/java/com/example/fonos_group13/BookDetailActivity.java");

        assertTrue(layout.contains("@+id/detail_rating_summary"));
        assertTrue(layout.contains("@+id/detail_save_count"));
        assertTrue(layout.contains("@+id/review_rating_input"));
        assertTrue(layout.contains("@+id/review_comment_input"));
        assertTrue(layout.contains("@+id/btn_load_more_reviews"));
        assertTrue(activity.contains("dataController.upsertReview"));
        assertTrue(activity.contains("dataController.deleteReview"));
    }

    @Test
    public void everyExistingBookListBindsRatingMetadata() throws Exception {
        for (String activity : new String[]{"DiscoverActivity", "SearchActivity", "LibraryActivity"}) {
            assertTrue(activity + " is missing rating metadata",
                    read("src/main/java/com/example/fonos_group13/" + activity + ".java")
                            .contains("BookRatingBinder.bind"));
        }
    }

    private String read(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
