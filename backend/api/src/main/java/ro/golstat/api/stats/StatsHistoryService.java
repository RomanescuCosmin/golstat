package ro.golstat.api.stats;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import ro.golstat.api.entity.Fixture;
import ro.golstat.api.entity.FixtureTeamStats;
import ro.golstat.api.repository.FixtureRepository;
import ro.golstat.api.repository.FixtureTeamStatsRepository;
import ro.golstat.common.GolstatConstants;
import ro.golstat.stats.model.EventCountSample;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Construieste ferestrele de count-uri ({@link EventCountSample}) ale unei echipe din
 * {@code fixture_team_stats}: ultimele N meciuri terminale dinaintea unei date, fiecare sample cu
 * "cate am facut EU / cate a facut ADVERSARUL". Meciurile fara statistici (pe oricare parte) sunt
 * sarite; un camp lipsa sare doar piata respectiva.
 */
@Service
public class StatsHistoryService {

    private static final List<String> TERMINAL = List.of(
            GolstatConstants.FixtureStatus.FINISHED,
            GolstatConstants.FixtureStatus.FINISHED_AET,
            GolstatConstants.FixtureStatus.FINISHED_PEN
    );

    private final FixtureRepository fixtures;
    private final FixtureTeamStatsRepository teamStats;

    public StatsHistoryService(FixtureRepository fixtures, FixtureTeamStatsRepository teamStats) {
        this.fixtures = fixtures;
        this.teamStats = teamStats;
    }

    public IstoricCounturi istoric(long teamId, OffsetDateTime before, int limit) {
        List<Fixture> meciuri = fixtures.findRecentForTeam(teamId, TERMINAL, before, PageRequest.of(0, limit));
        if (meciuri.isEmpty()) {
            return IstoricCounturi.gol();
        }
        Map<Long, Map<Long, FixtureTeamStats>> perMeci = teamStats
                .findByFixtureIdIn(meciuri.stream().map(Fixture::getId).toList()).stream()
                .collect(Collectors.groupingBy(FixtureTeamStats::getFixtureId,
                        Collectors.toMap(FixtureTeamStats::getTeamId, Function.identity())));

        List<EventCountSample> cornere = new ArrayList<>();
        List<EventCountSample> faulturi = new ArrayList<>();
        List<EventCountSample> cartonase = new ArrayList<>();
        List<FixtureTeamStats> proprii = new ArrayList<>();
        for (Fixture f : meciuri) {
            boolean home = f.getHomeTeamId() != null && f.getHomeTeamId() == teamId;
            Long adversarId = home ? f.getAwayTeamId() : f.getHomeTeamId();
            Map<Long, FixtureTeamStats> randuri = perMeci.getOrDefault(f.getId(), Map.of());
            FixtureTeamStats ale = randuri.get(teamId);
            FixtureTeamStats aleAdversarului = adversarId != null ? randuri.get(adversarId) : null;
            if (ale == null || aleAdversarului == null) {
                continue;
            }
            proprii.add(ale);
            LocalDate data = f.getKickoff() != null ? f.getKickoff().toLocalDate() : null;
            adauga(cornere, data, home, ale.getCornerKicks(), aleAdversarului.getCornerKicks());
            adauga(faulturi, data, home, ale.getFouls(), aleAdversarului.getFouls());
            adauga(cartonase, data, home, totalCartonase(ale), totalCartonase(aleAdversarului));
        }
        return new IstoricCounturi(List.copyOf(cornere), List.copyOf(faulturi), List.copyOf(cartonase),
                List.copyOf(proprii));
    }

    /** Galbene + rosii; {@code null} doar cand ambele lipsesc (rosii lipsa langa galbene = 0). */
    public static Integer totalCartonase(FixtureTeamStats s) {
        if (s.getYellowCards() == null && s.getRedCards() == null) {
            return null;
        }
        return nz(s.getYellowCards()) + nz(s.getRedCards());
    }

    private static void adauga(List<EventCountSample> lista, LocalDate data, boolean home,
                               Integer aleNoastre, Integer aleAdversarului) {
        if (aleNoastre == null || aleAdversarului == null) {
            return;
        }
        lista.add(new EventCountSample(data, home, aleNoastre, aleAdversarului, null));
    }

    private static int nz(Integer value) {
        return value != null ? value : 0;
    }
}
