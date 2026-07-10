package com.example.fonos_group13.data.core;

/** A framework-neutral handle for a live data subscription. */
public interface Subscription {
    Subscription NONE = new Subscription() {
        @Override
        public void cancel() {
        }

        @Override
        public boolean isCancelled() {
            return true;
        }
    };

    void cancel();

    default boolean isCancelled() {
        return false;
    }
}
