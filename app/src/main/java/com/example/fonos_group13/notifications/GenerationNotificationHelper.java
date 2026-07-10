package com.example.fonos_group13.notifications;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.fonos_group13.MyUploadsActivity;
import com.example.fonos_group13.R;

import java.util.Map;

public final class GenerationNotificationHelper {
    public static final String CHANNEL_ID = "audiobook_generation";
    public static final String TYPE_GENERATION_STATUS = "audiobook_generation_status";
    public static final String CLICK_TARGET_MY_UPLOADS = "my_uploads";

    private static final int NOTIFICATION_ID_BASE = 21000;
    private static final String KEY_TYPE = "type";
    private static final String KEY_BOOK_ID = "bookId";
    private static final String KEY_STATUS = "generationStatus";
    private static final String KEY_TITLE = "title";
    private static final String STATUS_READY_FOR_REVIEW = "ready_for_review";
    private static final String STATUS_FAILED = "failed";

    private GenerationNotificationHelper() {
    }

    public static boolean isGenerationStatusPayload(Map<String, String> data) {
        if (data == null) {
            return false;
        }
        String type = data.get(KEY_TYPE);
        String status = data.get(KEY_STATUS);
        return TYPE_GENERATION_STATUS.equals(type)
                && (STATUS_READY_FOR_REVIEW.equals(status) || STATUS_FAILED.equals(status));
    }

    public static String notificationTitle(String generationStatus) {
        if (STATUS_READY_FOR_REVIEW.equals(generationStatus)) {
            return "Audiobook ready for review";
        }
        if (STATUS_FAILED.equals(generationStatus)) {
            return "Audiobook generation failed";
        }
        return "Audiobook generation updated";
    }

    public static String notificationBody(String generationStatus, String audiobookTitle) {
        String title = trimToNull(audiobookTitle);
        if (STATUS_READY_FOR_REVIEW.equals(generationStatus)) {
            return title == null
                    ? "Your audiobook is ready to preview."
                    : title + " is ready to preview.";
        }
        if (STATUS_FAILED.equals(generationStatus)) {
            return title == null
                    ? "Your audiobook could not be generated. Open My Uploads to retry."
                    : title + " could not be generated. Open My Uploads to retry.";
        }
        return title == null
                ? "Open My Uploads to see the latest status."
                : "Open My Uploads to see the latest status for " + title + ".";
    }

    public static void showGenerationStatusNotification(Context context, Map<String, String> data) {
        if (context == null || !isGenerationStatusPayload(data) || !canPostNotifications(context)) {
            return;
        }

        createChannel(context);
        String status = data.get(KEY_STATUS);
        String bookId = data.get(KEY_BOOK_ID);
        String title = data.get(KEY_TITLE);
        Intent intent = new Intent(context, MyUploadsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId(bookId),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_book_open)
                .setContentTitle(notificationTitle(status))
                .setContentText(notificationBody(status, title))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(notificationBody(status, title)))
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        try {
            NotificationManagerCompat.from(context).notify(notificationId(bookId), builder.build());
        } catch (SecurityException ignored) {
            // Permission can be revoked between the explicit check and the notification call.
        }
    }

    private static boolean canPostNotifications(Context context) {
        return Build.VERSION.SDK_INT < 33
                || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT < 26) {
            return;
        }
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        if (notificationManager == null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.generation_notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
        );
        notificationManager.createNotificationChannel(channel);
    }

    private static int notificationId(String bookId) {
        String safeBookId = trimToNull(bookId);
        if (safeBookId == null) {
            return NOTIFICATION_ID_BASE;
        }
        return NOTIFICATION_ID_BASE + Math.floorMod(safeBookId.hashCode(), 100000);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
