package com.example.fonos_group13.data.firestore;

import com.example.fonos_group13.data.core.Subscription;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.concurrent.atomic.AtomicBoolean;

public final class FirestoreSubscription implements Subscription {
    private final ListenerRegistration registration;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    public FirestoreSubscription(ListenerRegistration registration) {
        this.registration = registration;
    }

    @Override
    public void cancel() {
        if (cancelled.compareAndSet(false, true) && registration != null) {
            registration.remove();
        }
    }

    @Override
    public boolean isCancelled() {
        return cancelled.get();
    }
}
