package ro.golstat.stats.goals;

/**
 * Agregate empirice de goluri pe o fereastra de meciuri, din perspectiva unei echipe.
 * Ratele sunt fractii 0..1 (ex. 0.75 = 75%); stratul de afisare le inmulteste cu 100.
 * {@code sampleSize} spune pe cate meciuri s-a calculat (esantionul).
 */
public record GoalStats(
        int sampleSize,
        double scoredRate,
        double concededRate,
        double avgGoalsFor,
        double avgGoalsAgainst
) {
    public boolean hasData() {
        return sampleSize > 0;
    }
}