package com.example.fonos_group13.controller.core;

/** Rejects callbacks from superseded or stopped controller requests. */
public final class RequestGate {
    private long generation;

    public long open() {
        generation++;
        return generation;
    }

    public void invalidate() {
        generation++;
    }

    public boolean isCurrent(long requestGeneration) {
        return requestGeneration == generation;
    }
}
