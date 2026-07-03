package ro.golstat.api.stats;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import ro.golstat.api.entity.Fixture;
import ro.golstat.api.entity.Standing;
import ro.golstat.api.repository.FixtureRepository;
import ro.golstat.api.repository.StandingRepository;
import ro.golstat.common.GolstatConstants;
import ro.golstat.stats.model.MatchSample;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Construieste istoricul (lista de {@link MatchSample}) al unei echipe pentru motor: ultimele N
 * meciuri terminale dinaintea unei date, cu rangul adversarului din clasament pe meci.
 * Ferestrele/filtrele (acasa/deplasare, clasament) le aplica motorul din {@code stats-engine}.
 */
@Service
public class MatchHistoryService {

    private static final List<String> TERMINAL = List.of(
            GolstatConstants.FixtureStatus.FINISHED,
            GolstatConstants.FixtureStatus.FINISHED_AET,
            GolstatConstants.FixtureStatus.FINISHED_PEN
    );

    private final FixtureRepository fixtures;
    private final StandingRepository standings;

    public MatchHistoryService(FixtureRepository fixtures, StandingRepository standings) {
        this.fixtures = fixtures;
        this.standings = standings;
    }

    public List<MatchSample> lastMatches(long teamId, OffsetDateTime before, int limit) {
        return fixtures.findRecentForTeam(teamId, TERMINAL, before, PageRequest.of(0, limit)).stream()
                .map(f -> MatchSampleMapper.toSample(f, teamId, opponentRank(f, teamId)))
                .toList();
    }

    private Integer opponentRank(Fixture fixture, long teamId) {
        boolean home = fixture.getHomeTeamId() != null && fixture.getHomeTeamId() == teamId;
        Long opponentId = home ? fixture.getAwayTeamId() : fixture.getHomeTeamId();
        if (opponentId == null || fixture.getLeagueId() == null || fixture.getSeasonYear() == null) {
            return null;
        }
        return standings.findById(new Standing.Pk(fixture.getLeagueId(), fixture.getSeasonYear(), opponentId))
                .map(Standing::getRank)
                .orElse(null);
    }
}
