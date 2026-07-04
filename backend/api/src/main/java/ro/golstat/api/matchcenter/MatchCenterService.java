package ro.golstat.api.matchcenter;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.golstat.api.entity.Coach;
import ro.golstat.api.entity.Fixture;
import ro.golstat.api.entity.FixtureEvent;
import ro.golstat.api.entity.FixtureLineup;
import ro.golstat.api.entity.FixtureLineupPlayer;
import ro.golstat.api.entity.FixtureTeamStats;
import ro.golstat.api.entity.League;
import ro.golstat.api.entity.Player;
import ro.golstat.api.entity.Team;
import ro.golstat.api.entity.Venue;
import ro.golstat.api.prediction.PredictieMeciDto.EchipaDto;
import ro.golstat.api.repository.CoachRepository;
import ro.golstat.api.repository.FixtureEventRepository;
import ro.golstat.api.repository.FixtureLineupPlayerRepository;
import ro.golstat.api.repository.FixtureLineupRepository;
import ro.golstat.api.repository.FixtureRepository;
import ro.golstat.api.repository.FixtureTeamStatsRepository;
import ro.golstat.api.repository.LeagueRepository;
import ro.golstat.api.repository.PlayerRepository;
import ro.golstat.api.repository.TeamRepository;
import ro.golstat.api.repository.VenueRepository;
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
    private final FixtureLineupRepository lineups;
    private final FixtureLineupPlayerRepository lineupPlayers;
    private final CoachRepository coaches;
    private final VenueRepository venues;
    private final LeagueRepository leagues;

    public MatchCenterService(FixtureRepository fixtures, TeamRepository teams,
                              FixtureTeamStatsRepository teamStats, FixtureEventRepository events,
                              PlayerRepository players, FixtureLineupRepository lineups,
                              FixtureLineupPlayerRepository lineupPlayers, CoachRepository coaches,
                              VenueRepository venues, LeagueRepository leagues) {
        this.fixtures = fixtures;
        this.teams = teams;
        this.teamStats = teamStats;
        this.events = events;
        this.players = players;
        this.lineups = lineups;
        this.lineupPlayers = lineupPlayers;
        this.coaches = coaches;
        this.venues = venues;
        this.leagues = leagues;
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

        League liga = meci.getLeagueId() != null ? leagues.findById(meci.getLeagueId()).orElse(null) : null;
        String status = meci.getStatusShort();
        return new MeciCentralDto(
                meci.getId(), meci.getLeagueId(),
                liga != null ? liga.getName() : null,
                liga != null ? liga.getLogo() : null,
                echipa(meci.getHomeTeamId(), echipe), echipa(meci.getAwayTeamId(), echipe),
                meci.getGoalsHome(), meci.getGoalsAway(),
                status, meci.getStatusLong(), meci.getStatusElapsed(),
                status != null && FixtureStatus.IN_PLAY.contains(status),
                status != null && FixtureStatus.TERMINAL.contains(status),
                meci.getKickoff(),
                meci.getReferee(),
                stadion(meci),
                statistici(meci),
                formatii(meci),
                cronologie(meci));
    }

    private String stadion(Fixture meci) {
        if (meci.getVenueId() == null) {
            return null;
        }
        return venues.findById(meci.getVenueId()).map(Venue::getName).orElse(null);
    }

    /** {@code null} cat timp nu avem lineup pentru AMBELE echipe (sursa le anunta impreuna). */
    private MeciCentralDto.Formatii formatii(Fixture meci) {
        Map<Long, FixtureLineup> perEchipa = lineups.findByFixtureId(meci.getId()).stream()
                .filter(l -> l.getTeamId() != null)
                .collect(Collectors.toMap(FixtureLineup::getTeamId, Function.identity()));
        FixtureLineup gazde = perEchipa.get(meci.getHomeTeamId());
        FixtureLineup oaspeti = perEchipa.get(meci.getAwayTeamId());
        if (gazde == null || oaspeti == null) {
            return null;
        }
        Map<Long, List<FixtureLineupPlayer>> jucatori = lineupPlayers.findByFixtureId(meci.getId()).stream()
                .filter(p -> p.getTeamId() != null)
                .collect(Collectors.groupingBy(FixtureLineupPlayer::getTeamId));
        Map<Long, String> antrenori = coaches.findAllById(
                        Stream.of(gazde.getCoachId(), oaspeti.getCoachId())
                                .filter(Objects::nonNull).distinct().toList())
                .stream()
                .filter(c -> c.getName() != null)
                .collect(Collectors.toMap(Coach::getId, Coach::getName));
        return new MeciCentralDto.Formatii(
                echipaFormatie(gazde, jucatori.getOrDefault(meci.getHomeTeamId(), List.of()), antrenori),
                echipaFormatie(oaspeti, jucatori.getOrDefault(meci.getAwayTeamId(), List.of()), antrenori));
    }

    private static MeciCentralDto.EchipaFormatie echipaFormatie(FixtureLineup lineup,
                                                                List<FixtureLineupPlayer> jucatori,
                                                                Map<Long, String> antrenori) {
        return new MeciCentralDto.EchipaFormatie(
                lineup.getFormation(),
                lineup.getCoachId() != null ? antrenori.get(lineup.getCoachId()) : null,
                jucatoriFormatie(jucatori, false),
                jucatoriFormatie(jucatori, true));
    }

    private static List<MeciCentralDto.JucatorDto> jucatoriFormatie(List<FixtureLineupPlayer> jucatori,
                                                                    boolean rezerve) {
        return jucatori.stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsSubstitute()) == rezerve)
                .map(p -> new MeciCentralDto.JucatorDto(
                        p.getPlayerId(), p.getPlayerName(), p.getNumber(), p.getPosition(), p.getGrid()))
                .toList();
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
