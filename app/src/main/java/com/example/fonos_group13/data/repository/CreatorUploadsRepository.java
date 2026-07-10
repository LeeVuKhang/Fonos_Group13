package com.example.fonos_group13.data.repository;

import com.example.fonos_group13.data.core.RepositoryCallback;
import com.example.fonos_group13.data.core.Subscription;
import com.example.fonos_group13.model.UserGeneratedAudiobook;
import com.example.fonos_group13.model.UserGeneratedChapter;

import java.util.List;

public interface CreatorUploadsRepository {
    void getMyUploads(RepositoryCallback<List<UserGeneratedAudiobook>> callback);

    Subscription observeMyUploads(RepositoryCallback<List<UserGeneratedAudiobook>> callback);

    Subscription observeUploadChapters(
            String bookId,
            RepositoryCallback<List<UserGeneratedChapter>> callback
    );
}
