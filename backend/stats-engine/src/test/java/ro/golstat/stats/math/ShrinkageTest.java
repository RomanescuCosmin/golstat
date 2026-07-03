package ro.golstat.stats.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShrinkageTest {

    private static final double EPS = 1e-9;

    @Test
    void blendsValueAndTargetByWeight() {
        // Design tema 3, ex.2: atacant tanar 0.60/90, baseline 0.35, n=5, K=5 → 0.475
        assertEquals(0.475, Shrinkage.toward(0.60, 5, 0.35, 5), EPS);
    }

    @Test
    void zeroSample_fallsToTarget() {
        assertEquals(0.35, Shrinkage.toward(0.99, 0, 0.35, 5), EPS);
    }

    @Test
    void largeSample_approachesValue() {
        // toward(0.60, 1000, 0.35, 5) = 601.75/1005 = 0.598756...
        assertEquals(0.598756, Shrinkage.toward(0.60, 1000, 0.35, 5), 1e-6);
    }

    @Test
    void refereeFactor_shrunkMean() {
        // Design tema 2, ex.3: arbitru 5.5 pe 18 meciuri, liga 4.2, K=10 → media shrunk 5.035714
        double shrunk = Shrinkage.toward(5.5, 18, 4.2, 10);
        assertEquals(141.0 / 28.0, shrunk, EPS);          // (18·5.5 + 10·4.2)/28
        assertEquals(1.198980, shrunk / 4.2, 1e-6);       // factorul (sub clamp-ul 1.3)
    }

    @Test
    void bothZero_fallsToTarget() {
        assertEquals(0.42, Shrinkage.toward(0.9, 0, 0.42, 0), EPS);
    }

    @Test
    void negativeSample_throws() {
        assertThrows(IllegalArgumentException.class, () -> Shrinkage.toward(1, -1, 1, 3));
    }

    @Test
    void negativeK_throws() {
        assertThrows(IllegalArgumentException.class, () -> Shrinkage.toward(1, 1, 1, -3));
    }

    @Test
    void weightMonotonicWithSampleSize() {
        double small = Shrinkage.toward(0.6, 2, 0.35, 5);
        double large = Shrinkage.toward(0.6, 20, 0.35, 5);
        assertTrue(Math.abs(large - 0.6) < Math.abs(small - 0.6));
    }
}
