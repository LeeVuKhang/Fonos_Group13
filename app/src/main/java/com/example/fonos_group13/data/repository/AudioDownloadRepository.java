package com.example.fonos_group13.data.repository;

import android.net.Uri;

import com.example.fonos_group13.data.core.RepositoryCallback;
import com.example.fonos_group13.data.core.RequestHandle;
import com.example.fonos_group13.model.Book;
import com.example.fonos_group13.model.BookChapter;

import java.io.File;

public interface AudioDownloadRepository {
    boolean isDownloaded(String bookId);

    boolean isDownloaded(String bookId, String chapterId);

    Uri getDownloadedUri(String bookId);

    Uri getDownloadedUri(String bookId, String chapterId);

    long getDownloadedSizeBytes(String bookId);

    long getDownloadedSizeBytes(String bookId, String chapterId);

    boolean deleteDownloadedAudio(String bookId);

    boolean deleteDownloadedAudio(String bookId, String chapterId);

    RequestHandle download(Book book, RepositoryCallback<File> callback);

    RequestHandle download(Book book, BookChapter chapter, RepositoryCallback<File> callback);
}
