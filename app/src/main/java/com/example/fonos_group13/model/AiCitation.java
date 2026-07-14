package com.example.fonos_group13.model;

import java.io.Serializable;

public final class AiCitation implements Serializable {
    private final String chapterId;
    private final String chapterTitle;
    private final String excerpt;

    public AiCitation(String chapterId, String chapterTitle, String excerpt) {
        this.chapterId = chapterId;
        this.chapterTitle = chapterTitle;
        this.excerpt = excerpt;
    }

    public String getChapterId() { return chapterId; }
    public String getChapterTitle() { return chapterTitle; }
    public String getExcerpt() { return excerpt; }
}
