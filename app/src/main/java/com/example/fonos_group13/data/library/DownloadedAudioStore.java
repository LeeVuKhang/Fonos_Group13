package com.example.fonos_group13.data.library;

import android.net.Uri;

import com.example.fonos_group13.model.BookChapter;

import java.io.File;

public final class DownloadedAudioStore {
    private final File audioDirectory;

    public DownloadedAudioStore(File audioDirectory) {
        this.audioDirectory = audioDirectory;
    }

    public boolean ensureDirectory() {
        return audioDirectory.exists() || audioDirectory.mkdirs();
    }

    public File audioFile(String bookId, String chapterId) {
        return new File(audioDirectory, sanitize(bookId) + "__" + sanitize(chapterId) + ".mp3");
    }

    public File temporaryFile(String bookId, String chapterId) {
        return new File(
                audioDirectory,
                sanitize(bookId) + "__" + sanitize(chapterId) + "__" + System.nanoTime() + ".tmp"
        );
    }

    public boolean isDownloaded(String bookId, String chapterId) {
        File target = audioFile(bookId, chapterId);
        if (target.exists() && target.length() > 0) {
            return true;
        }
        File legacy = legacyFile(bookId, chapterId);
        return legacy != null && legacy.exists() && legacy.length() > 0;
    }

    public Uri downloadedUri(String bookId, String chapterId) {
        File target = audioFile(bookId, chapterId);
        if (target.exists() && target.length() > 0) {
            return Uri.fromFile(target);
        }
        File legacy = legacyFile(bookId, chapterId);
        return legacy != null && legacy.exists() && legacy.length() > 0 ? Uri.fromFile(legacy) : null;
    }

    public long downloadedSize(String bookId, String chapterId) {
        File target = audioFile(bookId, chapterId);
        if (target.exists()) {
            return Math.max(target.length(), 0);
        }
        File legacy = legacyFile(bookId, chapterId);
        return legacy != null && legacy.exists() ? Math.max(legacy.length(), 0) : 0;
    }

    public boolean delete(String bookId, String chapterId) {
        boolean deleted = deleteIfExists(audioFile(bookId, chapterId));
        File legacy = legacyFile(bookId, chapterId);
        return legacy == null ? deleted : deleted && deleteIfExists(legacy);
    }

    public synchronized boolean replaceFromTemp(File temporaryFile, File targetFile) {
        if (temporaryFile == null || !temporaryFile.exists() || temporaryFile.length() <= 0) {
            return false;
        }
        File backup = new File(targetFile.getParentFile(), targetFile.getName() + ".bak");
        deleteIfExists(backup);
        boolean hadTarget = targetFile.exists();
        if (hadTarget && !targetFile.renameTo(backup)) {
            return false;
        }
        if (temporaryFile.renameTo(targetFile)) {
            deleteIfExists(backup);
            return true;
        }
        if (hadTarget) {
            backup.renameTo(targetFile);
        }
        return false;
    }

    private File legacyFile(String bookId, String chapterId) {
        if (!BookChapter.LEGACY_CHAPTER_ID.equals(chapterId)) {
            return null;
        }
        return new File(audioDirectory, sanitize(bookId) + ".mp3");
    }

    private boolean deleteIfExists(File file) {
        return file == null || !file.exists() || file.delete();
    }

    private String sanitize(String value) {
        if (value == null) {
            return "book";
        }
        String safe = value.replaceAll("[^a-zA-Z0-9_-]", "_");
        return safe.isEmpty() ? "book" : safe;
    }
}
