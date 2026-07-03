package ro.golstat.stats.form;

import ro.golstat.stats.model.MatchLocation;
import ro.golstat.stats.model.MatchSample;

import java.util.Comparator;
import java.util.List;

public final class MatchWindow {
    private MatchWindow() {

    }

    public static List<MatchSample> lastN(List<MatchSample> matches, int n) {
        return matches.stream()
                .sorted(Comparator.comparing(MatchSample::date).reversed())
                .limit(n)
                .toList();
    }


    public static List<MatchSample> lastN(List<MatchSample> matches, int n, MatchLocation matchLocation) {
        List<MatchSample> filtered = matches.stream()
                .filter(m -> m.home() == (matchLocation == MatchLocation.HOME))
                .toList();
        return lastN(filtered, n);
    }
}
