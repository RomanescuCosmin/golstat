package ro.golstat.stats.goals;

/**
 * Agregate empirice de goluri sparte pe REPRIZE, din perspectiva unei echipe.
 * Ratele sunt fractii 0..1 ("in cate meciuri a marcat/primit in repriza X").
 * Mediile sunt goluri/meci in repriza respectiva. {@code sampleSize} = nr. meciuri.
 */
public record GoalHalfStats(
        int sampleSize,
        double scoredFirstHalfRate,
        double scoredSecondHalfRate,
        double concededFirstHalfRate,
        double concededSecondHalfRate,
        double avgGoalsForFirstHalf,
        double avgGoalsForSecondHalf,
        double avgGoalsAgainstFirstHalf,
        double avgGoalsAgainstSecondHalf
) {
    public boolean hasData() {
        return sampleSize > 0;
    }
}
