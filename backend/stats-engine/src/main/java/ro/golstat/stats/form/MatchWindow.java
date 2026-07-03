package ro.golstat.stats.form;

import ro.golstat.stats.model.MatchLocation;
import ro.golstat.stats.model.TeamMatch;

import java.util.Comparator;
import java.util.List;

/** Selectia ferestrelor de meciuri. Generic peste orice {@link TeamMatch} (goluri, cornere...). */
public final class MatchWindow {
    private MatchWindow() {

    }

    public static <T extends TeamMatch> List<T> lastN(List<T> matches, int n) {
        return matches.stream()
                .sorted(Comparator.comparing(TeamMatch::date).reversed())
                .limit(n)
                .toList();
    }


    public static <T extends TeamMatch> List<T> lastN(List<T> matches, int n, MatchLocation matchLocation) {
        List<T> filtered = matches.stream()
                .filter(m -> m.home() == (matchLocation == MatchLocation.HOME))
                .toList();
        return lastN(filtered, n);
    }
}
