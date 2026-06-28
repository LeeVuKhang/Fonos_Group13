package com.example.fonos_group13.data;

import com.example.fonos_group13.model.CreateAudiobookDraftInput;
import com.example.fonos_group13.model.EditableAudiobookDraft;

interface CreatorBackendDataSource {
    void createDraft(CreateAudiobookDraftInput input, RepositoryCallback<String> callback);

    void getDraftForEdit(String bookId, RepositoryCallback<EditableAudiobookDraft> callback);

    void updateDraft(String bookId, CreateAudiobookDraftInput input, RepositoryCallback<String> callback);

    void requestGeneration(String bookId, RepositoryCallback<Void> callback);

    void publishAudiobook(String bookId, RepositoryCallback<Void> callback);
}
