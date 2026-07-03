package ro.golstat.stats.goals;

import ro.golstat.stats.model.MatchSample;

import java.util.List;

/**
 * Calculeaza empiric predictibilitatea de marcare/primire gol, SPART pe repriza 1 vs repriza 2.
 * Repriza 2 se deriva din {@code goalsForSecondHalf()} / {@code goalsAgainstSecondHalf()}.
 */
public final class GoalHalves {

    private GoalHalves() {
    }

    public static GoalHalfStats of(List<MatchSample> window) {
        int n = window.size();
        if (n == 0) {
            return new GoalHalfStats(0, 0, 0, 0, 0, 0, 0, 0, 0);
        }

        long scoredH1 = window.stream().filter(m -> m.goalsForHt() > 0).count();
        long scoredH2 = window.stream().filter(m -> m.goalsForSecondHalf() > 0).count();
        long concededH1 = window.stream().filter(m -> m.goalsAgainstHt() > 0).count();
        long concededH2 = window.stream().filter(m -> m.goalsAgainstSecondHalf() > 0).count();

        int sumForH1 = window.stream().mapToInt(MatchSample::goalsForHt).sum();
        int sumForH2 = window.stream().mapToInt(MatchSample::goalsForSecondHalf).sum();
        int sumAgainstH1 = window.stream().mapToInt(MatchSample::goalsAgainstHt).sum();
        int sumAgainstH2 = window.stream().mapToInt(MatchSample::goalsAgainstSecondHalf).sum();

        return new GoalHalfStats(
                n,
                (double) scoredH1 / n,
                (double) scoredH2 / n,
                (double) concededH1 / n,
                (double) concededH2 / n,
                (double) sumForH1 / n,
                (double) sumForH2 / n,
                (double) sumAgainstH1 / n,
                (double) sumAgainstH2 / n
        );
    }
}
