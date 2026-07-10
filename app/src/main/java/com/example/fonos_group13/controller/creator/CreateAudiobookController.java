package com.example.fonos_group13.controller.creator;

import com.example.fonos_group13.data.core.RepositoryCallback;
import com.example.fonos_group13.data.repository.CreatorCommandRepository;
import com.example.fonos_group13.model.CreateAudiobookDraftInput;
import com.example.fonos_group13.model.EditableAudiobookDraft;

public final class CreateAudiobookController {
    private final CreatorCommandRepository repository;

    public CreateAudiobookController(CreatorCommandRepository repository) {
        this.repository = repository;
    }

    public void createDraft(CreateAudiobookDraftInput input, RepositoryCallback<String> callback) {
        repository.createDraft(input, callback);
    }

    public void createDraftAndRequestGeneration(
            CreateAudiobookDraftInput input,
            RepositoryCallback<String> callback
    ) {
        repository.createDraftAndRequestGeneration(input, callback);
    }

    public void updateDraft(
            String bookId,
            CreateAudiobookDraftInput input,
            RepositoryCallback<String> callback
    ) {
        repository.updateDraft(bookId, input, callback);
    }

    public void updateDraftAndRequestGeneration(
            String bookId,
            CreateAudiobookDraftInput input,
            RepositoryCallback<String> callback
    ) {
        repository.updateDraftAndRequestGeneration(bookId, input, callback);
    }

    public void getDraftForEdit(
            String bookId,
            RepositoryCallback<EditableAudiobookDraft> callback
    ) {
        repository.getDraftForEdit(bookId, callback);
    }

    public void stop() {
        repository.cancelPendingRequests();
    }
}
