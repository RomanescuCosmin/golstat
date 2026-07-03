package ro.golstat.stats.goals;

import ro.golstat.stats.market.OverUnder;
import ro.golstat.stats.model.MatchSample;

import java.util.ArrayList;
import java.util.List;

/**
 * Calculeaza EMPIRIC probabilitatile pe piata de goluri (Over/Under, BTTS) pe o fereastra.
 * "Total meci" = goluri marcate + primite de echipa noastra in acel meci.
 * Pentru stabilizare pe esantioane mici / linii nevazute, vezi varianta Poisson.
 */
public final class GoalLines {

    /** Liniile standard afisate implicit. */
    public static final double[] STANDARD_LINES = {1.5, 2.5, 3.5};

    private GoalLines() {
    }

    public static GoalLineStats of(List<MatchSample> window) {
        return of(window, STANDARD_LINES);
    }

    public static GoalLineStats of(List<MatchSample> window, double... lines) {
        int n = window.size();
        if (n == 0) {
            List<OverUnder> empty = new ArrayList<>();
            for (double line : lines) {
                empty.add(new OverUnder(line, 0.0, 0.0));
            }
            return new GoalLineStats(0, List.copyOf(empty), 0.0);
        }

        List<OverUnder> result = new ArrayList<>();
        for (double line : lines) {
            long over = window.stream()
                    .filter(m -> m.goalsFor() + m.goalsAgainst() > line)
                    .count();
            double overRate = (double) over / n;
            result.add(new OverUnder(line, overRate, 1.0 - overRate));
        }

        long btts = window.stream()
                .filter(m -> m.goalsFor() > 0 && m.goalsAgainst() > 0)
                .count();

        return new GoalLineStats(n, List.copyOf(result), (double) btts / n);
    }
}
