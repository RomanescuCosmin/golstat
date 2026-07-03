package ro.golstat.stats.goals;

import org.junit.jupiter.api.Test;
import ro.golstat.stats.math.Poisson;
import ro.golstat.stats.model.MatchSample;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoalLineBlendTest {

    private static final double EPS = 1e-9;

    private static MatchSample match(int goalsFor, int goalsAgainst) {
        return new MatchSample(LocalDate.of(2024, 1, 1), true, goalsFor, goalsAgainst, 0, 0, null);
    }

    // Totaluri 3,0,2,5,1 (n=5); avgFor=1.4, avgAgainst=0.8, lambdaTotal=2.2; w = 5/(5+3) = 0.625
    private static List<MatchSample> sampleWindow() {
        return List.of(match(2, 1), match(0, 0), match(1, 1), match(3, 2), match(1, 0));
    }

    @Test
    void emptyWindow_isPurePoissonOnLeagueMean() {
        double leagueAvg = 2.6;
        GoalLineStats stats = GoalLineBlend.of(List.of(), leagueAvg);

        assertEquals(0, stats.sampleSize());
        assertFalse(stats.hasData());
        // w=0 → pur Poisson pe media ligii
        assertEquals(Poisson.probabilityOver(leagueAvg, 2.5), stats.line(2.5).overRate(), EPS);
        double half = leagueAvg / 2.0;
        double bttsExpected = (1 - Poisson.pmf(half, 0)) * (1 - Poisson.pmf(half, 0));
        assertEquals(bttsExpected, stats.bttsRate(), EPS);
    }

    @Test
    void blend_matchesManualComposition() {
        // p_final = w*p_emp + (1-w)*p_poisson, w=0.625, lambdaTotal=2.2
        double w = 5.0 / 8.0;
        GoalLineStats emp = GoalLines.of(sampleWindow());
        GoalLineStats blend = GoalLineBlend.of(sampleWindow(), 2.6);

        for (double line : GoalLines.STANDARD_LINES) {
            double expected = w * emp.line(line).overRate() + (1 - w) * Poisson.probabilityOver(2.2, line);
            assertEquals(expected, blend.line(line).overRate(), EPS, "linia " + line);
        }
    }

    @Test
    void blend_over2p5_knownValue() {
        // 0.625*0.4 + 0.375*0.377286 = 0.391482
        assertEquals(0.391482, GoalLineBlend.of(sampleWindow(), 2.6).line(2.5).overRate(), 1e-6);
    }

    @Test
    void btts_matchesManualComposition() {
        double w = 5.0 / 8.0;
        double bttsEmp = GoalLines.of(sampleWindow()).bttsRate();       // 0.6
        double bttsPoi = (1 - Poisson.pmf(1.4, 0)) * (1 - Poisson.pmf(0.8, 0));
        double expected = w * bttsEmp + (1 - w) * bttsPoi;
        assertEquals(expected, GoalLineBlend.of(sampleWindow(), 2.6).bttsRate(), EPS);
    }

    @Test
    void whenSampleIsPositive_leagueMeanIsIgnored() {
        // lambda vine din fereastra, nu din liga → rezultatul nu depinde de leagueAvg
        GoalLineStats a = GoalLineBlend.of(sampleWindow(), 2.6);
        GoalLineStats b = GoalLineBlend.of(sampleWindow(), 3.5);
        assertEquals(a.line(2.5).overRate(), b.line(2.5).overRate(), EPS);
        assertEquals(a.bttsRate(), b.bttsRate(), EPS);
    }

    @Test
    void blendedValue_liesBetweenEmpiricalAndPoisson() {
        GoalLineStats emp = GoalLines.of(sampleWindow());
        GoalLineStats blend = GoalLineBlend.of(sampleWindow(), 2.6);

        double e = emp.line(2.5).overRate();                    // 0.4
        double p = Poisson.probabilityOver(2.2, 2.5);           // ~0.377
        double b = blend.line(2.5).overRate();
        assertTrue(b >= Math.min(e, p) && b <= Math.max(e, p));
    }

    @Test
    void overPlusUnder_equalsOne() {
        GoalLineStats stats = GoalLineBlend.of(sampleWindow(), 2.6);
        for (GoalLineStats.OverUnder ou : stats.lines()) {
            assertEquals(1.0, ou.overRate() + ou.underRate(), EPS, "linia " + ou.line());
        }
    }

    @Test
    void smallSample_leansMoreOnPoissonThanLargeSample() {
        // aceeasi rata empirica (mereu over 2.5), dar esantion diferit → w diferit
        List<MatchSample> small = List.of(match(2, 1));                       // n=1, w=0.25
        List<MatchSample> large = List.of(
                match(2, 1), match(2, 1), match(2, 1), match(2, 1), match(2, 1),
                match(2, 1), match(2, 1), match(2, 1), match(2, 1)            // n=9, w=0.75
        );
        double empOver = 1.0; // ambele au total 3 > 2.5 in fiecare meci
        double smallOver = GoalLineBlend.of(small, 2.6).line(2.5).overRate();
        double largeOver = GoalLineBlend.of(large, 2.6).line(2.5).overRate();

        // esantionul mare trage mai aproape de empiric (1.0) decat cel mic
        assertTrue(Math.abs(largeOver - empOver) < Math.abs(smallOver - empOver));
    }

    @Test
    void sampleSizePreserved_forConfidenceDisplay() {
        assertEquals(5, GoalLineBlend.of(sampleWindow(), 2.6).sampleSize());
    }
}
