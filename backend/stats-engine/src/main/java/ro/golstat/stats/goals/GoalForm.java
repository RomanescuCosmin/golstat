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
}
