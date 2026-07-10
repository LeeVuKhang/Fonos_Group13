package com.example.fonos_group13.controller.catalog;

import com.example.fonos_group13.controller.core.RequestGate;
import com.example.fonos_group13.data.core.RepositoryCallback;
import com.example.fonos_group13.data.repository.CatalogRepository;
import com.example.fonos_group13.model.Book;

import java.util.List;

public final class CatalogController {
    public interface View {
        void showCatalogLoading();

        void showCatalogBooks(List<Book> books);

        void showCatalogError(Exception exception);
    }

    private final CatalogRepository repository;
    private final View view;
    private final RequestGate requestGate = new RequestGate();

    public CatalogController(CatalogRepository repository, View view) {
        this.repository = repository;
        this.view = view;
    }

    public void start() {
        long request = requestGate.open();
        view.showCatalogLoading();
        repository.getPublishedBooks(new RepositoryCallback<List<Book>>() {
            @Override
            public void onSuccess(List<Book> books) {
                if (requestGate.isCurrent(request)) {
                    view.showCatalogBooks(books);
                }
            }

            @Override
            public void onError(Exception exception) {
                if (requestGate.isCurrent(request)) {
                    view.showCatalogError(exception);
                }
            }
        });
    }

    public void stop() {
        requestGate.invalidate();
    }
}
