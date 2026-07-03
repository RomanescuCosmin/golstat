package ro.golstat.stats.goals;

import org.junit.jupiter.api.Test;
import ro.golstat.stats.model.MatchSample;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoalHalvesTest {

    private static final double EPS = 1e-9;

    /** (final marcat, final primit, marcat pauza, primit pauza). Repriza 2 se deriva. */
    private static MatchSample match(int gf, int ga, int gfHt, int gaHt) {
        return new MatchSample(LocalDate.of(2024, 1, 1), true, gf, ga, gfHt, gaHt, null);
    }

    // m1: 2-1 (HT 1-0), m2: 0-0 (HT 0-0), m3: 3-2 (HT 0-1). n=3.
    private static List<MatchSample> sampleWindow() {
        return List.of(match(2, 1, 1, 0), match(0, 0, 0, 0), match(3, 2, 0, 1));
    }

    @Test
    void emptyWindow_hasNoData() {
        GoalHalfStats stats = GoalHalves.of(List.of());
        assertEquals(0, stats.sampleSize());
        assertFalse(stats.hasData());
        assertEquals(0.0, stats.scoredFirstHalfRate());
        assertEquals(0.0, stats.avgGoalsAgainstSecondHalf());
    }

    @Test
    void scoredRates_perHalf() {
        GoalHalfStats stats = GoalHalves.of(sampleWindow());
        assertTrue(stats.hasData());
        // marcat repriza 1: doar m1 → 1/3
        assertEquals(1.0 / 3, stats.scoredFirstHalfRate(), EPS);
        // marcat repriza 2: m1 (1) si m3 (3) → 2/3
        assertEquals(2.0 / 3, stats.scoredSecondHalfRate(), EPS);
    }

    @Test
    void concededRates_perHalf() {
        GoalHalfStats stats = GoalHalves.of(sampleWindow());
        // primit repriza 1: doar m3 → 1/3
        assertEquals(1.0 / 3, stats.concededFirstHalfRate(), EPS);
        // primit repriza 2: m1 (1) si m3 (1) → 2/3
        assertEquals(2.0 / 3, stats.concededSecondHalfRate(), EPS);
    }

    @Test
    void averages_perHalf() {
        GoalHalfStats stats = GoalHalves.of(sampleWindow());
        assertEquals(1.0 / 3, stats.avgGoalsForFirstHalf(), EPS);      // (1+0+0)/3
        assertEquals(4.0 / 3, stats.avgGoalsForSecondHalf(), EPS);     // (1+0+3)/3
        assertEquals(1.0 / 3, stats.avgGoalsAgainstFirstHalf(), EPS);  // (0+0+1)/3
        assertEquals(2.0 / 3, stats.avgGoalsAgainstSecondHalf(), EPS); // (1+0+1)/3
    }

    @Test
    void secondHalf_derivedFromFinalMinusHalfTime() {
        // 4-0 final, 1-0 la pauza → repriza 2 marcat = 3
        GoalHalfStats stats = GoalHalves.of(List.of(match(4, 0, 1, 0)));
        assertEquals(1.0, stats.avgGoalsForFirstHalf(), EPS);
        assertEquals(3.0, stats.avgGoalsForSecondHalf(), EPS);
    }
}
