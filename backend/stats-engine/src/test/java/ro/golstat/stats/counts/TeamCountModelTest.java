package ro.golstat.stats.counts;

import org.junit.jupiter.api.Test;
import ro.golstat.stats.counts.TeamCountModel.LambdaMeci;
import ro.golstat.stats.market.EventLineStats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamCountModelTest {

    @Test
    void lambdas_attackTimesDefenceOnLocationAverages() {
        // cornere: liga 5.5 gazde / 4.5 oaspeti; K=3. Gazdele acasa: bat 7, primesc 4 (n=10);
        // oaspetii in deplasare: bat 4, primesc 6 (n=10). Fiecare raport e fata de media locatiei lui.
        LambdaMeci l = TeamCountModel.lambdas(5.5, 4.5,
                new EventCountStats(10, 7, 4), new EventCountStats(10, 4, 6));

        // 5.5 × (10·(7/5.5)+3)/13 × (10·(6/5.5)+3)/13 = 5.5 × 1.209790 × 1.069930
        assertEquals(7.119151, l.gazde(), 1e-5);
        // 4.5 × ((10·(4/4.5)+3)/13)² = 4.5 × 0.914530²
        assertEquals(3.763643, l.oaspeti(), 1e-5);
        assertEquals(l.gazde() + l.oaspeti(), l.total(), 1e-12);
    }

    @Test
    void lambdas_withoutHistory_fallOnLeagueAverages() {
        LambdaMeci l = TeamCountModel.lambdas(5.5, 4.5, new EventCountStats(0, 0, 0), new EventCountStats(0, 0, 0));
        assertEquals(5.5, l.gazde(), 1e-12);
        assertEquals(4.5, l.oaspeti(), 1e-12);
    }

    @Test
    void poisson_linesDecreaseAndComplement() {
        EventLineStats s = TeamCountModel.poisson(5.0, 14, 3.5, 4.5, 5.5);
        assertEquals(14, s.sampleSize());
        assertTrue(s.line(3.5).overRate() > s.line(4.5).overRate());
        assertTrue(s.line(4.5).overRate() > s.line(5.5).overRate());
        // P(X > 4.5 | λ=5) = 1 − P(X ≤ 4) = 0.559507
        assertEquals(0.559507, s.line(4.5).overRate(), 1e-6);
        assertEquals(1.0, s.line(4.5).overRate() + s.line(4.5).underRate(), 1e-12);
    }
}
