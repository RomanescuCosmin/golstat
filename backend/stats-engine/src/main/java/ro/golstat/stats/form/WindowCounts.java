package ro.golstat.stats.form;

import ro.golstat.stats.model.EventCountSample;
import ro.golstat.stats.model.MatchSample;

import java.util.List;
import java.util.function.Predicate;

/**
 * Contorizari empirice pe o fereastra de meciuri, pentru legendele de afisare de tip
 * "peste 9.5 cornere in 4/7 meciuri acasa". Doar numaratori brute — probabilitatile
 * modelate le dau blend-urile ({@code GoalLineBlend}, {@code EventLineBlend} etc.).
 */
public final class WindowCounts {

    private WindowCounts() {
    }

    /** In cate meciuri totalul de goluri (marcate + primite) a depasit linia. */
    public static int overTotalGoals(List<MatchSample> window, double line) {
        return count(window, m -> m.goalsFor() + m.goalsAgainst() > line);
    }

    /** In cate meciuri totalul de evenimente (ale ambelor echipe) a depasit linia. */
    public static int overTotalEvents(List<EventCountSample> window, double line) {
        return countEvents(window, m -> m.countFor() + m.countAgainst() > line);
    }

    /** In cate meciuri echipa a marcat. */
    public static int scored(List<MatchSample> window) {
        return count(window, m -> m.goalsFor() > 0);
    }

    /** In cate meciuri echipa a primit gol. */
    public static int conceded(List<MatchSample> window) {
        return count(window, m -> m.goalsAgainst() > 0);
    }

    /** In cate meciuri au marcat AMBELE echipe. */
    public static int btts(List<MatchSample> window) {
        return count(window, m -> m.goalsFor() > 0 && m.goalsAgainst() > 0);
    }

    /** In cate meciuri scorul final a fost egal. */
    public static int drawsFullTime(List<MatchSample> window) {
        return count(window, m -> m.goalsFor() == m.goalsAgainst());
    }

    /** In cate meciuri scorul la PAUZA a fost egal. */
    public static int drawsHalfTime(List<MatchSample> window) {
        return count(window, m -> m.goalsForHt() == m.goalsAgainstHt());
    }

    /** In cate meciuri s-a marcat (oricine) in prima repriza. */
    public static int goalInFirstHalf(List<MatchSample> window) {
        return count(window, m -> m.goalsForHt() + m.goalsAgainstHt() > 0);
    }

    /** In cate meciuri s-a marcat (oricine) in a doua repriza. */
    public static int goalInSecondHalf(List<MatchSample> window) {
        return count(window, m -> m.goalsForSecondHalf() + m.goalsAgainstSecondHalf() > 0);
    }

    private static int count(List<MatchSample> window, Predicate<MatchSample> conditie) {
        return (int) window.stream().filter(conditie).count();
    }

    private static int countEvents(List<EventCountSample> window, Predicate<EventCountSample> conditie) {
        return (int) window.stream().filter(conditie).count();
    }
}
