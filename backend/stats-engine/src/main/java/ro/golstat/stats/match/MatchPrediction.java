package ro.golstat.stats.match;

import ro.golstat.stats.market.OverUnder;

import java.util.List;

/**
 * Predictia unui meci: cei doi λ (asteptarea de goluri gazde/oaspeti), 1X2, liniile over/under si
 * BTTS. {@code esantionGazde}/{@code esantionOaspeti} = marimea ferestrelor pe care s-a calculat
 * (masura de incredere — mica la echipe/competitii cu putine meciuri, ex. Campionatul Mondial).
 * Ratele sunt fractii 0..1; stratul de afisare le inmulteste cu 100.
 */
public record MatchPrediction(
        double lambdaGazde,
        double lambdaOaspeti,
        double sansaGazde,
        double sansaEgal,
        double sansaOaspeti,
        List<OverUnder> linii,
        double btts,
        int esantionGazde,
        int esantionOaspeti
) {
    /** Rezultatul pentru o anumita linie (ex. 2.5); null daca linia n-a fost calculata. */
    public OverUnder linie(double line) {
        return linii.stream()
                .filter(l -> l.line() == line)
                .findFirst()
                .orElse(null);
    }
}
