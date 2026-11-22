package ro.golstat.stats.match;

import org.junit.jupiter.api.Test;
import ro.golstat.stats.goals.GoalStats;
import ro.golstat.stats.math.ScoreGrid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchGoalModelTest {

    private static final double EPS = 1e-9;

    /** GoalStats in care conteaza doar avgGoalsFor/avgGoalsAgainst/sampleSize (ratele nu intra in model). */
    private static GoalStats form(int n, double avgFor, double avgAgainst) {
        return new GoalStats(n, 0, 0, avgFor, avgAgainst);
    }

    @Test
    void lambda_doarDinCeleDouaEchipe() {
        // gazde acasa: marcheaza 2.0, primesc 0.9; oaspeti in deplasare: marcheaza 1.4, primesc 1.8
        MatchPrediction p = MatchGoalModel.predict(new MatchContext(form(7, 2.0, 0.9), form(7, 1.4, 1.8), 1.5, 1.1));

        // λGazde = 1.03 · (2.0 + 1.8) / 2 = 1.957; λOaspeti = 1.03 · (1.4 + 0.9) / 2 = 1.1845 — media ligii nu intra
        assertEquals(1.957, p.lambdaGazde(), EPS);
        assertEquals(1.1845, p.lambdaOaspeti(), EPS);
        assertTrue(p.sansaGazde() > p.sansaOaspeti(), "gazda net favorita");
        assertEquals(7, p.esantionGazde());
    }

    @Test
    void procentele_dinGrilaDixonColes() {
        MatchPrediction p = MatchGoalModel.predict(new MatchContext(form(7, 2.0, 0.9), form(7, 1.4, 1.8), 1.5, 1.1));
        ScoreGrid grid = ScoreGrid.dixonColes(1.957, 1.1845, MatchGoalModel.RHO, MatchGoalModel.MAX_GOALS);

        assertEquals(grid.probabilityOverTotal(2.5), p.linie(2.5).overRate(), EPS);
        assertEquals(grid.btts(), p.btts(), EPS);
        assertEquals(grid.draw(), p.sansaEgal(), EPS);
    }

    @Test
    void emptyWindows_lambdaFallsBackToLeagueAverage() {
        MatchPrediction p = MatchGoalModel.predict(new MatchContext(form(0, 0, 0), form(0, 0, 0), 1.5, 1.1));

        assertEquals(1.03 * 1.5, p.lambdaGazde(), EPS);
        assertEquals(1.03 * 1.1, p.lambdaOaspeti(), EPS);
        assertEquals(0, p.esantionGazde());
    }

    @Test
    void zeroGoluri_lambdaLaPodea() {
        MatchPrediction p = MatchGoalModel.predict(new MatchContext(form(7, 0, 0), form(7, 0, 0), 1.5, 1.1));
        assertEquals(MatchGoalModel.LAMBDA_MINIM, p.lambdaGazde(), EPS);
    }

    @Test
    void symmetricInputs_giveSymmetricOneXtwo() {
        MatchPrediction p = MatchGoalModel.predict(new MatchContext(form(12, 1.5, 1.2), form(12, 1.5, 1.2), 1.3, 1.3));

        assertEquals(p.lambdaGazde(), p.lambdaOaspeti(), 1e-12);
        assertEquals(p.sansaGazde(), p.sansaOaspeti(), 1e-9);
    }

    @Test
    void oneXtwo_sumsToOne() {
        MatchPrediction p = MatchGoalModel.predict(new MatchContext(form(10, 1.8, 1.0), form(10, 1.2, 1.4), 1.5, 1.1));
        assertEquals(1.0, p.sansaGazde() + p.sansaEgal() + p.sansaOaspeti(), 1e-3);
    }

    @Test
    void overLines_areMonotonicallyDecreasing() {
        MatchPrediction p = MatchGoalModel.predict(new MatchContext(form(10, 1.8, 1.0), form(10, 1.2, 1.4), 1.5, 1.1));

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
        double btts = MatchGoalModel.predict(new MatchContext(form(10, 1.8, 1.0), form(10, 1.2, 1.4), 1.5, 1.1)).btts();
        assertTrue(btts > 0.0 && btts < 1.0);
    }
}
