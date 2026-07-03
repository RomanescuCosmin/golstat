package ro.golstat.stats.goals;

import org.junit.jupiter.api.Test;
import ro.golstat.stats.market.OverUnder;
import ro.golstat.stats.model.MatchSample;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoalLinesTest {

    private static final double EPS = 1e-9;

    private static MatchSample match(int goalsFor, int goalsAgainst) {
        return new MatchSample(LocalDate.of(2024, 1, 1), true, goalsFor, goalsAgainst, 0, 0, null);
    }

    // Fereastra folosita de mai multe teste. Totaluri: 3, 0, 2, 5, 1 (n=5).
    private static List<MatchSample> sampleWindow() {
        return List.of(match(2, 1), match(0, 0), match(1, 1), match(3, 2), match(1, 0));
    }

    @Test
    void emptyWindow_hasNoData() {
        GoalLineStats stats = GoalLines.of(List.of());

        assertEquals(0, stats.sampleSize());
        assertFalse(stats.hasData());
        assertEquals(0.0, stats.bttsRate());
        assertEquals(3, stats.lines().size()); // liniile standard sunt tot prezente, cu 0.0
        assertEquals(0.0, stats.line(2.5).overRate());
    }

    @Test
    void over1p5_countsTotalsAboveLine() {
        // Totaluri 3,0,2,5,1 → peste 1.5: 3,2,5 = 3 din 5
        assertEquals(0.6, GoalLines.of(sampleWindow()).line(1.5).overRate(), EPS);
    }

    @Test
    void over2p5_countsTotalsAboveLine() {
        // peste 2.5: 3,5 = 2 din 5
        assertEquals(0.4, GoalLines.of(sampleWindow()).line(2.5).overRate(), EPS);
    }

    @Test
    void over3p5_countsTotalsAboveLine() {
        // peste 3.5: doar 5 = 1 din 5
        assertEquals(0.2, GoalLines.of(sampleWindow()).line(3.5).overRate(), EPS);
    }

    @Test
    void overPlusUnder_equalsOne() {
        GoalLineStats stats = GoalLines.of(sampleWindow());
        for (OverUnder ou : stats.lines()) {
            assertEquals(1.0, ou.overRate() + ou.underRate(), EPS, "linia " + ou.line());
        }
    }

    @Test
    void btts_bothTeamsScored() {
        // 2-1 da, 0-0 nu, 1-1 da, 3-2 da, 1-0 nu → 3 din 5
        assertEquals(0.6, GoalLines.of(sampleWindow()).bttsRate(), EPS);
    }

    @Test
    void totalOnIntegerBoundary_classifiedCorrectly() {
        // total = 2: peste 1.5 (over) dar sub 2.5 (under)
        List<MatchSample> window = List.of(match(1, 1));
        GoalLineStats stats = GoalLines.of(window);
        assertEquals(1.0, stats.line(1.5).overRate(), EPS);
        assertEquals(0.0, stats.line(2.5).overRate(), EPS);
    }

    @Test
    void customLines_areHonored() {
        GoalLineStats stats = GoalLines.of(sampleWindow(), 0.5, 4.5);
        assertEquals(2, stats.lines().size());
        // peste 0.5 = orice meci cu cel putin un gol total: 3,2,5,1 = 4 din 5 (0-0 iese)
        assertEquals(0.8, stats.line(0.5).overRate(), EPS);
        // peste 4.5: doar 5 = 1 din 5
        assertEquals(0.2, stats.line(4.5).overRate(), EPS);
    }

    @Test
    void line_absent_returnsNull() {
        assertNull(GoalLines.of(sampleWindow()).line(9.5));
    }

    @Test
    void allScoredBoth_bttsIsOne() {
        GoalLineStats stats = GoalLines.of(List.of(match(1, 1), match(2, 3), match(1, 2)));
        assertTrue(stats.hasData());
        assertEquals(1.0, stats.bttsRate(), EPS);
    }
}
