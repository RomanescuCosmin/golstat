package ro.golstat.stats.goals;

import org.junit.jupiter.api.Test;
import ro.golstat.stats.match.MatchGoalModel;
import ro.golstat.stats.math.Poisson;
import ro.golstat.stats.math.ScoreGrid;
import ro.golstat.stats.model.MatchSample;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HalfMarketsTest {

    private static final double EPS = 1e-9;

    private static MatchSample match(int gf, int ga, int gfHt, int gaHt) {
        return new MatchSample(LocalDate.of(2026, 1, 1), true, gf, ga, gfHt, gaHt, null);
    }

    // gazde: 2-1 (HT 1-0), 1-1 (HT 0-0) → avgFor H1=0.5 H2=1.0; avgAgainst H1=0.0 H2=1.0
    private static List<MatchSample> gazde() {
        return List.of(match(2, 1, 1, 0), match(1, 1, 0, 0));
    }

    // oaspeti: 0-0 (HT 0-0), 2-2 (HT 1-1) → avgFor H1=0.5 H2=0.5; avgAgainst H1=0.5 H2=0.5
    private static List<MatchSample> oaspeti() {
        return List.of(match(0, 0, 0, 0), match(2, 2, 1, 1));
    }

    @Test
    void blend_matchesManualComposition() {
        // λ incrucisate: gazdeH1=(0.5+0.5)/2=0.5, oaspetiH1=(0.5+0.0)/2=0.25,
        //                gazdeH2=(1.0+0.5)/2=0.75, oaspetiH2=(0.5+1.0)/2=0.75
        double modelEgal = ScoreGrid.dixonColes(0.5, 0.25, MatchGoalModel.RHO, MatchGoalModel.MAX_GOALS).draw();
        double modelGolH1 = Poisson.probabilityOver(0.75, 0.5);
        double modelGolH2 = Poisson.probabilityOver(1.5, 0.5);
        // empiric pe 4 meciuri: egal pauza 3/4 (0-0, 0-0, 1-1); gol R1 2/4; gol R2 3/4
        double w = 4.0 / 7.0;

        HalfMarkets.HalfMarketStats stats = HalfMarkets.of(gazde(), oaspeti());

        assertEquals(4, stats.sampleSize());
        assertEquals(w * 0.75 + (1 - w) * modelEgal, stats.htDrawRate(), EPS);
        assertEquals(w * 0.5 + (1 - w) * modelGolH1, stats.goalInFirstHalfRate(), EPS);
        assertEquals(w * 0.75 + (1 - w) * modelGolH2, stats.goalInSecondHalfRate(), EPS);
    }

    @Test
    void emptyBothWindows_hasNoData() {
        HalfMarkets.HalfMarketStats stats = HalfMarkets.of(List.of(), List.of());
        assertEquals(0, stats.sampleSize());
        assertFalse(stats.hasData());
    }

    @Test
    void oneEmptyWindow_usesOnlyTheOtherSide() {
        // fara fereastra gazdelor: λ gazdeH1 = apararea oaspetilor H1 = 0.5, λ oaspetiH1 = atacul lor = 0.5
        double modelEgal = ScoreGrid.dixonColes(0.5, 0.5, MatchGoalModel.RHO, MatchGoalModel.MAX_GOALS).draw();
        double w = 2.0 / 5.0;
        // empiric pe cele 2 meciuri ale oaspetilor: egal pauza 2/2
        HalfMarkets.HalfMarketStats stats = HalfMarkets.of(List.of(), oaspeti());

        assertEquals(2, stats.sampleSize());
        assertEquals(w * 1.0 + (1 - w) * modelEgal, stats.htDrawRate(), EPS);
    }

    @Test
    void ratesAreProbabilities() {
        HalfMarkets.HalfMarketStats stats = HalfMarkets.of(gazde(), oaspeti());
        for (double p : new double[]{stats.htDrawRate(), stats.goalInFirstHalfRate(), stats.goalInSecondHalfRate()}) {
            assertTrue(p >= 0.0 && p <= 1.0);
        }
    }
}
