package com.example.fonos_group13.controller.profile;

import com.example.fonos_group13.controller.core.RequestGate;
import com.example.fonos_group13.data.core.RepositoryCallback;
import com.example.fonos_group13.data.repository.CatalogRepository;
import com.example.fonos_group13.data.repository.ProgressRepository;
import com.example.fonos_group13.model.CatalogSnapshot;
import com.example.fonos_group13.model.ProgressKey;
import com.example.fonos_group13.model.UserProgress;

import java.util.Collections;
import java.util.Map;

public final class ProfileController {
    public interface View {
        void renderProfileStats(ProfileStats stats, boolean partial);
    }

    private final CatalogRepository catalogRepository;
    private final ProgressRepository progressRepository;
    private final View view;
    private final RequestGate requestGate = new RequestGate();

    public ProfileController(
            CatalogRepository catalogRepository,
            ProgressRepository progressRepository,
            View view
    ) {
        this.catalogRepository = catalogRepository;
        this.progressRepository = progressRepository;
        this.view = view;
    }

    public void start() {
        long request = requestGate.open();
        view.renderProfileStats(new ProfileStats(0, 0), false);
        catalogRepository.getPublishedCatalog(new RepositoryCallback<CatalogSnapshot>() {
            @Override
            public void onSuccess(CatalogSnapshot snapshot) {
                progressRepository.getAllProgress(new RepositoryCallback<Map<ProgressKey, UserProgress>>() {
                    @Override
                    public void onSuccess(Map<ProgressKey, UserProgress> progress) {
                        if (!requestGate.isCurrent(request)) {
                            return;
                        }
                        view.renderProfileStats(ProfileStats.calculate(
                                snapshot.getBooks(),
                                snapshot.getChaptersByBookId(),
                                progress
                        ), snapshot.isPartial());
                    }

                    @Override
                    public void onError(Exception exception) {
                        if (requestGate.isCurrent(request)) {
                            view.renderProfileStats(ProfileStats.calculate(
                                    snapshot.getBooks(),
                                    snapshot.getChaptersByBookId(),
                                    Collections.emptyMap()
                            ), true);
                        }
                    }
                });
            }

            @Override
            public void onError(Exception exception) {
                if (requestGate.isCurrent(request)) {
                    view.renderProfileStats(new ProfileStats(0, 0), true);
                }
            }
        });
    }

    public void stop() {
        requestGate.invalidate();
    }
}
