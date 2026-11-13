package ro.golstat.stats.counts;

import ro.golstat.stats.market.EventLineStats;
import ro.golstat.stats.market.OverUnder;
import ro.golstat.stats.math.AttackDefence;
import ro.golstat.stats.math.NegativeBinomial;
import ro.golstat.stats.math.Poisson;

import java.util.ArrayList;
import java.util.List;

/**
 * Pietele numarabile la nivel de ECHIPA (cornerele / cartonasele / faulturile unei singure echipe),
 * pe aceeasi schema ca {@code MatchGoalModel}:
 *
 * <pre>
 *   λ_gazde   = MediaLigaGazde   × AtacGazde   × SlabiciuneOaspeti
 *   λ_oaspeti = MediaLigaOaspeti × AtacOaspeti × SlabiciuneGazde
 * </pre>
 *
 * cu factorii din {@link AttackDefence} (gazdele pe meciurile de ACASA, oaspetii pe cele din DEPLASARE,
 * fiecare raportat la media ligii pe locatia lui). Totalul meciului e suma.
 */
public final class TeamCountModel {

    public static final double K = AttackDefence.K;

    /** λ-urile celor doua echipe dintr-un meci. */
    public record LambdaMeci(double gazde, double oaspeti) {
        public double total() {
            return gazde + oaspeti;
        }
    }

    private TeamCountModel() {
    }

    /**
     * @param mediaGazde   media ligii pe meci pentru echipa gazda (ex. cornere batute de gazde)
     * @param mediaOaspeti idem pentru oaspeti
     * @param gazde        forma gazdelor acasa
     * @param oaspeti      forma oaspetilor in deplasare
     */
    public static LambdaMeci lambdas(double mediaGazde, double mediaOaspeti,
                                     EventCountStats gazde, EventCountStats oaspeti) {
        return new LambdaMeci(
                mediaGazde * AttackDefence.factor(mediaGazde, gazde.avgCountFor(), gazde.sampleSize(),
                        oaspeti.avgCountAgainst(), oaspeti.sampleSize(), K),
                mediaOaspeti * AttackDefence.factor(mediaOaspeti, oaspeti.avgCountFor(), oaspeti.sampleSize(),
                        gazde.avgCountAgainst(), gazde.sampleSize(), K));
    }

    /** λ-ul unei echipe pe o medie unica (folosit de backtest pentru variantele pe locatie). */
    public static double lambda(double mediaLigaPeEchipa, EventCountStats echipa, EventCountStats adversar,
                                double k) {
        return mediaLigaPeEchipa * AttackDefence.factor(mediaLigaPeEchipa, echipa.avgCountFor(),
                echipa.sampleSize(), adversar.avgCountAgainst(), adversar.sampleSize(), k);
    }

    /** Linii Over/Under dintr-un λ, cu distributie Poisson (cornere). */
    public static EventLineStats poisson(double lambda, int sampleSize, double... lines) {
        List<OverUnder> result = new ArrayList<>();
        for (double line : lines) {
            double over = Poisson.probabilityOver(lambda, line);
            result.add(new OverUnder(line, over, 1.0 - over));
        }
        return new EventLineStats(sampleSize, List.copyOf(result));
    }

    /** Linii Over/Under dintr-o medie, cu Negative Binomial de dispersie {@code r} (cartonase, faulturi). */
    public static EventLineStats overDispersed(double mean, double dispersionR, int sampleSize, double... lines) {
        List<OverUnder> result = new ArrayList<>();
        for (double line : lines) {
            double over = NegativeBinomial.probabilityOver(mean, dispersionR, line);
            result.add(new OverUnder(line, over, 1.0 - over));
        }
        return new EventLineStats(sampleSize, List.copyOf(result));
    }
}
