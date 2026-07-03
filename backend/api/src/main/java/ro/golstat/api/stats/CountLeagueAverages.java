package ro.golstat.api.stats;

/**
 * Mediile TOTALURILOR pe meci ale ligii (ambele echipe la un loc), rezolvate cu fallback global —
 * intra ca "media ligii" in modelele numarabile ({@code EventLineBlend}, {@code CardMarket}).
 */
public record CountLeagueAverages(double cornerePeMeci, double faulturiPeMeci, double cartonasePeMeci) {
}
