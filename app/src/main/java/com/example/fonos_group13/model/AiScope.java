package com.example.fonos_group13.model;

import java.io.Serializable;

public final class AiScope implements Serializable {
    public static final String TYPE_BOOK = "book";
    public static final String TYPE_CHAPTER = "chapter";

    private final String type;
    private final String chapterId;

    private AiScope(String type, String chapterId) {
        this.type = type;
        this.chapterId = chapterId;
    }

    public static AiScope book() {
        return new AiScope(TYPE_BOOK, null);
    }

    public static AiScope chapter(String chapterId) {
        return new AiScope(TYPE_CHAPTER, chapterId);
    }

    public String getType() { return type; }
    public String getChapterId() { return chapterId; }
    public boolean isBook() { return TYPE_BOOK.equals(type); }
}
