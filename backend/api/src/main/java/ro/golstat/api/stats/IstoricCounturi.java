package ro.golstat.api.stats;

import ro.golstat.api.entity.FixtureTeamStats;
import ro.golstat.stats.model.EventCountSample;

import java.util.List;

/**
 * Ferestrele de count-uri ale unei echipe pe piata (cate un sample per meci, totalul meciului fiind
 * {@code countFor + countAgainst}) plus randurile PROPRII de statistici (pentru mediile afisate).
 * Listele pot avea lungimi diferite: un meci intra doar in pietele cu valori prezente.
 * {@code cornereRepriza1/2} vin din {@code fixture_team_half_stats} (meciurile fara reprize lipsesc).
 */
public record IstoricCounturi(
        List<EventCountSample> cornere,
        List<EventCountSample> faulturi,
        List<EventCountSample> cartonase,
        List<EventCountSample> suturi,
        List<EventCountSample> suturiPePoarta,
        List<FixtureTeamStats> statisticiEchipa,
        List<EventCountSample> cornereRepriza1,
        List<EventCountSample> cornereRepriza2
) {

    public IstoricCounturi(List<EventCountSample> cornere, List<EventCountSample> faulturi,
                           List<EventCountSample> cartonase, List<EventCountSample> suturi,
                           List<EventCountSample> suturiPePoarta, List<FixtureTeamStats> statisticiEchipa) {
        this(cornere, faulturi, cartonase, suturi, suturiPePoarta, statisticiEchipa, List.of(), List.of());
    }

    public static IstoricCounturi gol() {
        return new IstoricCounturi(List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }
}
