package ro.golstat.stats.form;

import org.junit.jupiter.api.Test;
import ro.golstat.stats.model.MatchSample;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScheduleFilterTest {

    private static MatchSample vsRank(Integer opponentRank) {
        return new MatchSample(LocalDate.of(2024, 1, 1), true, 0, 0, 0, 0, opponentRank);
    }

    @Test
    void keepsOnlyBottomHalfOpponents() {
        // Liga de 20 → prag 10, pastreaza rank > 10 (locurile 11-20)
        List<MatchSample> matches = List.of(
                vsRank(5),    // fruntea → exclus
                vsRank(14),   // jos → pastrat
                vsRank(11),   // jos → pastrat
                vsRank(20),   // jos → pastrat
                vsRank(10)    // exact la mijloc (jumatatea de sus) → exclus
        );

        List<MatchSample> result = ScheduleFilter.bottomHalfOpponents(matches, 20);

        assertEquals(3, result.size());
        assertTrue(result.stream().allMatch(m -> m.opponentRank() > 10));
    }

    @Test
    void excludesUnknownRank() {
        List<MatchSample> matches = List.of(vsRank(null), vsRank(15));
        List<MatchSample> result = ScheduleFilter.bottomHalfOpponents(matches, 20);
        assertEquals(1, result.size());
        assertEquals(15, result.get(0).opponentRank());
    }

    @Test
    void noBottomHalfOpponents_returnsEmpty() {
        List<MatchSample> matches = List.of(vsRank(3), vsRank(8), vsRank(null));
        assertTrue(ScheduleFilter.bottomHalfOpponents(matches, 20).isEmpty());
    }

    @Test
    void oddLeagueSize_thresholdIsFloorHalf() {
        // Liga de 18 → prag 9, pastreaza rank > 9 (locurile 10-18)
        List<MatchSample> matches = List.of(vsRank(9), vsRank(10), vsRank(18));
        List<MatchSample> result = ScheduleFilter.bottomHalfOpponents(matches, 18);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(m -> m.opponentRank() > 9));
    }
}
