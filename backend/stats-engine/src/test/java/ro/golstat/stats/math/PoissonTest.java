package ro.golstat.stats.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PoissonTest {

    private static final double EPS = 1e-6;

    // Valori de referinta pentru lambda = 1.5 (calculate manual):
    // pmf(0)=e^-1.5=0.223130, pmf(1)=0.334695, pmf(2)=0.251021, cdf(2)=0.808847
    @Test
    void pmf_knownValues_lambda1p5() {
        assertEquals(0.223130, Poisson.pmf(1.5, 0), EPS);
        assertEquals(0.334695, Poisson.pmf(1.5, 1), EPS);
        assertEquals(0.251021, Poisson.pmf(1.5, 2), EPS);
    }

    @Test
    void cdf_isCumulativeSumOfPmf() {
        double expected = Poisson.pmf(1.5, 0) + Poisson.pmf(1.5, 1) + Poisson.pmf(1.5, 2);
        assertEquals(expected, Poisson.cdf(1.5, 2), EPS);
        assertEquals(0.808847, Poisson.cdf(1.5, 2), EPS);
    }

    @Test
    void cdf_convergesToOneForLargeK() {
        assertEquals(1.0, Poisson.cdf(2.0, 50), EPS);
    }

    @Test
    void atLeast_isComplementOfCdf() {
        // P(X >= 1) = 1 - P(X = 0)
        assertEquals(1.0 - Poisson.pmf(1.5, 0), Poisson.atLeast(1.5, 1), EPS);
        // P(X >= 0) = 1 mereu
        assertEquals(1.0, Poisson.atLeast(1.5, 0), EPS);
    }

    @Test
    void probabilityOver_2p5_lambda1p5() {
        // Over 2.5 = P(X >= 3) = 1 - cdf(2) = 0.191153
        assertEquals(0.191153, Poisson.probabilityOver(1.5, 2.5), EPS);
    }

    @Test
    void probabilityOver_2p5_lambda2p0() {
        // lambda=2.0: cdf(2)=0.676676 → Over 2.5 = 0.323324
        assertEquals(0.323324, Poisson.probabilityOver(2.0, 2.5), EPS);
    }

    @Test
    void probabilityOver_1p5_equalsAtLeastTwo() {
        assertEquals(Poisson.atLeast(1.7, 2), Poisson.probabilityOver(1.7, 1.5), EPS);
    }

    @Test
    void lambdaZero_massAtZero() {
        assertEquals(1.0, Poisson.pmf(0.0, 0), EPS);
        assertEquals(0.0, Poisson.pmf(0.0, 1), EPS);
        assertEquals(1.0, Poisson.cdf(0.0, 0), EPS);
        assertEquals(0.0, Poisson.atLeast(0.0, 1), EPS);
    }

    @Test
    void allProbabilitiesInUnitInterval() {
        for (int k = 0; k <= 10; k++) {
            double p = Poisson.pmf(2.3, k);
            assertTrue(p >= 0.0 && p <= 1.0, "pmf out of range at k=" + k + ": " + p);
        }
    }

    @Test
    void negativeLambda_throws() {
        assertThrows(IllegalArgumentException.class, () -> Poisson.pmf(-0.1, 0));
    }

    @Test
    void negativeK_throws() {
        assertThrows(IllegalArgumentException.class, () -> Poisson.pmf(1.5, -1));
    }
}
