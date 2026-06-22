package com.example.fonos_group13.data;

import com.example.fonos_group13.model.CreateAudiobookDraftInput;

interface CreatorBackendDataSource {
    void createDraft(CreateAudiobookDraftInput input, RepositoryCallback<String> callback);

    void requestGeneration(String bookId, RepositoryCallback<Void> callback);
}
