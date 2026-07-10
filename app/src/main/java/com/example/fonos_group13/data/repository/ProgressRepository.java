package com.example.fonos_group13.data.repository;

import com.example.fonos_group13.data.core.RepositoryCallback;
import com.example.fonos_group13.model.UserProgress;

public interface ProgressRepository {
    void getProgress(String bookId, RepositoryCallback<UserProgress> callback);

    void getProgress(String bookId, String chapterId, RepositoryCallback<UserProgress> callback);

    void saveProgress(String bookId, long positionMs, long durationMs);

    void saveProgress(String bookId, String chapterId, long positionMs, long durationMs);
}
