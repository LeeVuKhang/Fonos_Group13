package com.example.fonos_group13.controller.creator;

import com.example.fonos_group13.data.core.RepositoryCallback;
import com.example.fonos_group13.data.core.Subscription;
import com.example.fonos_group13.data.repository.CreatorUploadsRepository;
import com.example.fonos_group13.model.UserGeneratedAudiobook;
import com.example.fonos_group13.model.UserGeneratedChapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MyUploadsController {
    public interface View {
        void showUploadsLoading();

        void showUploads(
                List<UserGeneratedAudiobook> uploads,
                Map<String, List<UserGeneratedChapter>> chaptersByBookId
        );

        void showUploadsError(Exception exception);
    }

    private final CreatorUploadsRepository repository;
    private final View view;
    private final Map<String, Subscription> chapterSubscriptions = new HashMap<>();
    private final Map<String, List<UserGeneratedChapter>> chaptersByBookId = new HashMap<>();
    private Subscription uploadsSubscription = Subscription.NONE;
    private List<UserGeneratedAudiobook> uploads = Collections.emptyList();
    private boolean active;

    public MyUploadsController(CreatorUploadsRepository repository, View view) {
        this.repository = repository;
        this.view = view;
    }

    public void start() {
        stop();
        active = true;
        view.showUploadsLoading();
        uploadsSubscription = repository.observeMyUploads(
                new RepositoryCallback<List<UserGeneratedAudiobook>>() {
                    @Override
                    public void onSuccess(List<UserGeneratedAudiobook> data) {
                        if (!active) {
                            return;
                        }
                        uploads = data == null ? Collections.emptyList() : data;
                        syncChapterSubscriptions();
                        emit();
                    }

                    @Override
                    public void onError(Exception exception) {
                        if (!active) {
                            return;
                        }
                        cancelChapterSubscriptions();
                        uploads = Collections.emptyList();
                        view.showUploadsError(exception);
                    }
                }
        );
    }

    public void stop() {
        active = false;
        uploadsSubscription.cancel();
        uploadsSubscription = Subscription.NONE;
        cancelChapterSubscriptions();
    }

    private void syncChapterSubscriptions() {
        Set<String> activeBookIds = new HashSet<>();
        for (UserGeneratedAudiobook upload : uploads) {
            String bookId = upload == null ? null : trimToNull(upload.getId());
            if (bookId == null) {
                continue;
            }
            activeBookIds.add(bookId);
            if (!chapterSubscriptions.containsKey(bookId)) {
                Subscription subscription = repository.observeUploadChapters(
                        bookId,
                        new RepositoryCallback<List<UserGeneratedChapter>>() {
                            @Override
                            public void onSuccess(List<UserGeneratedChapter> chapters) {
                                if (!active) {
                                    return;
                                }
                                chaptersByBookId.put(
                                        bookId,
                                        chapters == null ? Collections.emptyList() : chapters
                                );
                                emit();
                            }

                            @Override
                            public void onError(Exception exception) {
                                if (active) {
                                    chaptersByBookId.put(bookId, Collections.emptyList());
                                    emit();
                                }
                            }
                        }
                );
                chapterSubscriptions.put(bookId, subscription);
            }
        }

        List<String> staleIds = new ArrayList<>();
        for (String bookId : chapterSubscriptions.keySet()) {
            if (!activeBookIds.contains(bookId)) {
                staleIds.add(bookId);
            }
        }
        for (String bookId : staleIds) {
            chapterSubscriptions.remove(bookId).cancel();
            chaptersByBookId.remove(bookId);
        }
    }

    private void emit() {
        view.showUploads(
                Collections.unmodifiableList(new ArrayList<>(uploads)),
                Collections.unmodifiableMap(new HashMap<>(chaptersByBookId))
        );
    }

    private void cancelChapterSubscriptions() {
        for (Subscription subscription : chapterSubscriptions.values()) {
            subscription.cancel();
        }
        chapterSubscriptions.clear();
        chaptersByBookId.clear();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
