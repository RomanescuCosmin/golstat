package ro.golstat.stats.odds;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OddsTest {

    private static final double EPS = 1e-9;

    @Test
    void fromProbability_isReciprocal() {
        assertEquals(2.0, Odds.fromProbability(0.5), EPS);
        assertEquals(4.0, Odds.fromProbability(0.25), EPS);
        assertEquals(1.0 / 0.75, Odds.fromProbability(0.75), EPS); // ~1.3333
    }

    @Test
    void certainEvent_hasOddsOne() {
        assertEquals(1.0, Odds.fromProbability(1.0), EPS);
    }

    @Test
    void zeroProbability_isInfiniteOdds() {
        assertTrue(Double.isInfinite(Odds.fromProbability(0.0)));
    }

    @Test
    void probabilityOutsideUnitInterval_throws() {
        assertThrows(IllegalArgumentException.class, () -> Odds.fromProbability(-0.01));
        assertThrows(IllegalArgumentException.class, () -> Odds.fromProbability(1.01));
    }

    @Test
    void withMargin_lowersTheOdds() {
        // 1 / (0.5 * 1.05) = 1.904761...
        assertEquals(1.0 / (0.5 * 1.05), Odds.withMargin(0.5, 0.05), EPS);
        assertTrue(Odds.withMargin(0.5, 0.05) < Odds.fromProbability(0.5));
    }

    @Test
    void withMargin_zero_equalsFairOdds() {
        assertEquals(Odds.fromProbability(0.4), Odds.withMargin(0.4, 0.0), EPS);
    }

    @Test
    void negativeMargin_throws() {
        assertThrows(IllegalArgumentException.class, () -> Odds.withMargin(0.5, -0.1));
    }
}
