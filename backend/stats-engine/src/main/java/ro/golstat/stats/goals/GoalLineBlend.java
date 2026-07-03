package ro.golstat.stats.goals;

import ro.golstat.stats.market.OverUnder;
import ro.golstat.stats.math.Poisson;
import ro.golstat.stats.model.MatchSample;

import java.util.ArrayList;
import java.util.List;

/**
 * Blend hibrid empiric ↔ Poisson pentru piata de goluri.
 *
 * <p>{@code p_final = w * p_empiric + (1 - w) * p_poisson}, cu {@code w = n / (n + K)}.
 * Esantion mare → domina empiricul (w → 1); esantion mic sau 0 → cade pe Poisson,
 * ancorat pe media ligii cand nu exista deloc meciuri.
 *
 * <p>λ pentru Poisson vine din mediile ferestrei ({@code avgGoalsFor}, {@code avgGoalsAgainst});
 * pe fereastra goala se foloseste media ligii, impartita egal intre atac si aparare.
 * Iesirea e tot un {@link GoalLineStats}, deci se afiseaza identic cu varianta empirica.
 */
public final class GoalLineBlend {

    /** Constanta de incredere pentru shrinkage: {@code w = n / (n + K)}. */
    public static final int K = 3;

    private GoalLineBlend() {
    }

    public static GoalLineStats of(List<MatchSample> window, double leagueAvgTotalGoals) {
        return of(window, leagueAvgTotalGoals, GoalLines.STANDARD_LINES);
    }

    public static GoalLineStats of(List<MatchSample> window, double leagueAvgTotalGoals, double... lines) {
        int n = window.size();
        double w = (double) n / (n + K);

        GoalStats form = GoalForm.of(window);
        double lambdaFor = n > 0 ? form.avgGoalsFor() : leagueAvgTotalGoals / 2.0;
        double lambdaAgainst = n > 0 ? form.avgGoalsAgainst() : leagueAvgTotalGoals / 2.0;
        double lambdaTotal = lambdaFor + lambdaAgainst;

        GoalLineStats empirical = GoalLines.of(window, lines);

        List<OverUnder> blended = new ArrayList<>();
        for (double line : lines) {
            double pEmpiric = empirical.line(line).overRate();
            double pPoisson = Poisson.probabilityOver(lambdaTotal, line);
            double over = w * pEmpiric + (1 - w) * pPoisson;
            blended.add(new OverUnder(line, over, 1.0 - over));
        }

        // BTTS Poisson: doua Poisson independente, P(ambele marcheaza) = (1-e^-λfor)(1-e^-λagainst)
        double bttsPoisson = (1 - Poisson.pmf(lambdaFor, 0)) * (1 - Poisson.pmf(lambdaAgainst, 0));
        double btts = w * empirical.bttsRate() + (1 - w) * bttsPoisson;

        return new GoalLineStats(n, List.copyOf(blended), btts);
    }
}
