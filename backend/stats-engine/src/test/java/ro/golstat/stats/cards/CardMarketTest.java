package ro.golstat.stats.cards;

import org.junit.jupiter.api.Test;
import ro.golstat.stats.market.EventLineStats;
import ro.golstat.stats.math.NegativeBinomial;
import ro.golstat.stats.math.Shrinkage;
import ro.golstat.stats.model.EventCountSample;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CardMarketTest {

    private static final double R = 8;

    private static EventCountSample match(int cardsFor, int cardsAgainst) {
        return new EventCountSample(LocalDate.of(2024, 1, 1), true, cardsFor, cardsAgainst, null);
    }

    // 5 meciuri, total mediu 4.4 (avgFor 2.2 + avgAgainst 2.2)
    private static List<EventCountSample> window44() {
        return List.of(match(2, 2), match(3, 2), match(2, 2), match(2, 3), match(2, 2));
    }

    @Test
    void emptyWindow_pureNegativeBinomialOnLeague() {
        EventLineStats stats = CardMarket.of(List.of(), 4.4, RefereeFactor.NEUTRAL, R, 4.5);
        assertEquals(0, stats.sampleSize());
        assertFalse(stats.hasData());
        assertEquals(NegativeBinomial.probabilityOver(4.4, R, 4.5), stats.line(4.5).overRate(), 1e-9);
    }

    @Test
    void designAnchor_refereeShiftsMean() {
        // teamMean = leagueAvg = 4.4 → shrunk 4.4; arbitru 5.5/18 vs 4.2 → factor 1.198980
        // m_meci = 4.4 · 1.198980 = 5.275510 → Over 4.5 = 0.554390
        double factor = RefereeFactor.of(5.5, 18, 4.2);
        EventLineStats stats = CardMarket.of(window44(), 4.4, factor, R, 4.5);
        assertEquals(0.554390, stats.line(4.5).overRate(), 1e-4);
    }

    @Test
    void applyShrinkageThenFactorThenNegativeBinomial() {
        // Compozitie: over == NB.probabilityOver(shrinkage(teamMean)·factor, r, line)
        double factor = 1.15;
        double teamMean = 4.4;              // window44 → avgTotal 4.4
        double league = 4.2;
        double shrunk = Shrinkage.toward(teamMean, 5, league, CardMarket.K);
        double matchMean = shrunk * factor;
        EventLineStats stats = CardMarket.of(window44(), league, factor, R, 5.5);
        assertEquals(NegativeBinomial.probabilityOver(matchMean, R, 5.5), stats.line(5.5).overRate(), 1e-12);
    }

    @Test
    void higherRefereeFactor_raisesOverProbability() {
        double calm = CardMarket.of(window44(), 4.4, 1.0, R, 4.5).line(4.5).overRate();
        double strict = CardMarket.of(window44(), 4.4, 1.25, R, 4.5).line(4.5).overRate();
        assertTrue(strict > calm, strict + " ar trebui > " + calm);
    }

    @Test
    void sampleSizePreserved() {
        assertEquals(5, CardMarket.of(window44(), 4.4, RefereeFactor.NEUTRAL, R, CardMarket.STANDARD_LINES).sampleSize());
    }
}
