package ro.golstat.stats.match;

import ro.golstat.stats.goals.GoalStats;
import ro.golstat.stats.market.OverUnder;
import ro.golstat.stats.math.ScoreGrid;
import ro.golstat.stats.math.Shrinkage;

import java.util.ArrayList;
import java.util.List;

/**
 * Modelul de goluri la nivel de MECI. Cei doi λ vin din formula clasica atac × aparare, ancorata pe
 * media ligii:
 *
 * <pre>
 *   λGazde   = MediaLigaGazde   × FortaAtacGazde   × SlabiciuneApararaOaspeti
 *   λOaspeti = MediaLigaOaspeti × FortaAtacOaspeti × SlabiciuneApararaGazde
 * </pre>
 *
 * unde factorii sunt rate relative la media ligii (ex. {@code avgGoalsFor / mediaLiga}), fiecare
 * tras spre {@code 1.0} (media ligii) prin {@link Shrinkage} — esantion mic ⇒ factor ≈ 1, deci λ
 * cade pe media ligii. Cei doi λ intra in {@link ScoreGrid} (Dixon-Coles) → over/under, BTTS, 1X2.
 *
 * <p>Java pur, determinist. Nu decide ferestrele (acasa/deplasare) si nu calculeaza media ligii —
 * alea vin gata in {@link MatchContext}.
 */
public final class MatchGoalModel {

    /** Constanta de shrinkage a factorilor spre media ligii (aliniata cu {@code GoalLineBlend.K}). */
    public static final double K = 3;

    /** Corectia Dixon-Coles: usor negativa → putin mai multe egaluri mici (0-0, 1-1). */
    public static final double RHO = -0.1;

    public static final int MAX_GOALS = 10;

    private static final double[] STANDARD_LINES = {0.5, 1.5, 2.5, 3.5, 4.5};

    private MatchGoalModel() {
    }

    public static MatchPrediction predict(MatchContext ctx) {
        return predict(ctx, STANDARD_LINES);
    }

    public static MatchPrediction predict(MatchContext ctx, double... lines) {
        GoalStats gazde = ctx.gazdaAcasa();
        GoalStats oaspeti = ctx.oaspetiDeplasare();

        double lambdaGazde = lambda(
                ctx.mediaLigaGazde(),
                gazde.avgGoalsFor(), gazde.sampleSize(),
                oaspeti.avgGoalsAgainst(), oaspeti.sampleSize());
        double lambdaOaspeti = lambda(
                ctx.mediaLigaOaspeti(),
                oaspeti.avgGoalsFor(), oaspeti.sampleSize(),
                gazde.avgGoalsAgainst(), gazde.sampleSize());

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

    /** λ = mediaLiga × fortaAtac × slabiciuneAparare, cu ambii factori trasi spre 1.0 la esantion mic. */
    private static double lambda(double mediaLiga, double atac, int nAtac, double aparare, int nAparare) {
        double fortaAtac = Shrinkage.toward(ratio(atac, mediaLiga), nAtac, 1.0, K);
        double slabiciuneAparare = Shrinkage.toward(ratio(aparare, mediaLiga), nAparare, 1.0, K);
        return mediaLiga * fortaAtac * slabiciuneAparare;
    }

    /** Rata relativa la media ligii; media 0 (liga fara meciuri terminale) → factor neutru 1.0. */
    private static double ratio(double value, double mediaLiga) {
        return mediaLiga > 0 ? value / mediaLiga : 1.0;
    }
}
