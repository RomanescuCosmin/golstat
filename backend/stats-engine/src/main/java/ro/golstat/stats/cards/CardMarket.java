package ro.golstat.stats.cards;

import ro.golstat.stats.counts.EventCountForm;
import ro.golstat.stats.market.EventLineStats;
import ro.golstat.stats.market.OverUnder;
import ro.golstat.stats.math.NegativeBinomial;
import ro.golstat.stats.math.Shrinkage;
import ro.golstat.stats.model.EventCountSample;

import java.util.ArrayList;
import java.util.List;

/**
 * Piata de cartonase. Spre deosebire de goluri/faulturi (blend pe probabilitate), aici factorul
 * de arbitru trebuie sa intre cu greutate INTREAGA — deci facem shrinkage pe MEDIE, nu pe
 * probabilitate, apoi un singur Negative Binomial:
 *
 * <pre>
 *   m_echipe = shrinkage(avgCardsFor + avgCardsAgainst  spre media ligii,  w = n/(n+K))
 *   m_meci   = m_echipe · factorArbitru
 *   P(Over L) = NegativeBinomial(m_meci, r)
 * </pre>
 *
 * Rata empirica bruta (cate din ultimele meciuri au fost peste linie) se afiseaza separat ca
 * statistica observata — NU se amesteca in procentul final, tocmai ca arbitrul sa nu fie diluat.
 */
public final class CardMarket {

    /** Constanta de incredere pentru shrinkage-ul pe medie: {@code w = n/(n+K)}. */
    public static final int K = 3;
    public static final double[] STANDARD_LINES = {3.5, 4.5, 5.5};

    private CardMarket() {
    }

    public static EventLineStats of(List<EventCountSample> window, double leagueAvgTotalCards,
                                    double refereeFactor, double dispersionR, double... lines) {
        int n = window.size();
        double teamMean = n > 0 ? EventCountForm.of(window).avgCountTotal() : leagueAvgTotalCards;
        double shrunkMean = Shrinkage.toward(teamMean, n, leagueAvgTotalCards, K);
        double matchMean = shrunkMean * refereeFactor;

        List<OverUnder> result = new ArrayList<>();
        for (double line : lines) {
            double over = NegativeBinomial.probabilityOver(matchMean, dispersionR, line);
            result.add(new OverUnder(line, over, 1.0 - over));
        }
        return new EventLineStats(n, List.copyOf(result));
    }
}
