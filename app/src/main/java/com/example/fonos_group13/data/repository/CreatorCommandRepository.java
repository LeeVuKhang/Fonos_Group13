package com.example.fonos_group13.data.repository;

import com.example.fonos_group13.data.core.RepositoryCallback;
import com.example.fonos_group13.model.CreateAudiobookDraftInput;
import com.example.fonos_group13.model.CreateChapterDraftInput;
import com.example.fonos_group13.model.EditableAudiobookDraft;
import com.example.fonos_group13.model.EditableChapterDraft;

public interface CreatorCommandRepository {
    void createDraft(CreateAudiobookDraftInput input, RepositoryCallback<String> callback);

    void createDraftAndRequestGeneration(CreateAudiobookDraftInput input, RepositoryCallback<String> callback);

    void getDraftForEdit(String bookId, RepositoryCallback<EditableAudiobookDraft> callback);

    void updateDraft(String bookId, CreateAudiobookDraftInput input, RepositoryCallback<String> callback);

    void updateDraftAndRequestGeneration(
            String bookId,
            CreateAudiobookDraftInput input,
            RepositoryCallback<String> callback
    );

    void createChapterDraft(
            String bookId,
            CreateChapterDraftInput input,
            RepositoryCallback<String> callback
    );

    void createChapterDraftAndRequestGeneration(
            String bookId,
            CreateChapterDraftInput input,
            RepositoryCallback<String> callback
    );

    void getChapterDraftForEdit(
            String bookId,
            String chapterId,
            RepositoryCallback<EditableChapterDraft> callback
    );

    void updateChapterDraft(
            String bookId,
            String chapterId,
            CreateChapterDraftInput input,
            RepositoryCallback<String> callback
    );

    void updateChapterDraftAndRequestGeneration(
            String bookId,
            String chapterId,
            CreateChapterDraftInput input,
            RepositoryCallback<String> callback
    );

    void requestGeneration(String bookId, RepositoryCallback<Void> callback);

    void requestChapterGeneration(String bookId, String chapterId, RepositoryCallback<Void> callback);

    void publishAudiobook(String bookId, RepositoryCallback<Void> callback);

    void setAudiobookVisibility(String bookId, boolean hiddenByCreator, RepositoryCallback<Void> callback);

    void deleteChapter(String bookId, String chapterId, RepositoryCallback<Void> callback);
}
