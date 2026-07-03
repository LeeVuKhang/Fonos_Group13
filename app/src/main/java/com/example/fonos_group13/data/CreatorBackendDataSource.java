package com.example.fonos_group13.data;

import com.example.fonos_group13.model.CreateAudiobookDraftInput;
import com.example.fonos_group13.model.CreateChapterDraftInput;
import com.example.fonos_group13.model.EditableAudiobookDraft;
import com.example.fonos_group13.model.EditableChapterDraft;

interface CreatorBackendDataSource {
    void createDraft(CreateAudiobookDraftInput input, RepositoryCallback<String> callback);

    void getDraftForEdit(String bookId, RepositoryCallback<EditableAudiobookDraft> callback);

    void updateDraft(String bookId, CreateAudiobookDraftInput input, RepositoryCallback<String> callback);

    void createChapterDraft(String bookId, CreateChapterDraftInput input, RepositoryCallback<String> callback);

    void getChapterDraftForEdit(String bookId, String chapterId, RepositoryCallback<EditableChapterDraft> callback);

    void updateChapterDraft(String bookId, String chapterId, CreateChapterDraftInput input, RepositoryCallback<String> callback);

    void requestGeneration(String bookId, RepositoryCallback<Void> callback);

    void requestChapterGeneration(String bookId, String chapterId, RepositoryCallback<Void> callback);

    void publishAudiobook(String bookId, RepositoryCallback<Void> callback);
}
