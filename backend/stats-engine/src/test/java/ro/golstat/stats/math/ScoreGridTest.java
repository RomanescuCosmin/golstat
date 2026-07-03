package ro.golstat.stats.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScoreGridTest {

    private static final double EPS = 1e-6;
    private static final int M = 15;

    // Design tema 1, ex.1: lambdaFor = 1.2, lambdaAgainst = 1.0, rho = -0.1
    @Test
    void dixonColes_correctedSmallScores() {
        ScoreGrid g = ScoreGrid.dixonColes(1.2, 1.0, -0.1, M);
        assertEquals(0.124099537, g.exact(0, 0), EPS);
        assertEquals(0.119667411, g.exact(1, 0), EPS);
        assertEquals(0.097506779, g.exact(0, 1), EPS);
        assertEquals(0.146260169, g.exact(1, 1), EPS);
    }

    @Test
    void dixonColes_markets() {
        ScoreGrid g = ScoreGrid.dixonColes(1.2, 1.0, -0.1, M);
        assertEquals(0.341273728, 1.0 - g.probabilityOverTotal(1.5), EPS); // Under 1.5
        assertEquals(0.377286250, g.probabilityOverTotal(2.5), EPS);       // Over 2.5
        assertEquals(0.455025884, g.btts(), EPS);
    }

    @Test
    void matrixSumsToOne() {
        assertEquals(1.0, ScoreGrid.dixonColes(1.2, 1.0, -0.1, M).total(), 1e-9);
    }

    @Test
    void overLinesUnchangedByCorrection() {
        // Invariant Dixon-Coles: liniile 2.5/3.5 sunt identice cu Poisson-ul independent
        ScoreGrid dc = ScoreGrid.dixonColes(1.2, 1.0, -0.1, M);
        ScoreGrid indep = ScoreGrid.independent(1.2, 1.0, M);
        assertEquals(indep.probabilityOverTotal(2.5), dc.probabilityOverTotal(2.5), EPS);
        assertEquals(indep.probabilityOverTotal(3.5), dc.probabilityOverTotal(3.5), EPS);
    }

    @Test
    void independentGridEqualsPoissonConvolution() {
        // Poisson dublu independent → total ~ Poisson(lambdaFor + lambdaAgainst)
        ScoreGrid indep = ScoreGrid.independent(1.2, 1.0, M);
        assertEquals(Poisson.probabilityOver(2.2, 2.5), indep.probabilityOverTotal(2.5), EPS);
        assertEquals(Poisson.probabilityOver(2.2, 1.5), indep.probabilityOverTotal(1.5), EPS);
    }

    @Test
    void rhoZero_reducesToIndependent() {
        ScoreGrid dc0 = ScoreGrid.dixonColes(1.2, 1.0, 0.0, M);
        ScoreGrid indep = ScoreGrid.independent(1.2, 1.0, M);
        assertEquals(indep.exact(0, 0), dc0.exact(0, 0), EPS);
        assertEquals(indep.btts(), dc0.btts(), EPS);
    }

    // Design tema 1, ex.2: fereastra goala, media ligii 2.6 → lambdaFor = lambdaAgainst = 1.3
    @Test
    void dixonColes_leagueFallback() {
        ScoreGrid g = ScoreGrid.dixonColes(1.3, 1.3, -0.1, M);
        assertEquals(0.086825813, g.exact(0, 0), EPS);
        assertEquals(0.254832647, 1.0 - g.probabilityOverTotal(1.5), EPS); // Under 1.5
        assertEquals(0.481570424, g.probabilityOverTotal(2.5), EPS);       // Over 2.5
        assertEquals(0.541762227, g.btts(), EPS);
    }

    @Test
    void invalidArguments_throw() {
        assertThrows(IllegalArgumentException.class, () -> ScoreGrid.dixonColes(-1, 1, 0, M));
        assertThrows(IllegalArgumentException.class, () -> ScoreGrid.dixonColes(1, 1, 0, 0));
    }
}
