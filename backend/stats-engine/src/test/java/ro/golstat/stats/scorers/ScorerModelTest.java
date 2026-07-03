package ro.golstat.stats.scorers;

import org.junit.jupiter.api.Test;
import ro.golstat.stats.math.Poisson;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScorerModelTest {

    private static final double EPS = 1e-9;

    private static PlayerForm player(long id, Position pos, int goals, int minutes, int expectedMinutes) {
        return new PlayerForm(id, pos, goals, minutes, expectedMinutes);
    }

    // Lot de referinta:
    //   A (FORWARD):    10 goluri / 900 min → g90=1.0, shrunk toward(1.0,10,0.35,5)=11.75/15
    //   B (MIDFIELDER):  2 goluri / 900 min → g90=0.2, shrunk toward(0.2,10,0.12,5)= 2.6/15
    //   ambii 90 min asteptate. total = 14.35/15. teamLambda = 1.5.
    private static List<PlayerForm> squadAB() {
        return List.of(
                player(1, Position.FORWARD, 10, 900, 90),
                player(2, Position.MIDFIELDER, 2, 900, 90)
        );
    }

    @Test
    void lambdasSumToTeamLambda() {
        double team = 1.5;
        double sum = ScorerModel.of(team, squadAB()).stream()
                .mapToDouble(ScorerPrediction::lambdaPlayer).sum();
        assertEquals(team, sum, EPS);
    }

    @Test
    void lambdaShares_knownValues() {
        List<ScorerPrediction> preds = ScorerModel.of(1.5, squadAB());
        assertEquals(17.625 / 14.35, preds.get(0).lambdaPlayer(), 1e-9); // A: 1.5·11.75/14.35
        assertEquals(3.9 / 14.35, preds.get(1).lambdaPlayer(), 1e-9);    // B: 1.5·2.6/14.35
    }

    @Test
    void probabilitiesDerivedFromPoisson() {
        for (ScorerPrediction p : ScorerModel.of(1.5, squadAB())) {
            assertEquals(Poisson.atLeast(p.lambdaPlayer(), 1), p.anytimeProbability(), EPS);
            assertEquals(Poisson.atLeast(p.lambdaPlayer(), 2), p.braceProbability(), EPS);
        }
    }

    @Test
    void injuredPlayer_zeroExpectedMinutes_zeroLambda() {
        List<PlayerForm> squad = List.of(
                player(1, Position.FORWARD, 10, 900, 90),
                player(2, Position.FORWARD, 8, 900, 0)   // accidentat
        );
        List<ScorerPrediction> preds = ScorerModel.of(1.5, squad);
        assertEquals(0.0, preds.get(1).lambdaPlayer(), EPS);
        assertEquals(0.0, preds.get(1).anytimeProbability(), EPS);
        // toata masa merge la jucatorul disponibil
        assertEquals(1.5, preds.get(0).lambdaPlayer(), EPS);
    }

    @Test
    void debutant_zeroMinutesPlayed_getsPositionBaseline() {
        // fara minute istorice → rata = baseline pozitiei, deci λ pozitiv (nu 0)
        List<ScorerPrediction> preds = ScorerModel.of(1.5,
                List.of(player(1, Position.FORWARD, 0, 0, 90)));
        assertEquals(1.5, preds.get(0).lambdaPlayer(), EPS); // singurul jucator → toata masa
    }

    @Test
    void forwardOutscoresDefender_viaBaseline() {
        // ambii fara goluri si aceleasi minute → doar baseline-ul pozitiei ii diferentiaza
        List<ScorerPrediction> preds = ScorerModel.of(1.5, List.of(
                player(1, Position.FORWARD, 0, 900, 90),
                player(2, Position.DEFENDER, 0, 900, 90)
        ));
        assertTrue(preds.get(0).lambdaPlayer() > preds.get(1).lambdaPlayer());
    }

    @Test
    void allUnavailable_allLambdaZero() {
        List<ScorerPrediction> preds = ScorerModel.of(1.5, List.of(
                player(1, Position.FORWARD, 10, 900, 0),
                player(2, Position.MIDFIELDER, 5, 900, 0)
        ));
        assertTrue(preds.stream().allMatch(p -> p.lambdaPlayer() == 0.0));
    }

    @Test
    void emptySquad_returnsEmpty() {
        assertTrue(ScorerModel.of(1.5, List.of()).isEmpty());
    }
}
