package ro.golstat.stats.counts;

import org.junit.jupiter.api.Test;
import ro.golstat.stats.market.EventLineStats;
import ro.golstat.stats.market.OverUnder;
import ro.golstat.stats.math.NegativeBinomial;
import ro.golstat.stats.math.Poisson;
import ro.golstat.stats.model.EventCountSample;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class EventLineBlendTest {

    private static final double EPS = 1e-9;

    private static EventCountSample match(int countFor, int countAgainst) {
        return new EventCountSample(LocalDate.of(2024, 1, 1), true, countFor, countAgainst, null);
    }

    private static List<EventCountSample> repeat(int countFor, int countAgainst, int times) {
        return java.util.stream.IntStream.range(0, times)
                .mapToObj(i -> match(countFor, countAgainst))
                .toList();
    }

    @Test
    void fouls_emptyWindow_isPureNegativeBinomialOnLeague() {
        double leagueAvg = 47.0;
        EventLineStats stats = EventLineBlend.overDispersed(List.of(), leagueAvg, 30, 24.5);
        assertEquals(0, stats.sampleSize());
        assertFalse(stats.hasData());
        assertEquals(NegativeBinomial.probabilityOver(leagueAvg, 30, 24.5), stats.line(24.5).overRate(), EPS);
    }

    @Test
    void fouls_blendMatchesManualComposition() {
        // n=7 (w=0.7), 7 meciuri cu total 24; model NB(mean=24, r=30)
        List<EventCountSample> window = repeat(12, 12, 7);
        double w = 7.0 / 10.0;
        EventLineStats stats = EventLineBlend.overDispersed(window, 47.0, 30, 21.5, 24.5);

        // linia 21.5: toate 24 > 21.5 → empiric 1
        double exp215 = w * 1.0 + (1 - w) * NegativeBinomial.probabilityOver(24, 30, 21.5);
        assertEquals(exp215, stats.line(21.5).overRate(), EPS);
        // linia 24.5: 24 nu e > 24.5 → empiric 0
        double exp245 = w * 0.0 + (1 - w) * NegativeBinomial.probabilityOver(24, 30, 24.5);
        assertEquals(exp245, stats.line(24.5).overRate(), EPS);
    }

    @Test
    void corners_usePoissonModel() {
        List<EventCountSample> window = repeat(5, 4, 7); // total 9
        double w = 7.0 / 10.0;
        EventLineStats stats = EventLineBlend.poisson(window, 10.0, 8.5);
        // toate 9 > 8.5 → empiric 1; model Poisson(9)
        double expected = w * 1.0 + (1 - w) * Poisson.probabilityOver(9, 8.5);
        assertEquals(expected, stats.line(8.5).overRate(), EPS);
    }

    @Test
    void overPlusUnder_equalsOne() {
        EventLineStats stats = EventLineBlend.overDispersed(repeat(12, 12, 5), 47.0, 30,
                EventLineBlend.STANDARD_FOUL_LINES);
        for (OverUnder ou : stats.lines()) {
            assertEquals(1.0, ou.overRate() + ou.underRate(), EPS, "linia " + ou.line());
        }
    }

    @Test
    void sampleSizePreserved() {
        assertEquals(7, EventLineBlend.overDispersed(repeat(12, 12, 7), 47.0, 30, 24.5).sampleSize());
    }
}
