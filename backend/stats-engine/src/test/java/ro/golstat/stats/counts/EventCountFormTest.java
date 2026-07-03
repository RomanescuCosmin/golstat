package ro.golstat.stats.counts;

import org.junit.jupiter.api.Test;
import ro.golstat.stats.model.EventCountSample;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventCountFormTest {

    private static final double EPS = 1e-9;

    private static EventCountSample match(int countFor, int countAgainst) {
        return new EventCountSample(LocalDate.of(2024, 1, 1), true, countFor, countAgainst, null);
    }

    @Test
    void emptyWindow_hasNoData() {
        EventCountStats stats = EventCountForm.of(List.of());
        assertEquals(0, stats.sampleSize());
        assertFalse(stats.hasData());
        assertEquals(0.0, stats.avgCountTotal());
    }

    @Test
    void averages_knownNumbers() {
        // (12,10),(8,14),(10,12) → avgFor 10, avgAgainst 12, total 22
        EventCountStats stats = EventCountForm.of(List.of(match(12, 10), match(8, 14), match(10, 12)));
        assertTrue(stats.hasData());
        assertEquals(3, stats.sampleSize());
        assertEquals(10.0, stats.avgCountFor(), EPS);
        assertEquals(12.0, stats.avgCountAgainst(), EPS);
        assertEquals(22.0, stats.avgCountTotal(), EPS);
    }
}
