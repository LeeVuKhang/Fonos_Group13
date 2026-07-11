package com.example.fonos_group13.model;

public final class SaveMutationResult {
    private final boolean saved;
    private final int saveCount;

    public SaveMutationResult(boolean saved, int saveCount) {
        this.saved = saved;
        this.saveCount = Math.max(0, saveCount);
    }

    public boolean isSaved() { return saved; }
    public int getSaveCount() { return saveCount; }
}
