package com.example.fonos_group13.data.core;

/** A framework-neutral handle for cancellable one-shot work. */
public interface RequestHandle {
    RequestHandle NONE = new RequestHandle() {
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
