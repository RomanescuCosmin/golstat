package ro.golstat.api.stats;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import ro.golstat.api.entity.Fixture;
import ro.golstat.api.entity.FixtureTeamStats;
import ro.golstat.api.repository.FixtureRepository;
import ro.golstat.api.repository.FixtureTeamHalfStatsRepository;
import ro.golstat.api.repository.FixtureTeamStatsRepository;
import ro.golstat.common.GolstatConstants;
import ro.golstat.stats.model.EventCountSample;

import java.time.OffsetDateTime;
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
    private final FixtureTeamHalfStatsRepository halfStats;

    public StatsHistoryService(FixtureRepository fixtures, FixtureTeamStatsRepository teamStats,
                               FixtureTeamHalfStatsRepository halfStats) {
        this.fixtures = fixtures;
        this.teamStats = teamStats;
        this.halfStats = halfStats;
    }

    public IstoricCounturi istoric(long teamId, OffsetDateTime before, int limit) {
        return istoric(teamId, before, limit, null);
    }

    /** {@code leagueId} null = toate competitiile; altfel doar meciurile din competitia data. */
    public IstoricCounturi istoric(long teamId, OffsetDateTime before, int limit, Long leagueId) {
        List<Fixture> meciuri = leagueId == null
                ? fixtures.findRecentForTeam(teamId, TERMINAL, before, PageRequest.of(0, limit))
                : fixtures.findRecentForTeamInLeague(teamId, leagueId, TERMINAL, before, PageRequest.of(0, limit));
        if (meciuri.isEmpty()) {
            return IstoricCounturi.gol();
        }
        List<Long> ids = meciuri.stream().map(Fixture::getId).toList();
        Map<Long, Map<Long, FixtureTeamStats>> perMeci = teamStats
                .findByFixtureIdIn(ids).stream()
                .collect(Collectors.groupingBy(FixtureTeamStats::getFixtureId,
                        Collectors.toMap(FixtureTeamStats::getTeamId, Function.identity())));
        return CountSampleMapper.istoric(teamId, meciuri, perMeci,
                CountSampleMapper.reprizePerMeci(halfStats.findByFixtureIdIn(ids)));
    }

    /** Cartonasele din incidente cand le avem, altfel galbene + rosii; {@code null} cand lipsesc ambele. */
    public static Integer totalCartonase(FixtureTeamStats s) {
        if (s.getCartonase() != null) {
            return s.getCartonase();
        }
        if (s.getYellowCards() == null && s.getRedCards() == null) {
            return null;
        }
        return nz(s.getYellowCards()) + nz(s.getRedCards());
    }

    private static int nz(Integer value) {
        return value != null ? value : 0;
    }
}
