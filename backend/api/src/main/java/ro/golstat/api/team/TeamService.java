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
import ro.golstat.api.stats.CountAverage;
import ro.golstat.api.stats.GoalAverage;
import ro.golstat.common.GolstatConstants.EventType;
import ro.golstat.common.GolstatConstants.FixtureStatus;
import ro.golstat.common.GolstatConstants.Piata;

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
    private static final int FEREASTRA_REZULTATE = 10;
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
                rezultate(teamId, FEREASTRA_FORMA),
                rezultate(teamId, FEREASTRA_REZULTATE),
                statProcente(teamId, sezonStats, ctx),
                sezoane(teamId),
                urmatorulMeci(teamId),
                clasament(teamId, ctx),
                statBare(teamId, sezonStats, standing, ctx),
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
            // 1) cel mai recent sezon cu CLASAMENT si meciuri deja jucate — cel mai relevant default
            //    (un sezon nou poate avea deja un clasament initial cu 0 meciuri jucate → il sarim)
            Optional<Standing> dinClasament = standings.findByTeamId(teamId).stream()
                    .filter(s -> fLg == null || fLg.equals(s.getLeagueId()))
                    .filter(s -> fSz == null || fSz.equals(s.getSeasonYear()))
                    .filter(s -> s.getSeasonYear() != null)
                    .filter(s -> s.getPlayedAll() != null && s.getPlayedAll() > 0)
                    .max(Comparator.comparingInt(Standing::getSeasonYear));
            // 2) altfel cel mai recent sezon din statisticile de sezon
            Optional<TeamSeasonStats> dinStats = teamSeasonStats.findByTeamId(teamId).stream()
                    .filter(s -> fLg == null || fLg.equals(s.getLeagueId()))
                    .filter(s -> fSz == null || fSz.equals(s.getSeasonYear()))
                    .filter(s -> s.getSeasonYear() != null)
                    .max(Comparator.comparingInt(TeamSeasonStats::getSeasonYear));
            if (dinClasament.isPresent()) {
                lg = lg != null ? lg : dinClasament.get().getLeagueId();
                sz = sz != null ? sz : dinClasament.get().getSeasonYear();
            } else if (dinStats.isPresent()) {
                lg = lg != null ? lg : dinStats.get().getLeagueId();
                sz = sz != null ? sz : dinStats.get().getSeasonYear();
            } else {
                // 3) altfel din ultimul meci terminat
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
        League liga = ctx.leagueId != null ? leagues.findById(ctx.leagueId).orElse(null) : null;
        Venue venue = echipa.getVenueId() != null ? venues.findById(echipa.getVenueId()).orElse(null) : null;
        String antrenor = lineups.recentCoachIds(echipa.getId(), PageRequest.of(0, 1)).stream()
                .findFirst()
                .flatMap(coaches::findById)
                .map(Coach::getName)
                .orElse(null);
        return new PaginaEchipaDto.Antet(
                echipa.getId(), echipa.getName(), echipa.getLogo(), echipa.getCountryName(),
                liga != null ? liga.getName() : null, liga != null ? liga.getLogo() : null,
                ctx.leagueId, ctx.sezon, antrenor,
                venue != null ? venue.getName() : null,
                venue != null ? venue.getCapacity() : null);
    }

    /**
     * Sumarul sezonului: pozitie/puncte din clasament, restul din statisticile de sezon cu FALLBACK pe
     * clasament (multe sezoane au clasament dar nu {@code team_season_stats}) — asa apar mereu V/E/Î + goluri.
     */
    private PaginaEchipaDto.Sumar sumar(Standing standing, TeamSeasonStats s) {
        if (standing == null && s == null) {
            return null;
        }
        Integer jucate = coalesce(s != null ? s.getPlayedTotal() : null, standing != null ? standing.getPlayedAll() : null);
        Integer victorii = coalesce(s != null ? s.getWinsTotal() : null, standing != null ? standing.getWinAll() : null);
        Integer egaluri = coalesce(s != null ? s.getDrawsTotal() : null, standing != null ? standing.getDrawAll() : null);
        Integer infrangeri = coalesce(s != null ? s.getLosesTotal() : null, standing != null ? standing.getLoseAll() : null);
        Integer marcate = coalesce(s != null ? s.getGoalsForTotal() : null, standing != null ? standing.getGoalsForAll() : null);
        Integer primite = coalesce(s != null ? s.getGoalsAgainstTotal() : null, standing != null ? standing.getGoalsAgainstAll() : null);
        Integer golaveraj = (marcate != null && primite != null) ? marcate - primite
                : (standing != null ? standing.getGoalsDiff() : null);
        return new PaginaEchipaDto.Sumar(
                standing != null ? standing.getRank() : null,
                standing != null ? standing.getPoints() : null,
                jucate, victorii, egaluri, infrangeri, marcate, primite, golaveraj);
    }

    private static <T> T coalesce(T a, T b) {
        return a != null ? a : b;
    }

    private static Double perGame(Integer total, Integer jucate) {
        return (total != null && jucate != null && jucate > 0) ? (double) total / jucate : null;
    }

    private List<PaginaEchipaDto.MeciForma> rezultate(long teamId, int limita) {
        List<Fixture> recent = fixtures.findRecentForTeam(
                teamId, TERMINAL, OffsetDateTime.now(), PageRequest.of(0, limita));
        Map<Long, Team> adversari = adversari(recent, teamId);
        Map<Long, League> ligi = ligiPentru(recent);
        return recent.stream().map(f -> {
            boolean acasa = Objects.equals(f.getHomeTeamId(), teamId);
            Integer marcate = acasa ? f.getGoalsHome() : f.getGoalsAway();
            Integer primite = acasa ? f.getGoalsAway() : f.getGoalsHome();
            Long advId = acasa ? f.getAwayTeamId() : f.getHomeTeamId();
            League lg = f.getLeagueId() != null ? ligi.get(f.getLeagueId()) : null;
            return new PaginaEchipaDto.MeciForma(
                    f.getId(), f.getKickoff(), acasa, echipa(advId, adversari),
                    marcate, primite, rezultat(marcate, primite),
                    lg != null ? lg.getName() : null, lg != null ? lg.getLogo() : null, f.getRound());
        }).toList();
    }

    /** Ligile (nume/logo) pentru un set de meciuri, intr-un singur query. */
    private Map<Long, League> ligiPentru(List<Fixture> meciuri) {
        List<Long> ids = meciuri.stream().map(Fixture::getLeagueId)
                .filter(Objects::nonNull).distinct().toList();
        return leagues.findAllById(ids).stream()
                .collect(Collectors.toMap(League::getId, Function.identity()));
    }

    /**
     * Procentaje per categorie relativ la media ligii. Media echipei: goluri din statisticile de sezon
     * (fallback: medie din fixtures); cornere/faulturi/cartonase mediate din {@code fixture_team_stats}.
     * Media ligii: goluri prin {@code (avgGazde+avgOaspeti)/2}; restul din {@code avgCounts}. Categorie
     * omisa cand lipseste oricare parte.
     */
    private List<PaginaEchipaDto.StatProcent> statProcente(long teamId, TeamSeasonStats s, Context ctx) {
        if (ctx.leagueId == null || ctx.sezon == null) {
            return List.of();
        }
        List<FixtureTeamStats> peMeci = teamStats.findForTeamSeason(teamId, ctx.leagueId, ctx.sezon, TERMINAL);
        GoalAverage golLiga = fixtures.avgGoals(ctx.leagueId, ctx.sezon, TERMINAL);
        CountAverage countLiga = teamStats.avgCounts(ctx.leagueId, ctx.sezon, TERMINAL);

        Double golEchipa = s != null && s.getGoalsForAvgTotal() != null
                ? rotunjit(s.getGoalsForAvgTotal().doubleValue())
                : fixtures.avgGoalsForTeam(teamId, ctx.leagueId, ctx.sezon, TERMINAL);
        Double golLigaMedie = (golLiga != null && golLiga.getAvgGazde() != null && golLiga.getAvgOaspeti() != null)
                ? (golLiga.getAvgGazde() + golLiga.getAvgOaspeti()) / 2.0 : null;

        List<PaginaEchipaDto.StatProcent> rezultat = new ArrayList<>(4);
        adauga(rezultat, Piata.GOLURI, golEchipa, golLigaMedie);
        adauga(rezultat, Piata.CORNERE,
                medie(peMeci, x -> caDouble(x.getCornerKicks())),
                countLiga != null ? countLiga.getAvgCornere() : null);
        adauga(rezultat, Piata.FAULTURI,
                medie(peMeci, x -> caDouble(x.getFouls())),
                countLiga != null ? countLiga.getAvgFaulturi() : null);
        adauga(rezultat, Piata.CARTONASE,
                medie(peMeci, x -> caDouble(nz(x.getYellowCards()) + nz(x.getRedCards()))),
                countLiga != null ? countLiga.getAvgCartonase() : null);
        return rezultat;
    }

    /** Adauga o categorie doar cand ambele medii exista si media ligii e > 0. */
    private static void adauga(List<PaginaEchipaDto.StatProcent> acc, String categorie,
                               Double medieEchipa, Double medieLiga) {
        if (medieEchipa == null || medieLiga == null || medieLiga <= 0) {
            return;
        }
        int procent = (int) Math.round(100.0 * medieEchipa / (2.0 * medieLiga));
        procent = Math.max(0, Math.min(100, procent));
        acc.add(new PaginaEchipaDto.StatProcent(
                categorie, rotunjit(medieEchipa), rotunjit(medieLiga), procent));
    }

    /** Sezoanele echipei (din statistici ∪ meciuri), descrescator. */
    private List<Integer> sezoane(long teamId) {
        return Stream.concat(
                        teamSeasonStats.findByTeamId(teamId).stream()
                                .map(TeamSeasonStats::getSeasonYear),
                        fixtures.distinctSeasons(teamId).stream())
                .filter(Objects::nonNull)
                .distinct()
                .sorted(Comparator.reverseOrder())
                .toList();
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
        League lg = f.getLeagueId() != null ? leagues.findById(f.getLeagueId()).orElse(null) : null;
        return new PaginaEchipaDto.MeciScurt(f.getId(), f.getKickoff(), echipa(advId, adversari), acasa,
                lg != null ? lg.getName() : null, lg != null ? lg.getLogo() : null, f.getRound());
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
                    r.getPlayedAll(), r.getWinAll(), r.getDrawAll(), r.getLoseAll(),
                    r.getGoalsDiff(), r.getPoints(),
                    Objects.equals(r.getTeamId(), teamId));
        }).toList();
    }

    /**
     * Goluri/meci + clean sheets + cartonase din statisticile de sezon (cu FALLBACK pe clasament pentru
     * golurile/meci); suturi/posesie/pase mediate din meciuri (nullable).
     */
    private PaginaEchipaDto.StatBare statBare(long teamId, TeamSeasonStats s, Standing standing, Context ctx) {
        List<FixtureTeamStats> peMeci = (ctx.leagueId != null && ctx.sezon != null)
                ? teamStats.findForTeamSeason(teamId, ctx.leagueId, ctx.sezon, TERMINAL)
                : List.of();
        Double suturi = medie(peMeci, x -> caDouble(x.getShotsTotal()));
        Double posesie = medie(peMeci, x -> x.getBallPossession() != null ? x.getBallPossession().doubleValue() : null);
        Double precizie = medie(peMeci, x -> x.getPassesPercentage() != null ? x.getPassesPercentage().doubleValue() : null);
        if (s == null && standing == null && suturi == null && posesie == null && precizie == null) {
            return null;
        }
        Double marcatePeMeci = s != null ? caDouble(s.getGoalsForAvgTotal())
                : perGame(standing != null ? standing.getGoalsForAll() : null,
                          standing != null ? standing.getPlayedAll() : null);
        Double primitePeMeci = s != null ? caDouble(s.getGoalsAgainstAvgTotal())
                : perGame(standing != null ? standing.getGoalsAgainstAll() : null,
                          standing != null ? standing.getPlayedAll() : null);
        return new PaginaEchipaDto.StatBare(
                marcatePeMeci, primitePeMeci,
                s != null ? s.getCleanSheetTotal() : null,
                s != null ? s.getYellowCardsTotal() : null,
                s != null ? s.getRedCardsTotal() : null,
                suturi, posesie, precizie);
    }

    /** Distributia golurilor marcate vs primite pe intervale de 15 min; mereu 7 intervale (0 permis). */
    private List<PaginaEchipaDto.Bucket> goluriPeInterval(long teamId, Context ctx) {
        int[] marcate = new int[INTERVALE.size()];
        int[] primite = new int[INTERVALE.size()];
        if (ctx.leagueId != null && ctx.sezon != null) {
            for (Integer minut : events.goalMinutes(teamId, ctx.leagueId, ctx.sezon, EventType.GOAL, TERMINAL)) {
                if (minut != null) {
                    marcate[bucket(minut)]++;
                }
            }
            for (Integer minut : events.concededMinutes(teamId, ctx.leagueId, ctx.sezon, EventType.GOAL, TERMINAL)) {
                if (minut != null) {
                    primite[bucket(minut)]++;
                }
            }
        }
        List<PaginaEchipaDto.Bucket> rezultat = new ArrayList<>(INTERVALE.size());
        for (int i = 0; i < INTERVALE.size(); i++) {
            rezultat.add(new PaginaEchipaDto.Bucket(INTERVALE.get(i), marcate[i], primite[i]));
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
            return new PaginaEchipaDto.TopJucatori(null, null, null, null, null);
        }
        PlayerSeasonStats golgheter = top(stats, s -> nz(s.getGoalsTotal()));
        PlayerSeasonStats pasator = top(stats, s -> nz(s.getGoalsAssists()));
        PlayerSeasonStats minute = top(stats, s -> nz(s.getMinutes()));
        PlayerSeasonStats galbene = top(stats, s -> nz(s.getCardsYellow()));
        PlayerSeasonStats rosii = top(stats, s -> nz(s.getCardsRed()));

        Map<Long, Player> jucatori = players.findAllById(
                        Stream.of(golgheter, pasator, minute, galbene, rosii)
                                .filter(Objects::nonNull).map(PlayerSeasonStats::getPlayerId)
                                .filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(Player::getId, Function.identity()));

        return new PaginaEchipaDto.TopJucatori(
                jucator(golgheter, s -> nz(s.getGoalsTotal()), jucatori),
                jucator(pasator, s -> nz(s.getGoalsAssists()), jucatori),
                jucator(minute, s -> nz(s.getMinutes()), jucatori),
                jucator(galbene, s -> nz(s.getCardsYellow()), jucatori),
                jucator(rosii, s -> nz(s.getCardsRed()), jucatori));
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
