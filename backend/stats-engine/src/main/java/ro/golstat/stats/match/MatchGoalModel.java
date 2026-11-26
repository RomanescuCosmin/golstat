package ro.golstat.stats.match;

import ro.golstat.stats.goals.GoalStats;
import ro.golstat.stats.market.OverUnder;
import ro.golstat.stats.math.ScoreGrid;

import java.util.ArrayList;
import java.util.List;

/**
 * Modelul de goluri la nivel de MECI, doar din cele doua echipe (decizia utilizatorului, 2026-11-20:
 * fara media ligii):
 *
 * <pre>
 *   λGazde   = FACTOR · (marcate de gazde acasa + primite de oaspeti in deplasare) / 2
 *   λOaspeti = FACTOR · (marcate de oaspeti in deplasare + primite de gazde acasa) / 2
 * </pre>
 *
 * pe ultimele 7 meciuri pe locatie, cu golurile deja cantarite dupa forta adversarilor
 * ({@code GoalForm.of(window, forte)}). Media ligii intra doar cand o echipa n-are niciun meci pe locatie.
 * Cei doi λ intra in {@link ScoreGrid} (Dixon-Coles) → over/under, GG, 1X2 — aceeasi sursa pentru
 * predictie si pentru analiza pe piete.
 */
public final class MatchGoalModel {

    /** Corectia Dixon-Coles: usor pozitiva → egalurile prezise aproape de cele reale (backtest 2026-11-20). */
    public static final double RHO = 0.05;

    public static final int MAX_GOALS = 10;

    /** Backtest pe 1723 de meciuri: ferestrele dau ~3% mai putine goluri decat se marcheaza (2.99 vs 3.10). */
    public static final double FACTOR = 1.03;

    /** Podea pentru λ: o echipa cu 0 goluri in fereastra tot nu are sansa zero sa marcheze. */
    static final double LAMBDA_MINIM = 0.05;

    private static final double[] STANDARD_LINES = {0.5, 1.5, 2.5, 3.5, 4.5};

    private MatchGoalModel() {
    }

    public static MatchPrediction predict(MatchContext ctx) {
        return predict(ctx, STANDARD_LINES);
    }

    /**
     * Distributia totalului de goluri a unei predictii (aceeasi matrice Dixon-Coles, refacuta din λ):
     * {@code [k]} = P(exact k goluri), ultima pozitie = P(peste {@code max}). Baza pietelor-interval.
     */
    public static double[] distributieTotal(MatchPrediction p, int max) {
        return ScoreGrid.dixonColes(p.lambdaGazde(), p.lambdaOaspeti(), RHO, MAX_GOALS).totalDistribution(max);
    }

    public static MatchPrediction predict(MatchContext ctx, double... lines) {
        GoalStats gazde = ctx.formaGazde();
        GoalStats oaspeti = ctx.formaOaspeti();
        double gazdeMarcate = gazde.hasData() ? gazde.avgGoalsFor() : ctx.mediaLigaGazde();
        double gazdePrimite = gazde.hasData() ? gazde.avgGoalsAgainst() : ctx.mediaLigaOaspeti();
        double oaspetiMarcate = oaspeti.hasData() ? oaspeti.avgGoalsFor() : ctx.mediaLigaOaspeti();
        double oaspetiPrimite = oaspeti.hasData() ? oaspeti.avgGoalsAgainst() : ctx.mediaLigaGazde();

        double lambdaGazde = Math.max(FACTOR * (gazdeMarcate + oaspetiPrimite) / 2.0, LAMBDA_MINIM);
        double lambdaOaspeti = Math.max(FACTOR * (oaspetiMarcate + gazdePrimite) / 2.0, LAMBDA_MINIM);

        ScoreGrid grid = ScoreGrid.dixonColes(lambdaGazde, lambdaOaspeti, RHO, MAX_GOALS);

        List<OverUnder> linii = new ArrayList<>();
        for (double line : lines) {
            double over = grid.probabilityOverTotal(line);
            linii.add(new OverUnder(line, over, 1.0 - over));
        }

        return new MatchPrediction(
                lambdaGazde, lambdaOaspeti,
                grid.homeWin(), grid.draw(), grid.awayWin(),
                List.copyOf(linii), grid.btts(),
                gazde.sampleSize(), oaspeti.sampleSize());
    }
}
