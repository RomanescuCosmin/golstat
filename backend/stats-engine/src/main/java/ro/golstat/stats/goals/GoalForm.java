package ro.golstat.stats.goals;

import ro.golstat.stats.model.MatchSample;

import java.util.List;

public final class GoalForm {
    private GoalForm() {
    }

    public static GoalStats of(List<MatchSample> window) {
        int n = window.size();
        if (n == 0) {
            return new GoalStats(0, 0, 0, 0, 0);
        }

        long matchesScored = window.stream().filter(m -> m.goalsFor() > 0).count();
        long matchesConceded = window.stream().filter(m -> m.goalsAgainst() > 0).count();
        int totalGoalsFor = window.stream().mapToInt(MatchSample::goalsFor).sum();
        int totalGoalsAgainst = window.stream().mapToInt(MatchSample::goalsAgainst).sum();

        return new GoalStats(
                n,
                (double) matchesScored / n,
                (double) matchesConceded / n,
                (double) totalGoalsFor / n,
                (double) totalGoalsAgainst / n
        );
    }

    /**
     * Ca {@link #of(List)}, dar cu golurile cantarite dupa adversar ({@link ForteAdversari}): un gol marcat
     * contra unei aparari tari conteaza mai mult, unul primit de la un atac slab conteaza mai greu.
     * Ratele (a marcat / a primit in X din N) raman pe golurile reale.
     */
    public static GoalStats of(List<MatchSample> window, ForteAdversari forte) {
        GoalStats brut = of(window);
        if (window.isEmpty()) {
            return brut;
        }
        double marcate = window.stream()
                .mapToDouble(m -> m.goalsFor() * forte.factorMarcate(m.opponentId())).sum();
        double primite = window.stream()
                .mapToDouble(m -> m.goalsAgainst() * forte.factorPrimite(m.opponentId())).sum();
        return new GoalStats(brut.sampleSize(), brut.scoredRate(), brut.concededRate(),
                marcate / window.size(), primite / window.size());
    }
}
