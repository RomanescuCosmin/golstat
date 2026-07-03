package ro.golstat.stats.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NegativeBinomialTest {

    private static final double EPS = 1e-6;

    // Cartonase: m = 4.4, r = 8 (design tema 2, ex.1)
    @Test
    void pmf_knownValues_cards() {
        assertEquals(0.030015558, NegativeBinomial.pmf(4.4, 8, 0), EPS);
        assertEquals(0.085205454, NegativeBinomial.pmf(4.4, 8, 1), EPS);
        assertEquals(0.136053870, NegativeBinomial.pmf(4.4, 8, 2), EPS);
    }

    @Test
    void probabilityOver_cards() {
        assertEquals(0.587801188, NegativeBinomial.probabilityOver(4.4, 8, 3.5), EPS);
        assertEquals(0.430770577, NegativeBinomial.probabilityOver(4.4, 8, 4.5), EPS);
        assertEquals(0.194227900, NegativeBinomial.probabilityOver(4.4, 8, 6.5), EPS);
    }

    @Test
    void probabilityOver_cardsWithRefereeShiftedMean() {
        // Design tema 2, ex.3: m_meci = 5.275510 (dupa factor arbitru) → Over 4.5 = 0.554390
        assertEquals(0.554390, NegativeBinomial.probabilityOver(5.275510, 8, 4.5), 1e-5);
    }

    // Faulturi: m = 23.5, r = 30 (design tema 2, ex.4)
    @Test
    void probabilityOver_fouls() {
        assertEquals(0.598186, NegativeBinomial.probabilityOver(23.5, 30, 21.5), 1e-5);
        assertEquals(0.413328, NegativeBinomial.probabilityOver(23.5, 30, 24.5), 1e-5);
        assertEquals(0.254699, NegativeBinomial.probabilityOver(23.5, 30, 27.5), 1e-5);
    }

    @Test
    void overDispersion_heavierTailsThanPoisson() {
        // La aceeasi medie, NB muta masa in cozi: sub Poisson la centru, peste la coada inalta
        double nbHigh = NegativeBinomial.probabilityOver(4.4, 8, 6.5);
        double poissonHigh = Poisson.probabilityOver(4.4, 6.5);
        assertTrue(nbHigh > poissonHigh, "NB ar trebui sa aiba coada mai grea: " + nbHigh + " vs " + poissonHigh);
    }

    @Test
    void cdf_convergesToOne() {
        assertEquals(1.0, NegativeBinomial.cdf(4.4, 8, 200), EPS);
    }

    @Test
    void atLeast_isComplementOfCdf() {
        assertEquals(1.0 - NegativeBinomial.pmf(4.4, 8, 0), NegativeBinomial.atLeast(4.4, 8, 1), EPS);
        assertEquals(1.0, NegativeBinomial.atLeast(4.4, 8, 0), EPS);
    }

    @Test
    void allPmfInUnitInterval() {
        for (int k = 0; k <= 30; k++) {
            double v = NegativeBinomial.pmf(23.5, 30, k);
            assertTrue(v >= 0.0 && v <= 1.0, "pmf out of range at k=" + k + ": " + v);
        }
    }

    @Test
    void nonPositiveDispersion_throws() {
        assertThrows(IllegalArgumentException.class, () -> NegativeBinomial.pmf(4.4, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> NegativeBinomial.pmf(4.4, -1, 1));
    }

    @Test
    void negativeMeanOrK_throws() {
        assertThrows(IllegalArgumentException.class, () -> NegativeBinomial.pmf(-1, 8, 1));
        assertThrows(IllegalArgumentException.class, () -> NegativeBinomial.pmf(4.4, 8, -1));
    }
}
