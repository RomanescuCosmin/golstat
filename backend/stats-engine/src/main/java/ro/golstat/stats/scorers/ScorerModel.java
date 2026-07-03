package ro.golstat.stats.scorers;

import ro.golstat.stats.math.Poisson;
import ro.golstat.stats.math.Shrinkage;

import java.util.ArrayList;
import java.util.List;

/**
 * Model ierarhic de marcatori prin <b>Poisson thinning</b>: daca golurile echipei sunt
 * {@code Poisson(teamLambda)} si fiecare gol e atribuit jucatorului i cu probabilitatea
 * {@code ω_i}, atunci golurile jucatorului i sunt {@code Poisson(teamLambda · ω_i)}.
 *
 * <p>Cotele {@code ω_i} se construiesc din rata istorica a fiecarui jucator (goluri/90, cu
 * shrinkage spre baseline-ul pozitiei), ponderata cu minutele asteptate si normalizata.
 * Normalizarea garanteaza {@code Σ λ_i = teamLambda} exact.
 *
 * <p>{@code teamLambda} vine din modelul de goluri (nu se recalculeaza aici).
 */
public final class ScorerModel {

    private ScorerModel() {
    }

    public static List<ScorerPrediction> of(double teamLambda, List<PlayerForm> squad) {
        double[] weights = new double[squad.size()];
        double totalWeight = 0.0;
        for (int i = 0; i < squad.size(); i++) {
            weights[i] = weight(squad.get(i));
            totalWeight += weights[i];
        }

        List<ScorerPrediction> predictions = new ArrayList<>();
        for (int i = 0; i < squad.size(); i++) {
            double share = totalWeight > 0 ? weights[i] / totalWeight : 0.0;
            double lambdaPlayer = teamLambda * share;
            predictions.add(new ScorerPrediction(
                    squad.get(i).playerId(),
                    lambdaPlayer,
                    Poisson.atLeast(lambdaPlayer, 1),   // marcheaza oricand
                    Poisson.atLeast(lambdaPlayer, 2)    // marcheaza >= 2 (brace)
            ));
        }
        return predictions;
    }

    /** Greutatea nenormalizata: rata istorica shrunk × minutele asteptate. */
    private static double weight(PlayerForm player) {
        double baseline = PositionBaseline.goalsPer90(player.position());
        double goalsPer90 = player.minutes() > 0 ? 90.0 * player.goals() / player.minutes() : baseline;
        double equivalentMatches = player.minutes() / 90.0;
        double shrunkRate = Shrinkage.toward(goalsPer90, equivalentMatches, baseline, PositionBaseline.K_POZ);
        return shrunkRate * player.expectedMinutes() / 90.0;
    }
}
