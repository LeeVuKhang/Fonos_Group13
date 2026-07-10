package com.example.fonos_group13;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.fonos_group13.notifications.GenerationNotificationHelper;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class MyUploadsLiveNotificationConfigurationTest {
    @Test
    public void myUploadsUsesFirestoreSnapshotsAndRemovesTheListener() throws Exception {
        String activity = readFile("src/main/java/com/example/fonos_group13/MyUploadsActivity.java");
        String repository = readFile("src/main/java/com/example/fonos_group13/data/creator/FirestoreCreatorUploadsRepository.java");
        String controller = readFile("src/main/java/com/example/fonos_group13/controller/creator/MyUploadsController.java");

        assertTrue(repository.contains("FirestoreSubscription"));
        assertTrue(repository.contains("observeMyUploads"));
        assertTrue(repository.contains("addSnapshotListener"));
        assertTrue(activity.contains("protected void onStart()"));
        assertTrue(activity.contains("uploadsController.start()"));
        assertTrue(activity.contains("protected void onStop()"));
        assertTrue(controller.contains("uploadsSubscription.cancel()"));
        assertFalse(activity.contains("protected void onResume()"));
        assertFalse(activity.contains("loadUploads();"));
    }

    @Test
    public void myUploadsRegistersFcmAndDeclaresNotificationSupport() throws Exception {
        String activity = readFile("src/main/java/com/example/fonos_group13/MyUploadsActivity.java");
        String createActivity = readFile("src/main/java/com/example/fonos_group13/CreateAudiobookActivity.java");
        String manageActivity = readFile("src/main/java/com/example/fonos_group13/ManageChapterActivity.java");
        String manifest = readFile("src/main/AndroidManifest.xml");
        String setup = readFile(
                "src/main/java/com/example/fonos_group13/notifications/GenerationNotificationSetup.java"
        );
        String notificationHelper = readFile(
                "src/main/java/com/example/fonos_group13/notifications/GenerationNotificationHelper.java"
        );
        String tokenRepository = readFile(
                "src/main/java/com/example/fonos_group13/data/notification/UploadNotificationTokenRepository.java"
        );

        assertTrue(activity.contains("GenerationNotificationSetup"));
        assertTrue(activity.contains("notificationSetup.ensureReady()"));
        assertTrue(createActivity.contains("GenerationNotificationSetup"));
        assertTrue(createActivity.contains("ensureGenerationNotifications();"));
        assertTrue(manageActivity.contains("GenerationNotificationSetup"));
        assertTrue(manageActivity.contains("ensureGenerationNotifications();"));
        assertTrue(setup.contains("registerCurrentDevice"));
        assertTrue(setup.contains("Manifest.permission.POST_NOTIFICATIONS"));
        assertTrue(setup.contains("requestPermissions"));
        assertTrue(notificationHelper.contains("NotificationManager.IMPORTANCE_HIGH"));
        assertTrue(activity.contains("AudiobookGenerationStatus.PENDING_GENERATION"));
        assertTrue(manifest.contains("android.permission.POST_NOTIFICATIONS"));
        assertTrue(manifest.contains(".notifications.GenerationNotificationMessagingService"));
        assertTrue(manifest.contains("com.google.firebase.MESSAGING_EVENT"));
        assertTrue(tokenRepository.contains("notificationTokens"));
        assertTrue(tokenRepository.contains("SHA-256"));
    }

    @Test
    public void myUploadsLetsUsersEditSavedDrafts() throws Exception {
        String activity = readFile("src/main/java/com/example/fonos_group13/MyUploadsActivity.java");
        String createActivity = readFile("src/main/java/com/example/fonos_group13/CreateAudiobookActivity.java");

        assertTrue(activity.contains("Edit Draft"));
        assertTrue(activity.contains("CreateAudiobookActivity.EXTRA_EDIT_BOOK_ID"));
        assertTrue(createActivity.contains("updateDraft("));
        assertTrue(createActivity.contains("updateDraftAndRequestGeneration("));
        assertTrue(createActivity.contains("Save Changes"));
    }

    @Test
    public void myUploadsSupportsChapterManagementAndVisibilityToggle() throws Exception {
        String activity = readFile("src/main/java/com/example/fonos_group13/MyUploadsActivity.java");
        String repository = readFile("src/main/java/com/example/fonos_group13/data/creator/CreatorAudiobookRepository.java");
        String uploadsRepository = readFile("src/main/java/com/example/fonos_group13/data/creator/FirestoreCreatorUploadsRepository.java");

        assertTrue(uploadsRepository.contains("observeUploadChapters"));
        assertTrue(uploadsRepository.contains("CreatorUploadDocumentMapper.isDeletedChapter"));
        assertTrue(repository.contains("setAudiobookVisibility"));
        assertTrue(repository.contains("deleteChapter"));
        assertTrue(activity.contains("\"Preview\""));
        assertTrue(activity.contains("\"Edit\""));
        assertTrue(activity.contains("\"Request\""));
        assertTrue(activity.contains("\"Cancel Chapter\""));
        assertTrue(activity.contains("\"Make Public\""));
        assertTrue(activity.contains("\"Make Private\""));
        assertTrue(activity.contains("PopupMenu"));
        assertTrue(activity.contains("showChapterOverflowMenu"));
        assertTrue(activity.contains("repository.deleteChapter"));
    }

    @Test
    public void chapterPreviewOpensSingleChapterReaderInsteadOfDetailList() throws Exception {
        String activity = readFile("src/main/java/com/example/fonos_group13/MyUploadsActivity.java");
        String reader = readFile("src/main/java/com/example/fonos_group13/ActivityReader.java");
        String chapterActions = sectionBetween(
                activity,
                "private View createChapterActions",
                "private View createChapterOverflowButton"
        );
        String previewHelper = sectionBetween(
                activity,
                "private void openChapterPreview",
                "private void openDraftEditor"
        );

        assertTrue(chapterActions.contains("\"Preview\""));
        assertTrue(chapterActions.contains("openChapterPreview(upload, chapter)"));
        assertFalse(chapterActions.contains("openBookDetail(upload, true)"));
        assertFalse(chapterActions.contains("BookDetailActivity.class"));
        assertTrue(previewHelper.contains("new Intent(this, ActivityReader.class)"));
        assertTrue(previewHelper.contains("ActivityReader.EXTRA_BOOK_ID"));
        assertTrue(previewHelper.contains("upload.getId()"));
        assertTrue(previewHelper.contains("ActivityReader.EXTRA_CHAPTER_ID"));
        assertTrue(previewHelper.contains("chapter.getId()"));
        assertTrue(previewHelper.contains("ActivityReader.EXTRA_SINGLE_CHAPTER_PREVIEW"));
        assertTrue(reader.contains("EXTRA_SINGLE_CHAPTER_PREVIEW"));
        assertTrue(reader.contains("if (requestedSingleChapterPreview)"));
        assertTrue(reader.contains("currentChapters.add(selectedChapter);"));
    }

    @Test
    public void myUploadsPlacesAddChapterInChapterHeaderAndUsesLighterChapterUi() throws Exception {
        String activity = readFile("src/main/java/com/example/fonos_group13/MyUploadsActivity.java");
        String chapterRow = readFile("src/main/res/drawable/bg_upload_chapter_row.xml");
        String strings = readFile("src/main/res/values/strings.xml");
        String chapterPanel = sectionBetween(
                activity,
                "private View createChapterPanel",
                "private View createChapterRow"
        );
        String previewButton = sectionBetween(
                activity,
                "private MaterialButton createChapterPreviewButton",
                "private View createChapterOverflowButton"
        );
        String addChapterButton = sectionBetween(
                activity,
                "private MaterialButton createAddChapterButton",
                "private MaterialButton createOutlineActionButton"
        );

        assertTrue(chapterPanel.contains("headingRow.addView(createAddChapterButton(upload), addChapterParams)"));
        assertTrue(chapterPanel.contains("formatChapterSummary(chapters)"));
        assertFalse(chapterPanel.contains("fullWidthButtonParams"));
        assertTrue(addChapterButton.contains("R.string.my_uploads_add_chapter"));
        assertTrue(addChapterButton.contains("openAddChapter(upload)"));
        assertTrue(previewButton.contains("\"Preview\""));
        assertTrue(previewButton.contains("R.drawable.ic_play"));
        assertTrue(previewButton.contains("createOutlineActionButton"));
        assertTrue(chapterRow.contains("@color/surface_card") || chapterRow.contains("@color/surface"));
        assertTrue(chapterRow.contains("@color/stroke_soft"));
        assertFalse(chapterRow.contains("@color/surface_panel"));
        assertTrue(strings.contains("name=\"my_uploads_add_chapter\""));
    }

    @Test
    public void myUploadsPublishesReadyChapterUpdatesInline() throws Exception {
        String activity = readFile("src/main/java/com/example/fonos_group13/MyUploadsActivity.java");
        String chapterActions = sectionBetween(
                activity,
                "private View createChapterActions",
                "private MaterialButton createChapterPreviewButton"
        );
        String publishGate = sectionBetween(
                activity,
                "private boolean canPublishChapter",
                "private View createChapterOverflowButton"
        );
        String publishHelper = sectionBetween(
                activity,
                "private void publishUploadUpdate",
                "private void requestChapterGeneration"
        );
        String primaryAction = sectionBetween(
                activity,
                "private View createPrimaryActionButton",
                "private View createVisibilityButton"
        );

        assertTrue(chapterActions.contains("canPublishChapter(chapter)"));
        assertTrue(chapterActions.contains("\"Publish\""));
        assertTrue(chapterActions.contains("\"Publishing...\""));
        assertTrue(chapterActions.contains("publishUploadUpdate(upload)"));
        assertTrue(publishGate.contains("AudiobookGenerationStatus.READY_FOR_REVIEW"));
        assertTrue(publishGate.contains("chapter.canPreview()"));
        assertTrue(publishHelper.contains("publishingBookId = upload.getId()"));
        assertTrue(publishHelper.contains("repository.publishAudiobook(upload.getId()"));
        assertTrue(publishHelper.contains("\"Updates published.\""));
        assertFalse(primaryAction.contains("\"Review Updates\""));
        assertTrue(primaryAction.contains("\"Preview Audiobook\""));
        assertTrue(activity.contains("publishingBookIdActive()"));
    }

    @Test
    public void uploadStatusChipsUseDisplayLabels() throws Exception {
        String activity = readFile("src/main/java/com/example/fonos_group13/MyUploadsActivity.java");
        String status = readFile("src/main/java/com/example/fonos_group13/model/AudiobookGenerationStatus.java");

        assertTrue(activity.contains("status.getDisplayLabel()"));
        assertTrue(status.contains("Pending Generation"));
        assertTrue(status.contains("Ready for Review"));
    }

    @Test
    public void myUploadsUsesVisualCoverCardsAndDedicatedBackIcon() throws Exception {
        String activity = readFile("src/main/java/com/example/fonos_group13/MyUploadsActivity.java");
        String layout = readFile("src/main/res/layout/activity_my_uploads.xml");

        assertTrue(activity.contains("ShapeableImageView"));
        assertTrue(activity.contains("upload.getCoverUrl()"));
        assertTrue(activity.contains("Glide.with(cover)"));
        assertTrue(activity.contains("R.drawable.bg_cover_placeholder"));
        assertTrue(layout.contains("@+id/uploads_state_panel"));
        assertTrue(layout.contains("@+id/uploads_state_action"));
        assertTrue(layout.contains("@+id/btn_create_upload"));
        assertTrue(layout.contains("+ Create New"));
        assertTrue(layout.contains("+ Create New Audiobook"));
        assertTrue(layout.contains("@drawable/ic_arrow_back"));
        assertFalse(layout.contains("@drawable/ic_logout"));
    }

    @Test
    public void myUploadsUsesClearActionAndProgressLabels() throws Exception {
        String activity = readFile("src/main/java/com/example/fonos_group13/MyUploadsActivity.java");

        assertTrue(activity.contains("Preview Audiobook"));
        assertTrue(activity.contains("Make Public"));
        assertTrue(activity.contains("Make Private"));
        assertTrue(activity.contains("formatVisibilityLabel"));
        assertTrue(activity.contains("formatVoiceLabel"));
        assertTrue(activity.contains("formatChapterProgress"));
        assertTrue(activity.contains("formatChapterSummary"));
        assertTrue(activity.contains("ic_more_vert"));
        assertFalse(activity.contains("Review Updates"));
        assertFalse(activity.contains("Preview Updates"));
        assertFalse(activity.contains("Show Publicly"));
        assertFalse(activity.contains("Hidden from public"));
    }

    @Test
    public void generationNotificationTextHandlesReadyAndFailedPayloads() {
        Map<String, String> payload = new HashMap<>();
        payload.put("type", "audiobook_generation_status");
        payload.put("bookId", "book-1");
        payload.put("generationStatus", "ready_for_review");
        payload.put("title", "Demo Book");
        payload.put("clickTarget", "my_uploads");

        assertTrue(GenerationNotificationHelper.isGenerationStatusPayload(payload));
        assertEquals(
                "Audiobook ready for review",
                GenerationNotificationHelper.notificationTitle("ready_for_review")
        );
        assertEquals(
                "Demo Book is ready to preview.",
                GenerationNotificationHelper.notificationBody("ready_for_review", "Demo Book")
        );

        payload.put("generationStatus", "failed");
        assertTrue(GenerationNotificationHelper.isGenerationStatusPayload(payload));
        assertEquals(
                "Audiobook generation failed",
                GenerationNotificationHelper.notificationTitle("failed")
        );
        assertEquals(
                "Demo Book could not be generated. Open My Uploads to retry.",
                GenerationNotificationHelper.notificationBody("failed", "Demo Book")
        );
    }

    private String readFile(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }

    private String sectionBetween(String source, String startMarker, String endMarker) {
        int start = source.indexOf(startMarker);
        assertTrue("Missing start marker: " + startMarker, start >= 0);
        int end = source.indexOf(endMarker, start + startMarker.length());
        assertTrue("Missing end marker: " + endMarker, end > start);
        return source.substring(start, end);
    }
}
