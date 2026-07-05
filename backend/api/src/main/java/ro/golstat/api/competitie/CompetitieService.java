package ro.golstat.api.competitie;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.golstat.api.entity.Fixture;
import ro.golstat.api.entity.League;
import ro.golstat.api.entity.Player;
import ro.golstat.api.entity.PlayerSeasonStats;
import ro.golstat.api.entity.Season;
import ro.golstat.api.entity.Standing;
import ro.golstat.api.entity.Team;
import ro.golstat.api.prediction.PredictieMeciDto.EchipaDto;
import ro.golstat.api.repository.FixtureRepository;
import ro.golstat.api.repository.LeagueRepository;
import ro.golstat.api.repository.PlayerRepository;
import ro.golstat.api.repository.PlayerSeasonStatsRepository;
import ro.golstat.api.repository.SeasonRepository;
import ro.golstat.api.repository.StandingRepository;
import ro.golstat.api.repository.TeamRepository;
import ro.golstat.api.team.PaginaEchipaDto.RandClasament;
import ro.golstat.common.GolstatConstants.FixtureStatus;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Compune {@link PaginaCompetitieDto}: clasament, golgheteri/pasatori, rezultate si program. */
@Service
@Transactional(readOnly = true)
public class CompetitieService {

    private static final int TOP_JUCATORI = 10;
    private static final int NR_REZULTATE = 15;
    private static final int NR_PROGRAM = 15;

    private final LeagueRepository leagues;
    private final SeasonRepository seasons;
    private final StandingRepository standings;
    private final TeamRepository teams;
    private final PlayerRepository players;
    private final PlayerSeasonStatsRepository playerSeasonStats;
    private final FixtureRepository fixtures;

    public CompetitieService(LeagueRepository leagues, SeasonRepository seasons, StandingRepository standings,
                             TeamRepository teams, PlayerRepository players,
                             PlayerSeasonStatsRepository playerSeasonStats, FixtureRepository fixtures) {
        this.leagues = leagues;
        this.seasons = seasons;
        this.standings = standings;
        this.teams = teams;
        this.players = players;
        this.playerSeasonStats = playerSeasonStats;
        this.fixtures = fixtures;
    }

    public PaginaCompetitieDto pagina(long leagueId, Integer sezonParam) {
        League liga = leagues.findById(leagueId).orElseThrow(() -> new CompetitieNotFoundException(leagueId));
        List<Integer> sezoane = seasons.findByLeagueIdOrderByYearDesc(leagueId).stream()
                .map(Season::getYear).filter(Objects::nonNull).distinct().toList();
        Integer sezon = rezolvaSezon(leagueId, sezonParam, sezoane);

        return new PaginaCompetitieDto(
                new PaginaCompetitieDto.Antet(leagueId, liga.getName(), liga.getCountryName(), liga.getLogo(), sezon, sezoane),
                clasament(leagueId, sezon),
                topJucatori(leagueId, sezon, s -> nz(s.getGoalsTotal()), true),
                topJucatori(leagueId, sezon, s -> nz(s.getGoalsAssists()), false),
                rezultate(leagueId, sezon),
                program(leagueId, sezon));
    }

    /** Sezonul cerut, altfel cel mai recent sezon cu meciuri jucate (clasament cu played>0), altfel cel mai recent. */
    private Integer rezolvaSezon(long leagueId, Integer sezonParam, List<Integer> sezoane) {
        if (sezonParam != null) {
            return sezonParam;
        }
        for (Integer an : sezoane) {
            boolean jucat = standings.findByLeagueIdAndSeasonYearOrderByRankAsc(leagueId, an).stream()
                    .anyMatch(s -> s.getPlayedAll() != null && s.getPlayedAll() > 0);
            if (jucat) {
                return an;
            }
        }
        return sezoane.isEmpty() ? null : sezoane.get(0);
    }

    private List<RandClasament> clasament(long leagueId, Integer sezon) {
        if (sezon == null) {
            return List.of();
        }
        List<Standing> randuri = standings.findByLeagueIdAndSeasonYearOrderByRankAsc(leagueId, sezon);
        Map<Long, Team> echipe = echipeById(randuri.stream().map(Standing::getTeamId));
        return randuri.stream().map(r -> {
            Team t = echipe.get(r.getTeamId());
            return new RandClasament(
                    r.getRank(), r.getTeamId() != null ? r.getTeamId() : 0,
                    t != null ? t.getName() : null, t != null ? t.getLogo() : null,
                    r.getPlayedAll(), r.getWinAll(), r.getDrawAll(), r.getLoseAll(),
                    r.getGoalsDiff(), r.getPoints(), false);
        }).toList();
    }

    private List<PaginaCompetitieDto.Jucator> topJucatori(long leagueId, Integer sezon,
                                                          ToIntFunction<PlayerSeasonStats> metric, boolean goluri) {
        if (sezon == null) {
            return List.of();
        }
        List<PlayerSeasonStats> stats = goluri
                ? playerSeasonStats.topGolgheteri(leagueId, sezon, PageRequest.of(0, TOP_JUCATORI))
                : playerSeasonStats.topPasatori(leagueId, sezon, PageRequest.of(0, TOP_JUCATORI));

        Map<Long, Player> jucatori = players.findAllById(
                        stats.stream().map(PlayerSeasonStats::getPlayerId).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(Player::getId, Function.identity()));
        Map<Long, Team> echipe = echipeById(stats.stream().map(PlayerSeasonStats::getTeamId));

        return stats.stream().map(s -> {
            Player p = jucatori.get(s.getPlayerId());
            Team t = echipe.get(s.getTeamId());
            return new PaginaCompetitieDto.Jucator(
                    s.getPlayerId(), p != null ? p.getName() : null, p != null ? p.getPhoto() : null,
                    echipa(s.getTeamId(), t), metric.applyAsInt(s));
        }).toList();
    }

    private List<PaginaCompetitieDto.Meci> rezultate(long leagueId, Integer sezon) {
        if (sezon == null) {
            return List.of();
        }
        List<Fixture> found = fixtures.findByLeagueIdAndSeasonYearAndStatusShortInOrderByKickoffDesc(
                leagueId, sezon, FixtureStatus.TERMINAL, PageRequest.of(0, NR_REZULTATE));
        return meciuri(found);
    }

    private List<PaginaCompetitieDto.Meci> program(long leagueId, Integer sezon) {
        if (sezon == null) {
            return List.of();
        }
        List<Fixture> found = fixtures.findByLeagueIdAndSeasonYearAndStatusShortOrderByKickoffAsc(
                leagueId, sezon, FixtureStatus.NOT_STARTED, PageRequest.of(0, NR_PROGRAM));
        return meciuri(found);
    }

    private List<PaginaCompetitieDto.Meci> meciuri(List<Fixture> found) {
        Map<Long, Team> echipe = echipeById(found.stream()
                .flatMap(f -> Stream.of(f.getHomeTeamId(), f.getAwayTeamId())));
        return found.stream().map(f -> new PaginaCompetitieDto.Meci(
                f.getId(), f.getKickoff(),
                echipa(f.getHomeTeamId(), echipe.get(f.getHomeTeamId())),
                echipa(f.getAwayTeamId(), echipe.get(f.getAwayTeamId())),
                f.getGoalsHome(), f.getGoalsAway(), f.getStatusShort(),
                FixtureStatus.IN_PLAY.contains(f.getStatusShort()),
                FixtureStatus.TERMINAL.contains(f.getStatusShort()))).toList();
    }

    private Map<Long, Team> echipeById(Stream<Long> ids) {
        return teams.findAllById(ids.filter(Objects::nonNull).distinct().toList()).stream()
                .collect(Collectors.toMap(Team::getId, Function.identity()));
    }

    private static EchipaDto echipa(Long id, Team t) {
        return new EchipaDto(id != null ? id : 0, t != null ? t.getName() : null, t != null ? t.getLogo() : null);
    }

    private static int nz(Integer v) {
        return v != null ? v : 0;
    }
}
