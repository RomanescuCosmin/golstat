package ro.golstat.stats.form;

import ro.golstat.stats.model.MatchSample;

import java.util.List;

/**
 * Filtru pe calibrul adversarului (strength of schedule): pastreaza doar meciurile
 * contra echipelor din JUMATATEA DE JOS a clasamentului.
 *
 * <p>Clasamentul e 1-indexat (locul 1 = fruntea). Jumatatea de jos = {@code rank > leagueSize / 2}
 * (ex. liga de 20 → pastreaza locurile 11-20). Meciurile cu {@code opponentRank == null}
 * (necunoscut) sunt excluse. Fereastra rezultata poate fi goala (esantion 0, marcat explicit
 * de agregatele din aval prin {@code hasData()}).
 */
public final class ScheduleFilter {

    private ScheduleFilter() {
    }

    public static List<MatchSample> bottomHalfOpponents(List<MatchSample> matches, int leagueSize) {
        int threshold = leagueSize / 2;
        return matches.stream()
                .filter(m -> m.opponentRank() != null && m.opponentRank() > threshold)
                .toList();
    }
}
