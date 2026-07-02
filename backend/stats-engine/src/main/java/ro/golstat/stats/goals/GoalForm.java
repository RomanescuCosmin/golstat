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

        long scored = window.stream().filter(m -> m.goalsFor() > 0).count();
        long conceded = window.stream().filter(m -> m.goalsAgainst() > 0).count();
        int sumFor = window.stream().mapToInt(MatchSample::goalsFor).sum();
        int sumAgainst = window.stream().mapToInt(MatchSample::goalsAgainst).sum();

        return new GoalStats(
                n,
                (double) scored / n,
                (double) conceded / n,
                (double) sumFor / n,
                (double) sumAgainst / n
        );
    }
}
