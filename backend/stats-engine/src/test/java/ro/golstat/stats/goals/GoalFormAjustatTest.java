package ro.golstat.stats.goals;

import org.junit.jupiter.api.Test;
import ro.golstat.stats.model.MatchSample;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GoalFormAjustatTest {

    private static MatchSample meci(int marcate, int primite, long adversar) {
        return new MatchSample(LocalDate.of(2026, 10, 1), true, marcate, primite, 0, 0, null, adversar);
    }

    @Test
    void golurileSeCantarescDupaAdversar() {
        // medie 1.5 goluri / echipa / meci. Adversarul 1 (20 meciuri) primeste 1.0 → gol marcat cu el valoreaza
        // (20·1.5 + 5) / 25 = 1.4; marcheaza 3.0 → gol primit de la el valoreaza (20·0.5 + 5) / 25 = 0.6.
        ForteAdversari forte = new ForteAdversari(Map.of(1L, new ForteAdversari.Forta(3.0, 1.0, 20)), 1.5);

        GoalStats s = GoalForm.of(List.of(meci(2, 1, 1L), meci(1, 1, 99L)), forte);

        assertEquals((2 * 1.4 + 1) / 2, s.avgGoalsFor(), 1e-12);   // adversar necunoscut (99) → factor 1
        assertEquals((1 * 0.6 + 1) / 2, s.avgGoalsAgainst(), 1e-12);
        assertEquals(1.0, s.scoredRate(), 1e-12);                     // ratele raman pe golurile reale
        assertEquals(2, s.sampleSize());
    }

    @Test
    void faraForte_esteFormaSimpla() {
        List<MatchSample> fereastra = List.of(meci(3, 0, 1L), meci(0, 2, 2L));
        assertEquals(GoalForm.of(fereastra), GoalForm.of(fereastra, ForteAdversari.fara()));
    }
}
