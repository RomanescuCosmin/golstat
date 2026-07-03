package ro.golstat.stats.match;

import org.junit.jupiter.api.Test;
import ro.golstat.stats.goals.GoalStats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchGoalModelTest {

    /** GoalStats in care conteaza doar avgGoalsFor/avgGoalsAgainst/sampleSize (ratele nu intra in model). */
    private static GoalStats form(int n, double avgFor, double avgAgainst) {
        return new GoalStats(n, 0, 0, avgFor, avgAgainst);
    }

    @Test
    void lambda_attackTimesDefenceOverLeagueAverage_withShrinkage() {
        // gazde: atac 2.0, aparare 0.9 (n=10); oaspeti: atac 1.4, aparare 1.8 (n=10)
        // medii liga: gazde 1.5, oaspeti 1.1; K=3
        MatchContext ctx = new MatchContext(
                form(10, 2.0, 0.9), form(10, 1.4, 1.8), 1.5, 1.1);
        MatchPrediction p = MatchGoalModel.predict(ctx);

        // λGazde = 1.5 × (10·(2.0/1.5)+3)/13 × (10·(1.8/1.5)+3)/13 = 1.5 × 1.256410 × 1.153846
        assertEquals(2.174556, p.lambdaGazde(), 1e-4);
        // λOaspeti = 1.1 × (10·(1.4/1.1)+3)/13 × (10·(0.9/1.1)+3)/13
        assertEquals(1.144648, p.lambdaOaspeti(), 1e-4);
        assertTrue(p.sansaGazde() > p.sansaOaspeti(), "gazda net favorita");
        assertEquals(10, p.esantionGazde());
        assertEquals(10, p.esantionOaspeti());
    }

    @Test
    void emptyWindows_lambdaFallsBackToLeagueAverage() {
        MatchContext ctx = new MatchContext(form(0, 0, 0), form(0, 0, 0), 1.5, 1.1);
        MatchPrediction p = MatchGoalModel.predict(ctx);

        assertEquals(1.5, p.lambdaGazde(), 1e-9);   // factori trasi complet spre 1.0 (n=0)
        assertEquals(1.1, p.lambdaOaspeti(), 1e-9);
        assertEquals(0, p.esantionGazde());
    }

    @Test
    void symmetricInputs_giveSymmetricOneXtwo() {
        // forme identice + medii de liga egale → λGazde == λOaspeti → 1X2 simetric
        MatchContext ctx = new MatchContext(form(12, 1.5, 1.2), form(12, 1.5, 1.2), 1.3, 1.3);
        MatchPrediction p = MatchGoalModel.predict(ctx);

        assertEquals(p.lambdaGazde(), p.lambdaOaspeti(), 1e-12);
        assertEquals(p.sansaGazde(), p.sansaOaspeti(), 1e-9);
    }

    @Test
    void oneXtwo_sumsToOne() {
        MatchContext ctx = new MatchContext(form(10, 1.8, 1.0), form(10, 1.2, 1.4), 1.5, 1.1);
        MatchPrediction p = MatchGoalModel.predict(ctx);
        assertEquals(1.0, p.sansaGazde() + p.sansaEgal() + p.sansaOaspeti(), 1e-3);
    }

    @Test
    void overLines_areMonotonicallyDecreasing() {
        MatchContext ctx = new MatchContext(form(10, 1.8, 1.0), form(10, 1.2, 1.4), 1.5, 1.1);
        MatchPrediction p = MatchGoalModel.predict(ctx);

        double prev = 1.0;
        for (double line : new double[]{0.5, 1.5, 2.5, 3.5, 4.5}) {
            double over = p.linie(line).overRate();
            assertTrue(over < prev, "over(" + line + ") trebuie sub over-ul liniei precedente");
            assertTrue(over >= 0.0 && over <= 1.0);
            prev = over;
        }
    }

    @Test
    void btts_isAProbability() {
        MatchContext ctx = new MatchContext(form(10, 1.8, 1.0), form(10, 1.2, 1.4), 1.5, 1.1);
        double btts = MatchGoalModel.predict(ctx).btts();
        assertTrue(btts > 0.0 && btts < 1.0);
    }
}
