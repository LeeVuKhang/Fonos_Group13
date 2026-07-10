package com.example.fonos_group13.controller.catalog;

import com.example.fonos_group13.controller.core.RequestGate;
import com.example.fonos_group13.data.core.RepositoryCallback;
import com.example.fonos_group13.data.repository.CatalogRepository;
import com.example.fonos_group13.model.CatalogSnapshot;

public final class CatalogSnapshotController {
    public interface View {
        void showCatalogSnapshotLoading();

        void showCatalogSnapshot(CatalogSnapshot snapshot);

        void showCatalogSnapshotError(Exception exception);
    }

    private final CatalogRepository repository;
    private final View view;
    private final RequestGate requestGate = new RequestGate();

    public CatalogSnapshotController(CatalogRepository repository, View view) {
        this.repository = repository;
        this.view = view;
    }

    public void start() {
        long request = requestGate.open();
        view.showCatalogSnapshotLoading();
        repository.getPublishedCatalog(new RepositoryCallback<CatalogSnapshot>() {
            @Override
            public void onSuccess(CatalogSnapshot snapshot) {
                if (requestGate.isCurrent(request)) {
                    view.showCatalogSnapshot(snapshot);
                }
            }

            @Override
            public void onError(Exception exception) {
                if (requestGate.isCurrent(request)) {
                    view.showCatalogSnapshotError(exception);
                }
            }
        });
    }

    public void stop() {
        requestGate.invalidate();
    }
}
