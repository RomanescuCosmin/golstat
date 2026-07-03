package ro.golstat.stats.counts;

import ro.golstat.stats.market.EventLineStats;
import ro.golstat.stats.market.OverUnder;
import ro.golstat.stats.math.NegativeBinomial;
import ro.golstat.stats.math.Poisson;
import ro.golstat.stats.model.EventCountSample;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleBinaryOperator;

/**
 * Blend hibrid empiric ↔ model distributional pentru pietele numarabile Over/Under.
 *
 * <p>{@code p_final = w * p_empiric + (1 - w) * p_model}, cu {@code w = n / (n + K)} — aceeasi
 * schema ca la goluri. Media modelului vine din fereastra ({@code avgCountTotal}), cu fallback
 * pe media ligii pe fereastra goala.
 *
 * <ul>
 *   <li>{@link #overDispersed} — faulturi: model Negative Binomial (supra-dispersie).</li>
 *   <li>{@link #poisson} — cornere: model Poisson (dispersie ≈ 1).</li>
 * </ul>
 *
 * Cartonasele NU folosesc clasa asta: factorul de arbitru cere shrinkage pe medie (vezi CardMarket).
 */
public final class EventLineBlend {

    /** Constanta de incredere pentru shrinkage: {@code w = n / (n + K)}. */
    public static final int K = 3;
    public static final double[] STANDARD_FOUL_LINES = {21.5, 24.5, 27.5};
    public static final double[] STANDARD_CORNER_LINES = {8.5, 9.5, 10.5};

    private EventLineBlend() {
    }

    /** Faulturi: model Negative Binomial cu dispersia {@code dispersionR}. */
    public static EventLineStats overDispersed(List<EventCountSample> window,
                                               double leagueAvgTotal, double dispersionR, double... lines) {
        return blend(window, leagueAvgTotal, lines,
                (mean, line) -> NegativeBinomial.probabilityOver(mean, dispersionR, line));
    }

    /** Cornere: model Poisson. */
    public static EventLineStats poisson(List<EventCountSample> window,
                                         double leagueAvgTotal, double... lines) {
        return blend(window, leagueAvgTotal, lines, Poisson::probabilityOver);
    }

    private static EventLineStats blend(List<EventCountSample> window, double leagueAvgTotal,
                                        double[] lines, DoubleBinaryOperator modelOver) {
        int n = window.size();
        double w = (double) n / (n + K);
        double mean = n > 0 ? EventCountForm.of(window).avgCountTotal() : leagueAvgTotal;

        List<OverUnder> result = new ArrayList<>();
        for (double line : lines) {
            double pEmpiric = n == 0 ? 0.0 : empiricalOverRate(window, line, n);
            double pModel = modelOver.applyAsDouble(mean, line);
            double over = w * pEmpiric + (1 - w) * pModel;
            result.add(new OverUnder(line, over, 1.0 - over));
        }
        return new EventLineStats(n, List.copyOf(result));
    }

    private static double empiricalOverRate(List<EventCountSample> window, double line, int n) {
        long over = window.stream()
                .filter(m -> m.countFor() + m.countAgainst() > line)
                .count();
        return (double) over / n;
    }
}
