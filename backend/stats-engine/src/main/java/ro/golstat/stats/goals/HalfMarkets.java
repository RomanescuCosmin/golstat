package ro.golstat.stats.goals;

import ro.golstat.stats.form.WindowCounts;
import ro.golstat.stats.match.MatchGoalModel;
import ro.golstat.stats.math.Poisson;
import ro.golstat.stats.math.ScoreGrid;
import ro.golstat.stats.model.MatchSample;

import java.util.List;

/**
 * Pietele pe REPRIZE ale unui meci: egal la pauza + "se marcheaza in repriza 1 / 2".
 *
 * <p>Modelul: λ-urile pe repriza ale fiecarei parti se estimeaza incrucisat — atacul gazdei pe
 * repriza x mediat cu apararea oaspetilor pe aceeasi repriza (si invers) — apoi scorul la pauza
 * e o grila Dixon-Coles pe λ-urile reprizei 1 (corectia conteaza exact la scorurile mici, care
 * domina pauza), iar "gol in repriza" e coada Poisson a totalului reprizei.
 *
 * <p>Empiric ↔ model: {@code p = w·empiric + (1-w)·model}, {@code w = n/(n+K)}, aceeasi schema ca
 * {@link GoalLineBlend}; fereastra = ambele ferestre concatenate (fiecare meci vazut din
 * perspectiva echipei lui).
 */
public final class HalfMarkets {

    /** Constanta de incredere pentru shrinkage: {@code w = n / (n + K)}. */
    public static final int K = 3;

    private HalfMarkets() {
    }

    public static HalfMarketStats of(List<MatchSample> gazdaWindow, List<MatchSample> oaspetiWindow) {
        int n = gazdaWindow.size() + oaspetiWindow.size();
        if (n == 0) {
            return new HalfMarketStats(0, 0, 0, 0);
        }

        GoalHalfStats gazde = GoalHalves.of(gazdaWindow);
        GoalHalfStats oaspeti = GoalHalves.of(oaspetiWindow);

        double lambdaGazdeH1 = incrucisat(gazde.avgGoalsForFirstHalf(), gazdaWindow.size(),
                oaspeti.avgGoalsAgainstFirstHalf(), oaspetiWindow.size());
        double lambdaOaspetiH1 = incrucisat(oaspeti.avgGoalsForFirstHalf(), oaspetiWindow.size(),
                gazde.avgGoalsAgainstFirstHalf(), gazdaWindow.size());
        double lambdaGazdeH2 = incrucisat(gazde.avgGoalsForSecondHalf(), gazdaWindow.size(),
                oaspeti.avgGoalsAgainstSecondHalf(), oaspetiWindow.size());
        double lambdaOaspetiH2 = incrucisat(oaspeti.avgGoalsForSecondHalf(), oaspetiWindow.size(),
                gazde.avgGoalsAgainstSecondHalf(), gazdaWindow.size());

        double modelEgalPauza = ScoreGrid
                .dixonColes(lambdaGazdeH1, lambdaOaspetiH1, MatchGoalModel.RHO, MatchGoalModel.MAX_GOALS)
                .draw();
        double modelGolH1 = Poisson.probabilityOver(lambdaGazdeH1 + lambdaOaspetiH1, 0.5);
        double modelGolH2 = Poisson.probabilityOver(lambdaGazdeH2 + lambdaOaspetiH2, 0.5);

        double empiricEgalPauza = rata(WindowCounts.drawsHalfTime(gazdaWindow)
                + WindowCounts.drawsHalfTime(oaspetiWindow), n);
        double empiricGolH1 = rata(WindowCounts.goalInFirstHalf(gazdaWindow)
                + WindowCounts.goalInFirstHalf(oaspetiWindow), n);
        double empiricGolH2 = rata(WindowCounts.goalInSecondHalf(gazdaWindow)
                + WindowCounts.goalInSecondHalf(oaspetiWindow), n);

        double w = (double) n / (n + K);
        return new HalfMarketStats(n,
                w * empiricEgalPauza + (1 - w) * modelEgalPauza,
                w * empiricGolH1 + (1 - w) * modelGolH1,
                w * empiricGolH2 + (1 - w) * modelGolH2);
    }

    /** Atacul unei parti mediat cu apararea celeilalte; o fereastra goala lasa doar cealalta parte. */
    private static double incrucisat(double atac, int nAtac, double aparare, int nAparare) {
        if (nAtac == 0 && nAparare == 0) {
            return 0.0;
        }
        if (nAtac == 0) {
            return aparare;
        }
        if (nAparare == 0) {
            return atac;
        }
        return (atac + aparare) / 2.0;
    }

    private static double rata(int hits, int n) {
        return (double) hits / n;
    }

    /** Ratele finale (0..1) ale pietelor pe reprize; {@code sampleSize} = meciuri in ambele ferestre. */
    public record HalfMarketStats(
            int sampleSize,
            double htDrawRate,
            double goalInFirstHalfRate,
            double goalInSecondHalfRate
    ) {
        public boolean hasData() {
            return sampleSize > 0;
        }
    }
}
