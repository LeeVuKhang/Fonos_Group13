package com.example.fonos_group13.data;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.example.fonos_group13.model.Book;
import com.example.fonos_group13.model.BookChapter;

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
        return isDownloaded(bookId, BookChapter.LEGACY_CHAPTER_ID);
    }

    public boolean isDownloaded(String bookId, String chapterId) {
        File audioFile = getAudioFile(bookId, chapterId);
        if (audioFile.exists() && audioFile.length() > 0) {
            return true;
        }
        File legacyFile = getLegacyAudioFile(bookId, chapterId);
        return legacyFile != null && legacyFile.exists() && legacyFile.length() > 0;
    }

    public Uri getDownloadedUri(String bookId) {
        return getDownloadedUri(bookId, BookChapter.LEGACY_CHAPTER_ID);
    }

    public Uri getDownloadedUri(String bookId, String chapterId) {
        File audioFile = getAudioFile(bookId, chapterId);
        if (audioFile.exists() && audioFile.length() > 0) {
            return Uri.fromFile(audioFile);
        }
        File legacyFile = getLegacyAudioFile(bookId, chapterId);
        if (legacyFile != null && legacyFile.exists() && legacyFile.length() > 0) {
            return Uri.fromFile(legacyFile);
        }
        return null;
    }

    public long getDownloadedSizeBytes(String bookId) {
        return getDownloadedSizeBytes(bookId, BookChapter.LEGACY_CHAPTER_ID);
    }

    public long getDownloadedSizeBytes(String bookId, String chapterId) {
        File audioFile = getAudioFile(bookId, chapterId);
        if (audioFile.exists()) {
            return Math.max(audioFile.length(), 0);
        }
        File legacyFile = getLegacyAudioFile(bookId, chapterId);
        return legacyFile != null && legacyFile.exists() ? Math.max(legacyFile.length(), 0) : 0;
    }

    public boolean deleteDownloadedAudio(String bookId) {
        return deleteDownloadedAudio(bookId, BookChapter.LEGACY_CHAPTER_ID);
    }

    public boolean deleteDownloadedAudio(String bookId, String chapterId) {
        boolean deleted = deleteIfExists(getAudioFile(bookId, chapterId));
        File legacyFile = getLegacyAudioFile(bookId, chapterId);
        return legacyFile == null ? deleted : deleted && deleteIfExists(legacyFile);
    }

    public void download(Book book, RepositoryCallback<File> callback) {
        download(book, BookChapter.fromLegacyBook(book), callback);
    }

    public void download(Book book, BookChapter chapter, RepositoryCallback<File> callback) {
        new Thread(() -> {
            try {
                File downloadedFile = downloadBlocking(book, chapter);
                callback.onSuccess(downloadedFile);
            } catch (Exception exception) {
                callback.onError(exception);
            }
        }).start();
    }

    private File downloadBlocking(Book book, BookChapter chapter) throws IOException {
        if (book == null || TextUtils.isEmpty(book.getId())) {
            throw new IOException("Book is missing.");
        }
        if (chapter == null || TextUtils.isEmpty(chapter.getId())) {
            throw new IOException("Chapter is missing.");
        }
        String audioUrl = trimToNull(chapter.getAudioUrl());
        if (audioUrl == null) {
            throw new IOException("This chapter does not have an audioUrl to download.");
        }
        if (!audioDir.exists() && !audioDir.mkdirs()) {
            throw new IOException("Could not create audio download folder.");
        }

        File targetFile = getAudioFile(book.getId(), chapter.getId());
        File tempFile = new File(audioDir, sanitizeAudioId(book.getId(), chapter.getId()) + ".tmp");
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
        return getAudioFile(bookId, BookChapter.LEGACY_CHAPTER_ID);
    }

    private File getAudioFile(String bookId, String chapterId) {
        return new File(audioDir, sanitizeAudioId(bookId, chapterId) + ".mp3");
    }

    private File getLegacyAudioFile(String bookId, String chapterId) {
        if (!BookChapter.LEGACY_CHAPTER_ID.equals(chapterId)) {
            return null;
        }
        return new File(audioDir, sanitizeBookId(bookId) + ".mp3");
    }

    private boolean deleteIfExists(File audioFile) {
        return !audioFile.exists() || audioFile.delete();
    }

    private String sanitizeAudioId(String bookId, String chapterId) {
        return sanitizeBookId(bookId) + "__" + sanitizeBookId(chapterId);
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
