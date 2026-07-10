package com.example.fonos_group13.controller.creator;

import com.example.fonos_group13.data.core.RepositoryCallback;
import com.example.fonos_group13.data.repository.CreatorCommandRepository;
import com.example.fonos_group13.model.CreateChapterDraftInput;
import com.example.fonos_group13.model.EditableChapterDraft;

public final class ManageChapterController {
    private final CreatorCommandRepository repository;

    public ManageChapterController(CreatorCommandRepository repository) {
        this.repository = repository;
    }

    public void createChapterDraft(
            String bookId,
            CreateChapterDraftInput input,
            RepositoryCallback<String> callback
    ) {
        repository.createChapterDraft(bookId, input, callback);
    }

    public void createChapterDraftAndRequestGeneration(
            String bookId,
            CreateChapterDraftInput input,
            RepositoryCallback<String> callback
    ) {
        repository.createChapterDraftAndRequestGeneration(bookId, input, callback);
    }

    public void updateChapterDraft(
            String bookId,
            String chapterId,
            CreateChapterDraftInput input,
            RepositoryCallback<String> callback
    ) {
        repository.updateChapterDraft(bookId, chapterId, input, callback);
    }

    public void updateChapterDraftAndRequestGeneration(
            String bookId,
            String chapterId,
            CreateChapterDraftInput input,
            RepositoryCallback<String> callback
    ) {
        repository.updateChapterDraftAndRequestGeneration(bookId, chapterId, input, callback);
    }

    public void getChapterDraftForEdit(
            String bookId,
            String chapterId,
            RepositoryCallback<EditableChapterDraft> callback
    ) {
        repository.getChapterDraftForEdit(bookId, chapterId, callback);
    }

    public void stop() {
        repository.cancelPendingRequests();
    }
}
