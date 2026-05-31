package com.example.fonos_group13.model;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class Book {
    private final String id;
    private final String title;
    private final String author;
    private final String chapterTitle;
    private final String contentSample;
    private final String audioLocalResName;
    private final String audioUrl;
    private final String audioStoragePath;
    private final long durationSec;
    private final String languageCode;
    private final String voiceGender;
    private final boolean featured;
    private final boolean published;
    private final int order;

    public Book(
            String id,
            String title,
            String author,
            String chapterTitle,
            String contentSample,
            String audioLocalResName,
            String audioUrl,
            String audioStoragePath,
            long durationSec,
            String languageCode,
            String voiceGender,
            boolean featured,
            boolean published,
            int order
    ) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.chapterTitle = chapterTitle;
        this.contentSample = contentSample;
        this.audioLocalResName = audioLocalResName;
        this.audioUrl = audioUrl;
        this.audioStoragePath = audioStoragePath;
        this.durationSec = durationSec;
        this.languageCode = languageCode;
        this.voiceGender = voiceGender;
        this.featured = featured;
        this.published = published;
        this.order = order;
    }

    public static Book fromDocument(DocumentSnapshot document) {
        return new Book(
                document.getId(),
                valueOrDefault(document.getString("title"), "Untitled"),
                valueOrDefault(document.getString("author"), "Unknown author"),
                valueOrDefault(document.getString("chapterTitle"), "Chapter 1"),
                valueOrDefault(document.getString("contentSample"), ""),
                document.getString("audioLocalResName"),
                document.getString("audioUrl"),
                document.getString("audioStoragePath"),
                longValue(document.getLong("durationSec")),
                valueOrDefault(document.getString("languageCode"), "en-US"),
                valueOrDefault(document.getString("voiceGender"), "female"),
                booleanValue(document.getBoolean("featured")),
                booleanValue(document.getBoolean("published")),
                (int) longValue(document.getLong("order"))
        );
    }

    public static List<Book> fallbackBooks() {
        List<Book> books = new ArrayList<>();
        books.add(new Book(
                "pride_prejudice",
                "Pride and Prejudice",
                "Jane Austen",
                "Chapter 1",
                "It is a truth universally acknowledged, that a single man in possession of a good fortune, must be in want of a wife.",
                "pride_prejudice_ch1",
                null,
                null,
                1125,
                "en-US",
                "female",
                false,
                true,
                1
        ));
        books.add(new Book(
                "silent_echo",
                "The Silent Echo",
                "Elena Rostova",
                "Chapter 1",
                "The valley held its breath as the first bell rang through the morning fog.",
                "silent_echo_ch1",
                null,
                null,
                2410,
                "en-US",
                "female",
                true,
                true,
                2
        ));
        books.add(new Book(
                "midnight_garden",
                "Midnight in the Garden",
                "Thomas Black",
                "Chapter 1",
                "At midnight, the garden gate opened for the first time in twenty years.",
                "midnight_garden_ch1",
                null,
                null,
                2970,
                "en-US",
                "female",
                true,
                true,
                3
        ));
        books.add(new Book(
                "dragon_keep",
                "The Dragon's Keep",
                "R.R. Martin",
                "Chapter 1",
                "Smoke curled above the northern tower long before anyone saw the wings.",
                "dragon_keep_ch1",
                null,
                null,
                1845,
                "en-US",
                "female",
                false,
                true,
                4
        ));
        books.add(new Book(
                "design_system",
                "Design System",
                "Alla Kholmatova",
                "Chapter 1",
                "A design system starts as a shared language before it becomes a library of components.",
                "design_system_ch1",
                null,
                null,
                1620,
                "en-US",
                "female",
                false,
                true,
                5
        ));
        books.add(new Book(
                "scandal_bohemia",
                "A Scandal in Bohemia",
                "Arthur Conan Doyle",
                "Chapter 1",
                "Sherlock Holmes remembers one woman above all others, and her wit turns the case in a new direction.",
                "scandal_bohemia_ch1",
                null,
                null,
                2100,
                "en-US",
                "female",
                false,
                true,
                6
        ));
        books.add(new Book(
                "time_machine",
                "The Time Machine",
                "H.G. Wells",
                "Chapter 1",
                "The inventor gathers his friends and asks them to imagine time as another direction of travel.",
                "time_machine_ch1",
                null,
                null,
                2520,
                "en-US",
                "female",
                false,
                true,
                7
        ));
        return books;
    }

    public static Book fallbackById(String bookId) {
        for (Book book : fallbackBooks()) {
            if (book.getId().equals(bookId)) {
                return book;
            }
        }
        return fallbackBooks().get(0);
    }

    private static String valueOrDefault(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private static long longValue(Long value) {
        return value == null ? 0 : value;
    }

    private static boolean booleanValue(Boolean value) {
        return value != null && value;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getChapterTitle() {
        return chapterTitle;
    }

    public String getContentSample() {
        return contentSample;
    }

    public String getAudioLocalResName() {
        return audioLocalResName;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public String getAudioStoragePath() {
        return audioStoragePath;
    }

    public long getDurationSec() {
        return durationSec;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public String getVoiceGender() {
        return voiceGender;
    }

    public boolean isFeatured() {
        return featured;
    }

    public boolean isPublished() {
        return published;
    }

    public int getOrder() {
        return order;
    }
}
