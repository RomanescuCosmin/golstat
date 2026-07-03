package ro.golstat.api.stats;

/**
 * Mediile de goluri ale ligii, rezolvate (cu fallback global aplicat) — intra ca "MediaLiga" in
 * {@code MatchGoalModel}. Separate gazde/oaspeti ca sa prinda avantajul de teren.
 */
public record LeagueAverages(double mediaLigaGazde, double mediaLigaOaspeti) {
}
