package ro.golstat.stats.match;

import ro.golstat.stats.goals.GoalStats;

/**
 * Intrarea modelului de meci (gazde vs. oaspeti), din perspectiva fiecarei echipe pe LOCATIA ei:
 * forma gazdelor pe meciurile lor de ACASA si a oaspetilor pe cele din DEPLASARE, plus mediile de
 * goluri ale ligii (separate gazde/oaspeti, ca sa prinda avantajul de teren). Numerele le pregateste
 * stratul {@code api}; motorul ramane agnostic la entitati/DB.
 */
public record MatchContext(
        GoalStats gazdaAcasa,
        GoalStats oaspetiDeplasare,
        double mediaLigaGazde,
        double mediaLigaOaspeti
) {
}
