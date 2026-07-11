package com.example.fonos_group13.data.community;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import com.example.fonos_group13.model.BookReviewPage;
import com.example.fonos_group13.model.ReviewMutationResult;
import com.example.fonos_group13.model.SaveMutationResult;

import org.junit.Test;

public class CommunityApiContractTest {
    @Test
    public void serializesTrimmedReviewInput() throws Exception {
        assertEquals("{\"rating\":5,\"comment\":\"Great\"}", CommunityApiContract.reviewJson(5, " Great "));
        assertEquals("{\"rating\":4,\"comment\":null}", CommunityApiContract.reviewJson(4, "   "));
    }

    @Test
    public void parsesReviewPageAndViewerRatingOnlyReview() throws Exception {
        BookReviewPage page = CommunityApiContract.parseReviewPage(200, "{\"data\":{" +
                "\"reviews\":[{\"reviewerDisplayName\":\"Alice\",\"rating\":5,\"comment\":\"Great\"," +
                "\"createdAt\":\"2026-01-01T00:00:00.000Z\",\"updatedAt\":\"2026-01-01T00:00:00.000Z\",\"edited\":false}]," +
                "\"viewerReview\":{\"reviewerDisplayName\":\"Viewer\",\"rating\":4,\"comment\":null," +
                "\"createdAt\":null,\"updatedAt\":null,\"edited\":false}," +
                "\"nextCursor\":\"next\",\"hasMore\":true}}");

        assertEquals(1, page.getReviews().size());
        assertEquals("Alice", page.getReviews().get(0).getReviewerDisplayName());
        assertNull(page.getViewerReview().getComment());
        assertEquals("next", page.getNextCursor());
    }

    @Test
    public void parsesReviewAndSaveMutationAggregates() throws Exception {
        ReviewMutationResult review = CommunityApiContract.parseReviewMutation(200, "{\"data\":{" +
                "\"review\":{\"reviewerDisplayName\":\"Reader\",\"rating\":3,\"comment\":null," +
                "\"createdAt\":null,\"updatedAt\":null,\"edited\":false}," +
                "\"ratingAverage\":3.5,\"ratingCount\":2}}");
        SaveMutationResult save = CommunityApiContract.parseSaveMutation(200,
                "{\"data\":{\"saved\":false,\"saveCount\":8}}");

        assertEquals(3.5, review.getRatingAverage(), 0.001);
        assertEquals(2, review.getRatingCount());
        assertFalse(save.isSaved());
        assertEquals(8, save.getSaveCount());
    }
}
