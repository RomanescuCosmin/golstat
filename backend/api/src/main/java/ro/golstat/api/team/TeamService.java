package ro.golstat.api.team;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.golstat.api.entity.Coach;
import ro.golstat.api.entity.Fixture;
import ro.golstat.api.entity.FixtureTeamStats;
import ro.golstat.api.entity.League;
import ro.golstat.api.entity.Player;
import ro.golstat.api.entity.PlayerSeasonStats;
import ro.golstat.api.entity.Standing;
import ro.golstat.api.entity.Team;
import ro.golstat.api.entity.TeamSeasonStats;
import ro.golstat.api.entity.Venue;
import ro.golstat.api.prediction.PredictieMeciDto.EchipaDto;
import ro.golstat.api.repository.CoachRepository;
import ro.golstat.api.repository.FixtureEventRepository;
import ro.golstat.api.repository.FixtureLineupRepository;
import ro.golstat.api.repository.FixtureRepository;
import ro.golstat.api.repository.FixtureTeamStatsRepository;
import ro.golstat.api.repository.LeagueRepository;
import ro.golstat.api.repository.PlayerRepository;
import ro.golstat.api.repository.PlayerSeasonStatsRepository;
import ro.golstat.api.repository.StandingRepository;
import ro.golstat.api.repository.TeamRepository;
import ro.golstat.api.repository.TeamSeasonStatsRepository;
import ro.golstat.api.repository.VenueRepository;
import ro.golstat.common.GolstatConstants.EventType;
import ro.golstat.common.GolstatConstants.FixtureStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Compune {@link PaginaEchipaDto}. 404 doar daca echipa lipseste; orice alt bloc (clasament, sezon,
 * top jucatori, bare) degradeaza independent la {@code null}/{@code []} — setul 2022–2024 poate avea
 * {@code standing}/{@code team_season_stats}/{@code player_season_stats} goale, deci NU dam 500 pe date lipsa.
 */
@Service
@Transactional(readOnly = true)
public class TeamService {

    private static final int FEREASTRA_FORMA = 5;
    private static final List<String> TERMINAL = FixtureStatus.TERMINAL;

    /** Intervalele de 15 min pentru distributia golurilor; ultimul acopera prelungirile (>90). */
    private static final List<String> INTERVALE =
            List.of("1-15", "16-30", "31-45", "46-60", "61-75", "76-90", "90+");

    private final TeamRepository teams;
    private final LeagueRepository leagues;
    private final VenueRepository venues;
    private final CoachRepository coaches;
    private final StandingRepository standings;
    private final TeamSeasonStatsRepository teamSeasonStats;
    private final PlayerSeasonStatsRepository playerSeasonStats;
    private final PlayerRepository players;
    private final FixtureRepository fixtures;
    private final FixtureTeamStatsRepository teamStats;
    private final FixtureEventRepository events;
    private final FixtureLineupRepository lineups;

    public TeamService(TeamRepository teams, LeagueRepository leagues, VenueRepository venues,
                       CoachRepository coaches, StandingRepository standings,
                       TeamSeasonStatsRepository teamSeasonStats, PlayerSeasonStatsRepository playerSeasonStats,
                       PlayerRepository players, FixtureRepository fixtures,
                       FixtureTeamStatsRepository teamStats, FixtureEventRepository events,
                       FixtureLineupRepository lineups) {
        this.teams = teams;
        this.leagues = leagues;
        this.venues = venues;
        this.coaches = coaches;
        this.standings = standings;
        this.teamSeasonStats = teamSeasonStats;
        this.playerSeasonStats = playerSeasonStats;
        this.players = players;
        this.fixtures = fixtures;
        this.teamStats = teamStats;
        this.events = events;
        this.lineups = lineups;
    }

    public PaginaEchipaDto pagina(long teamId, Long leagueId, Integer sezon) {
        Team echipa = teams.findById(teamId)
                .orElseThrow(() -> new EchipaNotFoundException(teamId));

        Context ctx = rezolvaContext(teamId, leagueId, sezon);

        TeamSeasonStats sezonStats = (ctx.leagueId != null && ctx.sezon != null)
                ? teamSeasonStats.findByTeamIdAndLeagueIdAndSeasonYear(teamId, ctx.leagueId, ctx.sezon).orElse(null)
                : null;
        Standing standing = (ctx.leagueId != null && ctx.sezon != null)
                ? standings.findByLeagueIdAndSeasonYearAndTeamId(ctx.leagueId, ctx.sezon, teamId).orElse(null)
                : null;

        return new PaginaEchipaDto(
                antet(echipa, ctx),
                sumar(standing, sezonStats),
                forma(teamId),
                urmatorulMeci(teamId),
                clasament(teamId, ctx),
                statBare(teamId, sezonStats, ctx),
                goluriPeInterval(teamId, ctx),
                topJucatori(teamId, ctx));
    }

    /** Rezolva liga/sezonul: parametrii au prioritate; altfel cel mai recent sezon din statistici, altfel din ultimul meci. */
    private Context rezolvaContext(long teamId, Long leagueId, Integer sezon) {
        Long lg = leagueId;
        Integer sz = sezon;
        if (lg == null || sz == null) {
            final Long fLg = lg;
            final Integer fSz = sz;
            Optional<TeamSeasonStats> best = teamSeasonStats.findByTeamId(teamId).stream()
                    .filter(s -> fLg == null || fLg.equals(s.getLeagueId()))
                    .filter(s -> fSz == null || fSz.equals(s.getSeasonYear()))
                    .filter(s -> s.getSeasonYear() != null)
                    .max(Comparator.comparingInt(TeamSeasonStats::getSeasonYear));
            if (best.isPresent()) {
                lg = lg != null ? lg : best.get().getLeagueId();
                sz = sz != null ? sz : best.get().getSeasonYear();
            } else {
                List<Fixture> recent = fixtures.findRecentForTeam(
                        teamId, TERMINAL, OffsetDateTime.now(), PageRequest.of(0, 1));
                if (!recent.isEmpty()) {
                    Fixture f = recent.get(0);
                    lg = lg != null ? lg : f.getLeagueId();
                    sz = sz != null ? sz : f.getSeasonYear();
                }
            }
        }
        return new Context(lg, sz);
    }

    private PaginaEchipaDto.Antet antet(Team echipa, Context ctx) {
        String liga = ctx.leagueId != null
                ? leagues.findById(ctx.leagueId).map(League::getName).orElse(null) : null;
        Venue venue = echipa.getVenueId() != null ? venues.findById(echipa.getVenueId()).orElse(null) : null;
        String antrenor = lineups.recentCoachIds(echipa.getId(), PageRequest.of(0, 1)).stream()
                .findFirst()
                .flatMap(coaches::findById)
                .map(Coach::getName)
                .orElse(null);
        return new PaginaEchipaDto.Antet(
                echipa.getId(), echipa.getName(), echipa.getLogo(), echipa.getCountryName(),
                liga, ctx.leagueId, ctx.sezon, antrenor,
                venue != null ? venue.getName() : null,
                venue != null ? venue.getCapacity() : null);
    }

    /** Pozitie/puncte din clasament, restul din statisticile de sezon; {@code null} daca lipsesc ambele surse. */
    private PaginaEchipaDto.Sumar sumar(Standing standing, TeamSeasonStats s) {
        if (standing == null && s == null) {
            return null;
        }
        Integer marcate = s != null ? s.getGoalsForTotal() : null;
        Integer primite = s != null ? s.getGoalsAgainstTotal() : null;
        return new PaginaEchipaDto.Sumar(
                standing != null ? standing.getRank() : null,
                standing != null ? standing.getPoints() : null,
                s != null ? s.getPlayedTotal() : null,
                s != null ? s.getWinsTotal() : null,
                s != null ? s.getDrawsTotal() : null,
                s != null ? s.getLosesTotal() : null,
                marcate, primite,
                (marcate != null && primite != null) ? marcate - primite : null);
    }

    private List<PaginaEchipaDto.MeciForma> forma(long teamId) {
        List<Fixture> recent = fixtures.findRecentForTeam(
                teamId, TERMINAL, OffsetDateTime.now(), PageRequest.of(0, FEREASTRA_FORMA));
        Map<Long, Team> adversari = adversari(recent, teamId);
        return recent.stream().map(f -> {
            boolean acasa = Objects.equals(f.getHomeTeamId(), teamId);
            Integer marcate = acasa ? f.getGoalsHome() : f.getGoalsAway();
            Integer primite = acasa ? f.getGoalsAway() : f.getGoalsHome();
            Long advId = acasa ? f.getAwayTeamId() : f.getHomeTeamId();
            return new PaginaEchipaDto.MeciForma(
                    f.getId(), f.getKickoff(), acasa, echipa(advId, adversari),
                    marcate, primite, rezultat(marcate, primite));
        }).toList();
    }

    private PaginaEchipaDto.MeciScurt urmatorulMeci(long teamId) {
        List<Fixture> urm = fixtures.findNextForTeam(
                teamId, FixtureStatus.NOT_STARTED, OffsetDateTime.now(), PageRequest.of(0, 1));
        if (urm.isEmpty()) {
            return null;
        }
        Fixture f = urm.get(0);
        boolean acasa = Objects.equals(f.getHomeTeamId(), teamId);
        Long advId = acasa ? f.getAwayTeamId() : f.getHomeTeamId();
        Map<Long, Team> adversari = adversari(List.of(f), teamId);
        return new PaginaEchipaDto.MeciScurt(f.getId(), f.getKickoff(), echipa(advId, adversari), acasa);
    }

    private List<PaginaEchipaDto.RandClasament> clasament(long teamId, Context ctx) {
        if (ctx.leagueId == null || ctx.sezon == null) {
            return List.of();
        }
        List<Standing> randuri = standings.findByLeagueIdAndSeasonYearOrderByRankAsc(ctx.leagueId, ctx.sezon);
        Map<Long, Team> echipe = teams.findAllById(
                        randuri.stream().map(Standing::getTeamId).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(Team::getId, Function.identity()));
        return randuri.stream().map(r -> {
            Team t = echipe.get(r.getTeamId());
            return new PaginaEchipaDto.RandClasament(
                    r.getRank(), r.getTeamId() != null ? r.getTeamId() : 0,
                    t != null ? t.getName() : null, t != null ? t.getLogo() : null,
                    r.getPlayedAll(), r.getPoints(), r.getGoalsDiff(),
                    Objects.equals(r.getTeamId(), teamId));
        }).toList();
    }

    /** Goluri/meci + clean sheets + cartonase din statisticile de sezon; suturi/posesie/pase mediate din meciuri (nullable). */
    private PaginaEchipaDto.StatBare statBare(long teamId, TeamSeasonStats s, Context ctx) {
        List<FixtureTeamStats> peMeci = (ctx.leagueId != null && ctx.sezon != null)
                ? teamStats.findForTeamSeason(teamId, ctx.leagueId, ctx.sezon, TERMINAL)
                : List.of();
        Double suturi = medie(peMeci, x -> caDouble(x.getShotsTotal()));
        Double posesie = medie(peMeci, x -> x.getBallPossession() != null ? x.getBallPossession().doubleValue() : null);
        Double precizie = medie(peMeci, x -> x.getPassesPercentage() != null ? x.getPassesPercentage().doubleValue() : null);
        if (s == null && suturi == null && posesie == null && precizie == null) {
            return null;
        }
        return new PaginaEchipaDto.StatBare(
                s != null ? caDouble(s.getGoalsForAvgTotal()) : null,
                s != null ? caDouble(s.getGoalsAgainstAvgTotal()) : null,
                s != null ? s.getCleanSheetTotal() : null,
                s != null ? s.getYellowCardsTotal() : null,
                s != null ? s.getRedCardsTotal() : null,
                suturi, posesie, precizie);
    }

    /** Distributia golurilor pe intervale de 15 min; mereu 7 intervale (0 permis), minute null ignorate. */
    private List<PaginaEchipaDto.Bucket> goluriPeInterval(long teamId, Context ctx) {
        int[] cos = new int[INTERVALE.size()];
        if (ctx.leagueId != null && ctx.sezon != null) {
            for (Integer minut : events.goalMinutes(teamId, ctx.leagueId, ctx.sezon, EventType.GOAL, TERMINAL)) {
                if (minut == null) {
                    continue;
                }
                cos[bucket(minut)]++;
            }
        }
        List<PaginaEchipaDto.Bucket> rezultat = new ArrayList<>(INTERVALE.size());
        for (int i = 0; i < INTERVALE.size(); i++) {
            rezultat.add(new PaginaEchipaDto.Bucket(INTERVALE.get(i), cos[i]));
        }
        return rezultat;
    }

    private static int bucket(int minut) {
        if (minut <= 15) return 0;
        if (minut <= 30) return 1;
        if (minut <= 45) return 2;
        if (minut <= 60) return 3;
        if (minut <= 75) return 4;
        if (minut <= 90) return 5;
        return 6;
    }

    private PaginaEchipaDto.TopJucatori topJucatori(long teamId, Context ctx) {
        List<PlayerSeasonStats> stats = (ctx.leagueId != null && ctx.sezon != null)
                ? playerSeasonStats.findByTeamIdAndLeagueIdAndSeasonYear(teamId, ctx.leagueId, ctx.sezon)
                : List.of();
        if (stats.isEmpty()) {
            return new PaginaEchipaDto.TopJucatori(null, null, null, null);
        }
        PlayerSeasonStats golgheter = top(stats, s -> nz(s.getGoalsTotal()));
        PlayerSeasonStats pasator = top(stats, s -> nz(s.getGoalsAssists()));
        PlayerSeasonStats minute = top(stats, s -> nz(s.getMinutes()));
        PlayerSeasonStats cartonase = top(stats, s -> nz(s.getCardsYellow()) + nz(s.getCardsRed()));

        Map<Long, Player> jucatori = players.findAllById(
                        Stream.of(golgheter, pasator, minute, cartonase)
                                .filter(Objects::nonNull).map(PlayerSeasonStats::getPlayerId)
                                .filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(Player::getId, Function.identity()));

        return new PaginaEchipaDto.TopJucatori(
                jucator(golgheter, s -> nz(s.getGoalsTotal()), jucatori),
                jucator(pasator, s -> nz(s.getGoalsAssists()), jucatori),
                jucator(minute, s -> nz(s.getMinutes()), jucatori),
                jucator(cartonase, s -> nz(s.getCardsYellow()) + nz(s.getCardsRed()), jucatori));
    }

    private static PlayerSeasonStats top(List<PlayerSeasonStats> stats, ToIntFunction<PlayerSeasonStats> metric) {
        return stats.stream().max(Comparator.comparingInt(metric)).orElse(null);
    }

    private static PaginaEchipaDto.Jucator jucator(PlayerSeasonStats s, ToIntFunction<PlayerSeasonStats> metric,
                                                   Map<Long, Player> jucatori) {
        if (s == null) {
            return null;
        }
        Player p = jucatori.get(s.getPlayerId());
        return new PaginaEchipaDto.Jucator(
                s.getPlayerId(),
                p != null ? p.getName() : null,
                p != null ? p.getPhoto() : null,
                metric.applyAsInt(s));
    }

    private Map<Long, Team> adversari(List<Fixture> meciuri, long teamId) {
        List<Long> ids = meciuri.stream()
                .flatMap(f -> Stream.of(f.getHomeTeamId(), f.getAwayTeamId()))
                .filter(Objects::nonNull)
                .filter(id -> !Objects.equals(id, teamId))
                .distinct()
                .toList();
        return teams.findAllById(ids).stream()
                .collect(Collectors.toMap(Team::getId, Function.identity()));
    }

    private static EchipaDto echipa(Long id, Map<Long, Team> echipe) {
        Team t = echipe.get(id);
        return new EchipaDto(id != null ? id : 0, t != null ? t.getName() : null, t != null ? t.getLogo() : null);
    }

    private static String rezultat(Integer marcate, Integer primite) {
        if (marcate == null || primite == null) {
            return null;
        }
        return marcate > primite ? "V" : marcate.equals(primite) ? "E" : "I";
    }

    /** Media campului peste randurile cu valoare; {@code null} = fara date (nu 0). */
    private static Double medie(List<FixtureTeamStats> randuri, Function<FixtureTeamStats, Double> valoare) {
        List<Double> valori = randuri.stream().map(valoare).filter(Objects::nonNull).toList();
        if (valori.isEmpty()) {
            return null;
        }
        return rotunjit(valori.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
    }

    private static Double caDouble(Integer v) {
        return v != null ? v.doubleValue() : null;
    }

    private static Double caDouble(BigDecimal v) {
        return v != null ? rotunjit(v.doubleValue()) : null;
    }

    private static int nz(Integer v) {
        return v != null ? v : 0;
    }

    private static double rotunjit(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private record Context(Long leagueId, Integer sezon) {
    }
}
