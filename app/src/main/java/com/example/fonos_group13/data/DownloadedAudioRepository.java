package com.example.fonos_group13.data;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.example.fonos_group13.model.Book;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadedAudioRepository {
    private static final String AUDIO_DIR_NAME = "audiobooks";
    private static final int CONNECT_TIMEOUT_MS = 15000;
    private static final int READ_TIMEOUT_MS = 30000;

    private final File audioDir;

    public DownloadedAudioRepository(Context context) {
        audioDir = new File(context.getApplicationContext().getFilesDir(), AUDIO_DIR_NAME);
    }

    public boolean isDownloaded(String bookId) {
        File audioFile = getAudioFile(bookId);
        return audioFile.exists() && audioFile.length() > 0;
    }

    public Uri getDownloadedUri(String bookId) {
        if (!isDownloaded(bookId)) {
            return null;
        }
        return Uri.fromFile(getAudioFile(bookId));
    }

    public long getDownloadedSizeBytes(String bookId) {
        File audioFile = getAudioFile(bookId);
        return audioFile.exists() ? Math.max(audioFile.length(), 0) : 0;
    }

    public boolean deleteDownloadedAudio(String bookId) {
        File audioFile = getAudioFile(bookId);
        return !audioFile.exists() || audioFile.delete();
    }

    public void download(Book book, RepositoryCallback<File> callback) {
        new Thread(() -> {
            try {
                File downloadedFile = downloadBlocking(book);
                callback.onSuccess(downloadedFile);
            } catch (Exception exception) {
                callback.onError(exception);
            }
        }).start();
    }

    private File downloadBlocking(Book book) throws IOException {
        if (book == null || TextUtils.isEmpty(book.getId())) {
            throw new IOException("Book is missing.");
        }
        String audioUrl = trimToNull(book.getAudioUrl());
        if (audioUrl == null) {
            throw new IOException("This book does not have an audioUrl to download.");
        }
        if (!audioDir.exists() && !audioDir.mkdirs()) {
            throw new IOException("Could not create audio download folder.");
        }

        File targetFile = getAudioFile(book.getId());
        File tempFile = new File(audioDir, sanitizeBookId(book.getId()) + ".tmp");
        HttpURLConnection connection = (HttpURLConnection) new URL(audioUrl).openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setInstanceFollowRedirects(true);

        try {
            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                throw new IOException("Download failed with HTTP " + responseCode + ".");
            }

            try (InputStream input = connection.getInputStream();
                 FileOutputStream output = new FileOutputStream(tempFile, false)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
            }

            if (tempFile.length() == 0) {
                throw new IOException("Downloaded audio file is empty.");
            }
            if (targetFile.exists() && !targetFile.delete()) {
                throw new IOException("Could not replace existing audio file.");
            }
            if (!tempFile.renameTo(targetFile)) {
                throw new IOException("Could not save downloaded audio file.");
            }
            return targetFile;
        } finally {
            connection.disconnect();
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    private File getAudioFile(String bookId) {
        return new File(audioDir, sanitizeBookId(bookId) + ".mp3");
    }

    private String sanitizeBookId(String bookId) {
        if (bookId == null) {
            return "book";
        }
        String safeId = bookId.replaceAll("[^a-zA-Z0-9_-]", "_");
        return safeId.isEmpty() ? "book" : safeId;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
