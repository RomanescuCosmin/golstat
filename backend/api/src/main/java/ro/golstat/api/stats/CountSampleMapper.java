package ro.golstat.api.stats;

import ro.golstat.api.entity.Fixture;
import ro.golstat.api.entity.FixtureTeamStats;
import ro.golstat.stats.model.EventCountSample;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Traduce meciurile deja incarcate ale unei echipe in {@link IstoricCounturi} — cate un
 * {@link EventCountSample} per meci si per piata, din perspectiva echipei ("cate am facut EU /
 * cate a facut ADVERSARUL"). Meciurile fara statistici pe oricare parte sunt sarite; un camp lipsa
 * sare doar piata lui.
 *
 * <p>Partajat de {@link StatsHistoryService} (o echipa, query propriu) si de
 * {@link FerestreBatchService} (multe echipe, incarcare in bloc) — aceeasi logica pentru amandoua,
 * altfel cele doua cai ar produce procente diferite pe acelasi meci.
 */
public final class CountSampleMapper {

    private CountSampleMapper() {
    }

    /**
     * @param statsPerMeci {@code fixtureId → (teamId → statistici)}, deja incarcat de apelant
     */
    public static IstoricCounturi istoric(long teamId, List<Fixture> meciuri,
                                          Map<Long, Map<Long, FixtureTeamStats>> statsPerMeci) {
        List<EventCountSample> cornere = new ArrayList<>();
        List<EventCountSample> faulturi = new ArrayList<>();
        List<EventCountSample> cartonase = new ArrayList<>();
        List<EventCountSample> suturi = new ArrayList<>();
        List<EventCountSample> suturiPePoarta = new ArrayList<>();
        List<FixtureTeamStats> proprii = new ArrayList<>();

        for (Fixture f : meciuri) {
            boolean home = f.getHomeTeamId() != null && f.getHomeTeamId() == teamId;
            Long adversarId = home ? f.getAwayTeamId() : f.getHomeTeamId();
            Map<Long, FixtureTeamStats> randuri = statsPerMeci.getOrDefault(f.getId(), Map.of());
            FixtureTeamStats ale = randuri.get(teamId);
            FixtureTeamStats aleAdversarului = adversarId != null ? randuri.get(adversarId) : null;
            if (ale == null || aleAdversarului == null) {
                continue;
            }
            proprii.add(ale);
            LocalDate data = f.getKickoff() != null ? f.getKickoff().toLocalDate() : null;
            adauga(cornere, data, home, ale.getCornerKicks(), aleAdversarului.getCornerKicks());
            adauga(faulturi, data, home, ale.getFouls(), aleAdversarului.getFouls());
            adauga(cartonase, data, home, StatsHistoryService.totalCartonase(ale),
                    StatsHistoryService.totalCartonase(aleAdversarului));
            adauga(suturi, data, home, ale.getShotsTotal(), aleAdversarului.getShotsTotal());
            adauga(suturiPePoarta, data, home, ale.getShotsOnGoal(), aleAdversarului.getShotsOnGoal());
        }
        return new IstoricCounturi(List.copyOf(cornere), List.copyOf(faulturi), List.copyOf(cartonase),
                List.copyOf(suturi), List.copyOf(suturiPePoarta), List.copyOf(proprii));
    }

    private static void adauga(List<EventCountSample> lista, LocalDate data, boolean home,
                               Integer aleNoastre, Integer aleAdversarului) {
        if (aleNoastre == null || aleAdversarului == null) {
            return;
        }
        lista.add(new EventCountSample(data, home, aleNoastre, aleAdversarului, null));
    }
}
