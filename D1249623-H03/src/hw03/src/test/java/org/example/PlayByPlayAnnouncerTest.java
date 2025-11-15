package org.example;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlayByPlayAnnouncerTest {

    // --- (4) extractRuns() 測試 ---
    @Test
    void testExtractRuns_2B1R() {
        assertEquals(1, PlayByPlayAnnouncer.extractRuns("2B 1R"));
    }

    @Test
    void testExtractRuns_BB3R() {
        assertEquals(3, PlayByPlayAnnouncer.extractRuns("BB 3R"));
    }

    @Test
    void testExtractRuns_K() {
        assertEquals(0, PlayByPlayAnnouncer.extractRuns("K"));
    }

    // --- (5) getInningSummary() 測試 ---

    @Test
    void testInningSummary_t01() {
        String[] t01 = {"BB","1B","K","2B 1R","F8","6-3"};

        PlayByPlayAnnouncer.InningSummary s =
                PlayByPlayAnnouncer.getInningSummary(t01);

        assertEquals(1, s.runs);
        assertEquals(2, s.hits);
        assertEquals(3, s.outs);
        assertEquals(2, s.lob);
    }

    @Test
    void testInningSummary_t02() {
        String[] t02 = {"K","K","K","1B"};

        PlayByPlayAnnouncer.InningSummary s =
                PlayByPlayAnnouncer.getInningSummary(t02);

        assertEquals(0, s.runs);
        assertEquals(0, s.hits);
        assertEquals(3, s.outs);
        assertEquals(0, s.lob);
    }

    @Test
    void testInningSummary_t03() {
        String[] t03 = {"BB","K","3B 1R","WTF","HR 3R"};

        PlayByPlayAnnouncer.InningSummary s =
                PlayByPlayAnnouncer.getInningSummary(t03);

        assertEquals(1, s.runs);
        assertEquals(1, s.hits);
        assertEquals(1, s.outs);
        assertEquals(1, s.lob);
    }

    @Test
    void testInningSummary_t04() {
        String[] t04 = {"BB","BB","BB","BB 1R","2B 2R","K","BB","K"};

        PlayByPlayAnnouncer.InningSummary s =
                PlayByPlayAnnouncer.getInningSummary(t04);

        assertEquals(4, s.runs);
        assertEquals(1, s.hits);
        assertEquals(2, s.outs);
        assertEquals(2, s.lob);
    }
}
