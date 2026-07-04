package ro.golstat.api.matchcenter;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.golstat.api.entity.Fixture;
import ro.golstat.api.entity.FixtureEvent;
import ro.golstat.api.entity.FixtureTeamStats;
import ro.golstat.api.entity.Player;
import ro.golstat.api.entity.Team;
import ro.golstat.api.prediction.PredictieMeciDto.EchipaDto;
import ro.golstat.api.repository.FixtureEventRepository;
import ro.golstat.api.repository.FixtureRepository;
import ro.golstat.api.repository.FixtureTeamStatsRepository;
import ro.golstat.api.repository.PlayerRepository;
import ro.golstat.api.repository.TeamRepository;
import ro.golstat.common.GolstatConstants.FixtureStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Compune {@link MeciCentralDto} pentru Match Center: fixture + echipe (nume/logo) + statistici
 * pe echipa (mapate pe teamId) + cronologia evenimentelor cu nume de jucatori. Fiecare bloc de
 * statistici degradeaza null-safe; fara randuri de statistici → {@code statistici == null}.
 */
@Service
@Transactional(readOnly = true)
public class MatchCenterService {

    private final FixtureRepository fixtures;
    private final TeamRepository teams;
    private final FixtureTeamStatsRepository teamStats;
    private final FixtureEventRepository events;
    private final PlayerRepository players;

    public MatchCenterService(FixtureRepository fixtures, TeamRepository teams,
                              FixtureTeamStatsRepository teamStats, FixtureEventRepository events,
                              PlayerRepository players) {
        this.fixtures = fixtures;
        this.teams = teams;
        this.teamStats = teamStats;
        this.events = events;
        this.players = players;
    }

    /** Detaliul meciului dupa id; {@link MeciNotFoundException} daca nu exista. */
    public MeciCentralDto matchCenter(long fixtureId) {
        Fixture meci = fixtures.findById(fixtureId)
                .orElseThrow(() -> new MeciNotFoundException(fixtureId));

        Map<Long, Team> echipe = teams.findAllById(
                        Stream.of(meci.getHomeTeamId(), meci.getAwayTeamId())
                                .filter(Objects::nonNull).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Team::getId, Function.identity()));

        String status = meci.getStatusShort();
        return new MeciCentralDto(
                meci.getId(), meci.getLeagueId(),
                echipa(meci.getHomeTeamId(), echipe), echipa(meci.getAwayTeamId(), echipe),
                meci.getGoalsHome(), meci.getGoalsAway(),
                status, meci.getStatusLong(), meci.getStatusElapsed(),
                status != null && FixtureStatus.IN_PLAY.contains(status),
                status != null && FixtureStatus.TERMINAL.contains(status),
                meci.getKickoff(),
                statistici(meci),
                cronologie(meci));
    }

    private MeciCentralDto.Statistici statistici(Fixture meci) {
        Map<Long, FixtureTeamStats> perEchipa = teamStats.findByFixtureIdIn(List.of(meci.getId())).stream()
                .filter(s -> s.getTeamId() != null)
                .collect(Collectors.toMap(FixtureTeamStats::getTeamId, Function.identity()));
        if (perEchipa.isEmpty()) {
            return null;
        }
        return new MeciCentralDto.Statistici(
                echipaStats(perEchipa.get(meci.getHomeTeamId())),
                echipaStats(perEchipa.get(meci.getAwayTeamId())));
    }

    private static MeciCentralDto.Echipa echipaStats(FixtureTeamStats s) {
        if (s == null) {
            return null;
        }
        return new MeciCentralDto.Echipa(
                rotunjit(s.getBallPossession()),
                s.getShotsOnGoal(),
                s.getShotsTotal(),
                s.getCornerKicks(),
                s.getFouls(),
                s.getYellowCards(),
                s.getRedCards(),
                s.getPassesTotal(),
                s.getPassesAccurate(),
                rotunjit(s.getPassesPercentage()),
                s.getExpectedGoals() != null ? s.getExpectedGoals().doubleValue() : null);
    }

    private List<EvenimentDto> cronologie(Fixture meci) {
        List<FixtureEvent> randuri = events.findTimeline(meci.getId());
        Map<Long, String> nume = players.findAllById(
                        randuri.stream()
                                .flatMap(e -> Stream.of(e.getPlayerId(), e.getAssistId()))
                                .filter(Objects::nonNull).distinct().toList())
                .stream()
                .filter(p -> p.getName() != null)
                .collect(Collectors.toMap(Player::getId, Player::getName));
        Long home = meci.getHomeTeamId();
        return randuri.stream()
                .map(e -> new EvenimentDto(
                        e.getId(), e.getTeamId(),
                        e.getTeamId() != null && e.getTeamId().equals(home),
                        e.getTimeElapsed(), e.getTimeExtra(),
                        e.getType(), e.getDetail(),
                        e.getPlayerId() != null ? nume.get(e.getPlayerId()) : null,
                        e.getAssistId() != null ? nume.get(e.getAssistId()) : null))
                .toList();
    }

    private static EchipaDto echipa(Long id, Map<Long, Team> echipe) {
        Team t = echipe.get(id);
        return new EchipaDto(id != null ? id : 0, t != null ? t.getName() : null, t != null ? t.getLogo() : null);
    }

    /** BigDecimal (posesie/precizie pase, ex. 55.0) → Integer rotunjit; null-safe. */
    private static Integer rotunjit(BigDecimal v) {
        return v != null ? v.setScale(0, RoundingMode.HALF_UP).intValueExact() : null;
    }
}
