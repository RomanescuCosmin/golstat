package ro.golstat.stats.form;

import org.junit.jupiter.api.Test;
import ro.golstat.stats.model.EventCountSample;
import ro.golstat.stats.model.MatchSample;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WindowCountsTest {

    private static MatchSample match(int gf, int ga, int gfHt, int gaHt) {
        return new MatchSample(LocalDate.of(2026, 1, 1), true, gf, ga, gfHt, gaHt, null);
    }

    // 2-1 (HT 1-1), 0-0 (HT 0-0), 3-0 (HT 0-0), 1-2 (HT 1-0), 2-2 (HT 0-1)
    private static List<MatchSample> window() {
        return List.of(
                match(2, 1, 1, 1),
                match(0, 0, 0, 0),
                match(3, 0, 0, 0),
                match(1, 2, 1, 0),
                match(2, 2, 0, 1)
        );
    }

    @Test
    void overTotalGoals_countsTotalsAboveLine() {
        // totaluri: 3, 0, 3, 3, 4
        assertEquals(4, WindowCounts.overTotalGoals(window(), 1.5));
        assertEquals(4, WindowCounts.overTotalGoals(window(), 2.5));
        assertEquals(1, WindowCounts.overTotalGoals(window(), 3.5));
    }

    @Test
    void scoredAndConceded() {
        assertEquals(4, WindowCounts.scored(window()));
        assertEquals(3, WindowCounts.conceded(window()));
    }

    @Test
    void btts_bothTeamsScored() {
        // 2-1, 1-2, 2-2
        assertEquals(3, WindowCounts.btts(window()));
    }

    @Test
    void draws_fullTimeAndHalfTime() {
        // FT egal: 0-0, 2-2; HT egal: 1-1, 0-0, 0-0
        assertEquals(2, WindowCounts.drawsFullTime(window()));
        assertEquals(3, WindowCounts.drawsHalfTime(window()));
    }

    @Test
    void goalPerHalf() {
        // gol in R1: 1-1, 1-0, 0-1 → 3; gol in R2: 2-1→(1,0), 3-0→(3,0), 1-2→(0,2), 2-2→(2,1) → 4
        assertEquals(3, WindowCounts.goalInFirstHalf(window()));
        assertEquals(4, WindowCounts.goalInSecondHalf(window()));
    }

    @Test
    void overTotalEvents_countsCombinedEvents() {
        List<EventCountSample> cornere = List.of(
                new EventCountSample(LocalDate.of(2026, 1, 1), true, 6, 4, null),   // 10
                new EventCountSample(LocalDate.of(2026, 1, 2), false, 3, 4, null),  // 7
                new EventCountSample(LocalDate.of(2026, 1, 3), true, 5, 5, null)    // 10
        );
        assertEquals(3, WindowCounts.overTotalEvents(cornere, 6.5));
        assertEquals(2, WindowCounts.overTotalEvents(cornere, 9.5));
        assertEquals(0, WindowCounts.overTotalEvents(cornere, 10.5));
    }

    @Test
    void emptyWindow_isZeroEverywhere() {
        assertEquals(0, WindowCounts.overTotalGoals(List.of(), 2.5));
        assertEquals(0, WindowCounts.btts(List.of()));
        assertEquals(0, WindowCounts.drawsHalfTime(List.of()));
        assertEquals(0, WindowCounts.overTotalEvents(List.of(), 9.5));
    }
}
