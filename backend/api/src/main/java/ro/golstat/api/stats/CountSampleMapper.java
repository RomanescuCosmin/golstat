package ro.golstat.api.stats;

import ro.golstat.api.entity.Fixture;
import ro.golstat.api.entity.FixtureTeamHalfStats;
import ro.golstat.api.entity.FixtureTeamStats;
import ro.golstat.stats.model.EventCountSample;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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

    /** Ca mai jos, fara statistici pe reprize. */
    public static IstoricCounturi istoric(long teamId, List<Fixture> meciuri,
                                          Map<Long, Map<Long, FixtureTeamStats>> statsPerMeci) {
        return istoric(teamId, meciuri, statsPerMeci, Map.of());
    }

    /**
     * @param statsPerMeci {@code fixtureId → (teamId → statistici)}, deja incarcat de apelant
     * @param reprizePerMeci {@code fixtureId → ("teamId:repriza" → statistici)}, vezi {@link #reprizePerMeci}
     */
    public static IstoricCounturi istoric(long teamId, List<Fixture> meciuri,
                                          Map<Long, Map<Long, FixtureTeamStats>> statsPerMeci,
                                          Map<Long, Map<String, FixtureTeamHalfStats>> reprizePerMeci) {
        List<EventCountSample> cornere = new ArrayList<>();
        List<EventCountSample> faulturi = new ArrayList<>();
        List<EventCountSample> cartonase = new ArrayList<>();
        List<EventCountSample> suturi = new ArrayList<>();
        List<EventCountSample> suturiPePoarta = new ArrayList<>();
        List<FixtureTeamStats> proprii = new ArrayList<>();
        List<EventCountSample> cornereR1 = new ArrayList<>();
        List<EventCountSample> cornereR2 = new ArrayList<>();

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
            Map<String, FixtureTeamHalfStats> reprize = reprizePerMeci.getOrDefault(f.getId(), Map.of());
            adaugaRepriza(cornereR1, data, home, reprize.get(cheie(teamId, 1)), reprize.get(cheie(adversarId, 1)));
            adaugaRepriza(cornereR2, data, home, reprize.get(cheie(teamId, 2)), reprize.get(cheie(adversarId, 2)));
        }
        return new IstoricCounturi(List.copyOf(cornere), List.copyOf(faulturi), List.copyOf(cartonase),
                List.copyOf(suturi), List.copyOf(suturiPePoarta), List.copyOf(proprii),
                List.copyOf(cornereR1), List.copyOf(cornereR2));
    }

    /** Grupeaza randurile pe reprize pentru {@link #istoric}. */
    public static Map<Long, Map<String, FixtureTeamHalfStats>> reprizePerMeci(Collection<FixtureTeamHalfStats> randuri) {
        Map<Long, Map<String, FixtureTeamHalfStats>> out = new HashMap<>();
        for (FixtureTeamHalfStats r : randuri) {
            if (r.getFixtureId() != null && r.getTeamId() != null && r.getHalf() != null) {
                out.computeIfAbsent(r.getFixtureId(), k -> new HashMap<>()).put(cheie(r.getTeamId(), r.getHalf()), r);
            }
        }
        return out;
    }

    private static String cheie(Long teamId, int repriza) {
        return teamId + ":" + repriza;
    }

    private static void adaugaRepriza(List<EventCountSample> lista, LocalDate data, boolean home,
                                      FixtureTeamHalfStats ale, FixtureTeamHalfStats aleAdversarului) {
        if (ale != null && aleAdversarului != null) {
            adauga(lista, data, home, ale.getCornerKicks(), aleAdversarului.getCornerKicks());
        }
    }

    private static void adauga(List<EventCountSample> lista, LocalDate data, boolean home,
                               Integer aleNoastre, Integer aleAdversarului) {
        if (aleNoastre == null || aleAdversarului == null) {
            return;
        }
        lista.add(new EventCountSample(data, home, aleNoastre, aleAdversarului, null));
    }
}
