package com.example.fonos_group13.ui;

import static org.junit.Assert.assertEquals;

import com.example.fonos_group13.model.CreateAudiobookDraftInput;

import org.junit.Test;

public class ChapterTextCounterStateTest {
    @Test
    public void blankChapterTextShowsZeroWords() {
        ChapterTextCounterState state = ChapterTextCounterState.from("   \n\t ");

        assertEquals(0, state.getWordCount());
        assertEquals("0 / 3500 words", state.getCounterText());
        assertEquals(ChapterTextCounterState.Severity.NORMAL, state.getSeverity());
        assertEquals("", state.getMessage());
    }

    @Test
    public void countsWordsAcrossWhitespace() {
        ChapterTextCounterState state = ChapterTextCounterState.from("one\ntwo\tthree");

        assertEquals(3, state.getWordCount());
        assertEquals("3 / 3500 words", state.getCounterText());
    }

    @Test
    public void warnsWhenChapterTextApproachesWordLimit() {
        ChapterTextCounterState belowWarning =
                ChapterTextCounterState.from(repeatedWords(ChapterTextCounterState.WARNING_CHAPTER_TEXT_WORDS - 1));
        ChapterTextCounterState atWarning =
                ChapterTextCounterState.from(repeatedWords(ChapterTextCounterState.WARNING_CHAPTER_TEXT_WORDS));

        assertEquals(ChapterTextCounterState.Severity.NORMAL, belowWarning.getSeverity());
        assertEquals(ChapterTextCounterState.Severity.WARNING, atWarning.getSeverity());
        assertEquals("Approaching the 3500-word limit.", atWarning.getMessage());
    }

    @Test
    public void keepsThreeThousandFiveHundredWordsWithinContract() {
        ChapterTextCounterState state =
                ChapterTextCounterState.from(repeatedWords(CreateAudiobookDraftInput.MAX_CHAPTER_TEXT_WORDS));

        assertEquals(3500, state.getWordCount());
        assertEquals("3500 / 3500 words", state.getCounterText());
        assertEquals(ChapterTextCounterState.Severity.WARNING, state.getSeverity());
    }

    @Test
    public void marksChapterTextOverLimitAsError() {
        ChapterTextCounterState state =
                ChapterTextCounterState.from(repeatedWords(CreateAudiobookDraftInput.MAX_CHAPTER_TEXT_WORDS + 1));

        assertEquals(3501, state.getWordCount());
        assertEquals(ChapterTextCounterState.Severity.ERROR, state.getSeverity());
        assertEquals("Chapter text must be 3500 words or fewer", state.getMessage());
    }

    private static String repeatedWords(int count) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < count; index++) {
            if (index > 0) {
                builder.append(" ");
            }
            builder.append("word");
        }
        return builder.toString();
    }
}
