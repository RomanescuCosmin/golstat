package ro.golstat.stats.cards;

import ro.golstat.stats.math.Shrinkage;

/**
 * Factor multiplicativ de arbitru pentru piata de cartonase: cat de "carton-dornic" e arbitrul
 * fata de media ligii. Media lui de cartonase/meci e regresata spre media ligii dupa numarul de
 * meciuri arbitrate (esantion mic → aproape de 1), apoi impartita la media ligii si limitata
 * (clamp) in {@code [0.7, 1.3]}. Arbitru necunoscut ⇒ {@link #NEUTRAL} (1.0), fara efect.
 */
public final class  RefereeFactor {

    public static final double NEUTRAL = 1.0;

    private static final double K = 10;
    private static final double MIN = 0.7;
    private static final double MAX = 1.3;

    private RefereeFactor() {
    }

    public static double of(double refereeAvgCards, double refereeMatches, double leagueAvgCards) {
        if (leagueAvgCards <= 0) {
            return NEUTRAL;
        }
        double shrunkMean = Shrinkage.toward(refereeAvgCards, refereeMatches, leagueAvgCards, K);
        double factor = shrunkMean / leagueAvgCards;
        return Math.max(MIN, Math.min(MAX, factor));
    }
}
