package ro.golstat.api.stats;

import ro.golstat.api.entity.FixtureTeamStats;
import ro.golstat.stats.model.EventCountSample;

import java.util.List;

/**
 * Ferestrele de count-uri ale unei echipe pe piata (cate un sample per meci, totalul meciului fiind
 * {@code countFor + countAgainst}) plus randurile PROPRII de statistici (pentru mediile afisate).
 * Listele pot avea lungimi diferite: un meci intra doar in pietele cu valori prezente.
 */
public record IstoricCounturi(
        List<EventCountSample> cornere,
        List<EventCountSample> faulturi,
        List<EventCountSample> cartonase,
        List<FixtureTeamStats> statisticiEchipa
) {

    public static IstoricCounturi gol() {
        return new IstoricCounturi(List.of(), List.of(), List.of(), List.of());
    }
}
