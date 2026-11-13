package ro.golstat.stats.match;

import ro.golstat.stats.goals.GoalStats;

/**
 * Intrarea modelului de meci (gazde vs. oaspeti): forma gazdelor pe ultimele meciuri de ACASA si a
 * oaspetilor pe ultimele din DEPLASARE, plus mediile de goluri ale ligii separate gazde/oaspeti — folosite doar cand o echipa n-are meciuri pe
 * locatie. Numerele le pregateste stratul {@code api}; motorul ramane agnostic la entitati/DB.
 */
public record MatchContext(
        GoalStats formaGazde,
        GoalStats formaOaspeti,
        double mediaLigaGazde,
        double mediaLigaOaspeti
) {
}
