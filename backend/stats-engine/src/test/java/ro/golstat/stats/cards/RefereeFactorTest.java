package ro.golstat.stats.cards;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RefereeFactorTest {

    @Test
    void shrunkFactor_designExample() {
        // arbitru 5.5 pe 18 meciuri, liga 4.2 → (141/28)/4.2 = 1.198980 (in interiorul clamp-ului)
        assertEquals(1.198980, RefereeFactor.of(5.5, 18, 4.2), 1e-6);
    }

    @Test
    void clampsHighFactor() {
        // 8.8333/3.0 = 2.94 → clamp 1.3
        assertEquals(1.3, RefereeFactor.of(10.0, 50, 3.0), 1e-9);
    }

    @Test
    void clampsLowFactor() {
        // 1.5/4.0 = 0.375 → clamp 0.7
        assertEquals(0.7, RefereeFactor.of(1.0, 50, 4.0), 1e-9);
    }

    @Test
    void unknownReferee_zeroMatches_isNeutral() {
        // media arbitrului ignorata (greutate 0) → factor 1.0
        assertEquals(1.0, RefereeFactor.of(9.9, 0, 4.2), 1e-9);
    }

    @Test
    void nonPositiveLeagueAverage_isNeutral() {
        assertEquals(RefereeFactor.NEUTRAL, RefereeFactor.of(5.0, 10, 0.0), 1e-9);
    }

    @Test
    void neutralIsOne() {
        assertEquals(1.0, RefereeFactor.NEUTRAL, 1e-9);
    }
}
