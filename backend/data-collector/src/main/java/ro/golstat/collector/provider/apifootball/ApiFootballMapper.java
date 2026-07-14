package ro.golstat.collector.provider.apifootball;

import ro.golstat.common.dto.CoachDto;
import ro.golstat.common.dto.FixtureDto;
import ro.golstat.common.dto.FixtureEventDto;
import ro.golstat.common.dto.FixtureLineupDto;
import ro.golstat.common.dto.FixtureLineupPlayerDto;
import ro.golstat.common.dto.FixtureLiveDto;
import ro.golstat.common.dto.FixturePlayerStatsDto;
import ro.golstat.common.dto.FixtureTeamStatsDto;
import ro.golstat.common.dto.InjuryDto;
import ro.golstat.common.dto.LeagueDto;
import ro.golstat.common.dto.PlayerDto;
import ro.golstat.common.dto.PlayerSeasonStatsDto;
import ro.golstat.common.dto.PlayerSezonDto;
import ro.golstat.common.dto.SeasonDto;
import ro.golstat.common.dto.StandingDto;
import ro.golstat.common.dto.TeamDto;
import ro.golstat.common.dto.TeamSeasonStatsDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Traduce raspunsul API-Football in DTO-urile de domeniu din {@code common}. Pur si null-tolerant:
 * orice nod imbricat poate lipsi (ex. {@code goals}/{@code score} la meciuri ne-incepute), asa ca
 * fiecare acces e aparat. Aici se izoleaza forma furnizorului — restul colectorului nu o cunoaste.
 */
public final class ApiFootballMapper {

    private ApiFootballMapper() {
    }

    public static FixtureDto toFixture(FixtureItem item) {
        FixtureItem.Fixture f = item.fixture();
        FixtureItem.League league = item.league();
        FixtureItem.Teams teams = item.teams();
        FixtureItem.Goals goals = item.goals();
        FixtureItem.Score score = item.score();

        FixtureItem.Venue venue = f != null ? f.venue() : null;
        FixtureItem.Status status = f != null ? f.status() : null;
        FixtureItem.Side home = teams != null ? teams.home() : null;
        FixtureItem.Side away = teams != null ? teams.away() : null;
        FixtureItem.Pair ht = score != null ? score.halftime() : null;
        FixtureItem.Pair ft = score != null ? score.fulltime() : null;
        FixtureItem.Pair et = score != null ? score.extratime() : null;
        FixtureItem.Pair pen = score != null ? score.penalty() : null;

        return new FixtureDto(
                f != null ? f.id() : null,
                f != null ? f.referee() : null,
                f != null ? f.timezone() : null,
                f != null ? parseOffset(f.date()) : null,
                league != null ? league.id() : null,
                league != null ? league.season() : null,
                league != null ? league.round() : null,
                venue != null ? venue.id() : null,
                status != null ? status.longStatus() : null,
                status != null ? status.shortStatus() : null,
                status != null ? status.elapsed() : null,
                home != null ? home.id() : null,
                away != null ? away.id() : null,
                goals != null ? goals.home() : null,
                goals != null ? goals.away() : null,
                ht != null ? ht.home() : null,
                ht != null ? ht.away() : null,
                ft != null ? ft.home() : null,
                ft != null ? ft.away() : null,
                et != null ? et.home() : null,
                et != null ? et.away() : null,
                pen != null ? pen.home() : null,
                pen != null ? pen.away() : null
        );
    }

    /** Un meci live cu evenimentele inline (din {@code fixtures?live=all}); evenimente {@code null} → lista goala. */
    public static FixtureLiveDto toFixtureLive(FixtureItem item) {
        FixtureDto fixture = toFixture(item);
        Long fixtureId = item.fixture() != null ? item.fixture().id() : null;
        List<FixtureEventDto> evenimente = new ArrayList<>();
        if (item.events() != null && fixtureId != null) {
            for (EventItem e : item.events()) {
                if (e != null) {
                    evenimente.add(toEvent(e, fixtureId));
                }
            }
        }
        return new FixtureLiveDto(fixture, evenimente);
    }

    public static TeamSeasonStatsDto toTeamSeasonStats(TeamStatisticsItem item) {
        TeamStatisticsItem.League league = item.league();
        TeamStatisticsItem.Fixtures fx = item.fixtures();
        TeamStatisticsItem.Goals goals = item.goals();
        TeamStatisticsItem.Side forGoals = goals != null ? goals.forGoals() : null;
        TeamStatisticsItem.Side against = goals != null ? goals.against() : null;
        return new TeamSeasonStatsDto(
                item.team() != null ? item.team().id() : null,
                league != null ? league.id() : null,
                league != null ? league.season() : null,
                item.form(),
                home(fx != null ? fx.played() : null), away(fx != null ? fx.played() : null), total(fx != null ? fx.played() : null),
                home(fx != null ? fx.wins() : null), away(fx != null ? fx.wins() : null), total(fx != null ? fx.wins() : null),
                home(fx != null ? fx.draws() : null), away(fx != null ? fx.draws() : null), total(fx != null ? fx.draws() : null),
                home(fx != null ? fx.loses() : null), away(fx != null ? fx.loses() : null), total(fx != null ? fx.loses() : null),
                home(forGoals != null ? forGoals.total() : null), away(forGoals != null ? forGoals.total() : null), total(forGoals != null ? forGoals.total() : null),
                avg(forGoals != null && forGoals.average() != null ? forGoals.average().home() : null),
                avg(forGoals != null && forGoals.average() != null ? forGoals.average().away() : null),
                avg(forGoals != null && forGoals.average() != null ? forGoals.average().total() : null),
                home(against != null ? against.total() : null), away(against != null ? against.total() : null), total(against != null ? against.total() : null),
                avg(against != null && against.average() != null ? against.average().home() : null),
                avg(against != null && against.average() != null ? against.average().away() : null),
                avg(against != null && against.average() != null ? against.average().total() : null),
                home(item.cleanSheet()), away(item.cleanSheet()), total(item.cleanSheet()),
                home(item.failedToScore()), away(item.failedToScore()), total(item.failedToScore()),
                sumCards(item.cards() != null ? item.cards().yellow() : null),
                sumCards(item.cards() != null ? item.cards().red() : null)
        );
    }

    public static PlayerSezonDto toPlayerSezon(PlayerItem item) {
        PlayerItem.Player p = item.player();
        Long playerId = p != null ? p.id() : null;
        PlayerItem.Birth birth = p != null ? p.birth() : null;
        PlayerDto profil = new PlayerDto(
                playerId,
                p != null ? p.name() : null,
                p != null ? p.firstname() : null,
                p != null ? p.lastname() : null,
                p != null ? p.age() : null,
                birth != null ? parseDate(birth.date()) : null,
                birth != null ? birth.place() : null,
                birth != null ? birth.country() : null,
                p != null ? p.nationality() : null,
                p != null ? p.height() : null,
                p != null ? p.weight() : null,
                p != null ? p.injured() : null,
                p != null ? p.photo() : null
        );
        List<PlayerSeasonStatsDto> statistici = new ArrayList<>();
        if (item.statistics() != null) {
            for (PlayerItem.Statistic s : item.statistics()) {
                if (s != null) {
                    statistici.add(toPlayerSeasonStats(s, playerId));
                }
            }
        }
        return new PlayerSezonDto(profil, statistici);
    }

    private static PlayerSeasonStatsDto toPlayerSeasonStats(PlayerItem.Statistic s, Long playerId) {
        PlayerItem.Games games = s.games();
        PlayerItem.Goals goals = s.goals();
        PlayerItem.Shots shots = s.shots();
        PlayerItem.Passes passes = s.passes();
        PlayerItem.Tackles tackles = s.tackles();
        PlayerItem.Duels duels = s.duels();
        PlayerItem.Dribbles dribbles = s.dribbles();
        PlayerItem.Fouls fouls = s.fouls();
        PlayerItem.Cards cards = s.cards();
        PlayerItem.Penalty penalty = s.penalty();
        return new PlayerSeasonStatsDto(
                playerId,
                s.team() != null ? s.team().id() : null,
                s.league() != null ? s.league().id() : null,
                s.league() != null ? s.league().season() : null,
                games != null ? games.position() : null,
                games != null ? games.appearences() : null,
                games != null ? games.lineups() : null,
                games != null ? games.minutes() : null,
                games != null ? toDecimal(games.rating()) : null,
                games != null ? games.captain() : null,
                goals != null ? goals.total() : null,
                goals != null ? goals.conceded() : null,
                goals != null ? goals.assists() : null,
                goals != null ? goals.saves() : null,
                shots != null ? shots.total() : null,
                shots != null ? shots.on() : null,
                passes != null ? passes.total() : null,
                passes != null ? passes.key() : null,
                passes != null ? passes.accuracy() : null,
                tackles != null ? tackles.total() : null,
                tackles != null ? tackles.blocks() : null,
                tackles != null ? tackles.interceptions() : null,
                duels != null ? duels.total() : null,
                duels != null ? duels.won() : null,
                dribbles != null ? dribbles.attempts() : null,
                dribbles != null ? dribbles.success() : null,
                fouls != null ? fouls.drawn() : null,
                fouls != null ? fouls.committed() : null,
                cards != null ? cards.yellow() : null,
                cards != null ? cards.yellowred() : null,
                cards != null ? cards.red() : null,
                penalty != null ? penalty.won() : null,
                penalty != null ? penalty.commited() : null,
                penalty != null ? penalty.scored() : null,
                penalty != null ? penalty.missed() : null,
                penalty != null ? penalty.saved() : null
        );
    }

    public static CoachDto toCoach(CoachItem item) {
        return new CoachDto(
                item.id(), item.name(), item.firstname(), item.lastname(),
                item.age(), item.nationality(), item.photo()
        );
    }

    /** Antrenorul curent al echipei cerute = intrarea cu {@code end == null} pe acea echipa; altfel {@code null}. */
    public static boolean esteAntrenorCurent(CoachItem item, long teamId) {
        if (item.career() == null) {
            return false;
        }
        return item.career().stream().anyMatch(c ->
                c != null && c.end() == null && c.team() != null
                        && c.team().id() != null && c.team().id() == teamId);
    }

    private static Integer home(TeamStatisticsItem.HomeAwayTotal h) {
        return h != null ? h.home() : null;
    }

    private static Integer away(TeamStatisticsItem.HomeAwayTotal h) {
        return h != null ? h.away() : null;
    }

    private static Integer total(TeamStatisticsItem.HomeAwayTotal h) {
        return h != null ? h.total() : null;
    }

    private static BigDecimal avg(String value) {
        return toDecimal(value);
    }

    private static Integer sumCards(Map<String, TeamStatisticsItem.Bucket> buckets) {
        if (buckets == null) {
            return null;
        }
        int sum = 0;
        boolean any = false;
        for (TeamStatisticsItem.Bucket b : buckets.values()) {
            if (b != null && b.total() != null) {
                sum += b.total();
                any = true;
            }
        }
        return any ? sum : null;
    }

    public static FixtureEventDto toEvent(EventItem e, long fixtureId) {
        EventItem.Time time = e.time();
        EventItem.Team team = e.team();
        EventItem.Player player = e.player();
        EventItem.Player assist = e.assist();
        return new FixtureEventDto(
                fixtureId,
                team != null ? team.id() : null,
                player != null ? player.id() : null,
                assist != null ? assist.id() : null,
                time != null ? time.elapsed() : null,
                time != null ? time.extra() : null,
                e.type(),
                e.detail(),
                e.comments()
        );
    }

    public static FixtureTeamStatsDto toFixtureTeamStats(StatisticsItem item, long fixtureId) {
        Map<String, Object> v = new HashMap<>();
        if (item.statistics() != null) {
            for (StatisticsItem.Stat stat : item.statistics()) {
                if (stat != null && stat.type() != null) {
                    v.put(stat.type(), stat.value());
                }
            }
        }
        StatisticsItem.Team team = item.team();
        return new FixtureTeamStatsDto(
                fixtureId,
                team != null ? team.id() : null,
                toInt(v.get("Shots on Goal")),
                toInt(v.get("Shots off Goal")),
                toInt(v.get("Total Shots")),
                toInt(v.get("Blocked Shots")),
                toInt(v.get("Shots insidebox")),
                toInt(v.get("Shots outsidebox")),
                toInt(v.get("Fouls")),
                toInt(v.get("Corner Kicks")),
                toInt(v.get("Offsides")),
                toDecimal(v.get("Ball Possession")),
                toInt(v.get("Yellow Cards")),
                toInt(v.get("Red Cards")),
                toInt(v.get("Goalkeeper Saves")),
                toInt(v.get("Total passes")),
                toInt(v.get("Passes accurate")),
                toDecimal(v.get("Passes %")),
                toDecimal(v.get("expected_goals"))
        );
    }

    /** Un element = o ECHIPA cu jucatorii ei; jucatorii fara id sau fara {@code statistics} sunt sariti. */
    public static List<FixturePlayerStatsDto> toFixturePlayerStats(FixturePlayersItem item, long fixtureId) {
        if (item.players() == null) {
            return List.of();
        }
        Long teamId = item.team() != null ? item.team().id() : null;
        List<FixturePlayerStatsDto> out = new ArrayList<>();
        for (FixturePlayersItem.PlayerEntry entry : item.players()) {
            if (entry == null || entry.player() == null || entry.player().id() == null
                    || entry.statistics() == null || entry.statistics().isEmpty()) {
                continue;
            }
            FixturePlayersItem.Statistic s = entry.statistics().get(0);
            if (s != null) {
                out.add(toFixturePlayerStats(s, fixtureId, teamId, entry.player()));
            }
        }
        return out;
    }

    private static FixturePlayerStatsDto toFixturePlayerStats(FixturePlayersItem.Statistic s, long fixtureId,
                                                              Long teamId, FixturePlayersItem.Player player) {
        FixturePlayersItem.Games games = s.games();
        FixturePlayersItem.Goals goals = s.goals();
        FixturePlayersItem.Shots shots = s.shots();
        FixturePlayersItem.Passes passes = s.passes();
        FixturePlayersItem.Tackles tackles = s.tackles();
        FixturePlayersItem.Duels duels = s.duels();
        FixturePlayersItem.Dribbles dribbles = s.dribbles();
        FixturePlayersItem.Fouls fouls = s.fouls();
        FixturePlayersItem.Cards cards = s.cards();
        FixturePlayersItem.Penalty penalty = s.penalty();
        return new FixturePlayerStatsDto(
                fixtureId,
                teamId,
                player.id(),
                player.name(),
                games != null ? games.minutes() : null,
                games != null ? toDecimal(games.rating()) : null,
                games != null ? games.captain() : null,
                games != null ? games.substitute() : null,
                games != null ? games.position() : null,
                shots != null ? shots.total() : null,
                shots != null ? shots.on() : null,
                goals != null ? goals.total() : null,
                goals != null ? goals.conceded() : null,
                goals != null ? goals.assists() : null,
                goals != null ? goals.saves() : null,
                passes != null ? passes.total() : null,
                passes != null ? passes.key() : null,
                passes != null ? toInt(passes.accuracy()) : null,
                tackles != null ? tackles.total() : null,
                tackles != null ? tackles.blocks() : null,
                tackles != null ? tackles.interceptions() : null,
                duels != null ? duels.total() : null,
                duels != null ? duels.won() : null,
                dribbles != null ? dribbles.attempts() : null,
                dribbles != null ? dribbles.success() : null,
                fouls != null ? fouls.drawn() : null,
                fouls != null ? fouls.committed() : null,
                cards != null ? cards.yellow() : null,
                cards != null ? cards.red() : null,
                penalty != null ? penalty.won() : null,
                penalty != null ? penalty.commited() : null,
                penalty != null ? penalty.scored() : null,
                penalty != null ? penalty.missed() : null,
                penalty != null ? penalty.saved() : null
        );
    }

    public static FixtureLineupDto toFixtureLineup(LineupItem item, long fixtureId) {
        Long teamId = item.team() != null ? item.team().id() : null;
        List<FixtureLineupPlayerDto> players = new ArrayList<>();
        addLineupPlayers(players, item.startXI(), fixtureId, teamId, false);
        addLineupPlayers(players, item.substitutes(), fixtureId, teamId, true);
        return new FixtureLineupDto(
                fixtureId,
                teamId,
                item.formation(),
                item.coach() != null ? item.coach().id() : null,
                players
        );
    }

    private static void addLineupPlayers(List<FixtureLineupPlayerDto> out, List<LineupItem.Slot> slots,
                                         long fixtureId, Long teamId, boolean substitute) {
        if (slots == null) {
            return;
        }
        for (LineupItem.Slot slot : slots) {
            LineupItem.Player p = slot != null ? slot.player() : null;
            if (p == null) {
                continue;
            }
            out.add(new FixtureLineupPlayerDto(fixtureId, teamId, p.id(), p.name(),
                    p.number(), p.pos(), p.grid(), substitute));
        }
    }

    public static InjuryDto toInjury(InjuryItem item) {
        InjuryItem.Player player = item.player();
        InjuryItem.Fixture fixture = item.fixture();
        InjuryItem.League league = item.league();
        OffsetDateTime kickoff = fixture != null ? parseOffset(fixture.date()) : null;
        return new InjuryDto(
                player != null ? player.id() : null,
                player != null ? player.name() : null,
                item.team() != null ? item.team().id() : null,
                fixture != null ? fixture.id() : null,
                league != null ? league.id() : null,
                league != null ? league.season() : null,
                player != null ? player.type() : null,
                player != null ? player.reason() : null,
                kickoff != null ? kickoff.toLocalDate() : null
        );
    }

    public static List<StandingDto> toStandings(StandingsLeagueItem item) {
        StandingsLeagueItem.League league = item.league();
        if (league == null || league.standings() == null) {
            return List.of();
        }
        List<StandingDto> out = new ArrayList<>();
        for (List<StandingsLeagueItem.Row> group : league.standings()) {
            if (group == null) {
                continue;
            }
            for (StandingsLeagueItem.Row row : group) {
                out.add(toStanding(row, league.id(), league.season()));
            }
        }
        return out;
    }

    public static TeamDto toTeam(TeamItem item) {
        TeamItem.Team t = item.team();
        TeamItem.Venue v = item.venue();
        return new TeamDto(
                t != null ? t.id() : null,
                t != null ? t.name() : null,
                t != null ? t.code() : null,
                t != null ? t.country() : null,
                t != null ? t.founded() : null,
                t != null ? t.national() : null,
                t != null ? t.logo() : null,
                v != null ? v.id() : null
        );
    }

    public static LeagueDto toLeague(LeagueItem item) {
        LeagueItem.League l = item.league();
        LeagueItem.Country c = item.country();
        return new LeagueDto(
                l != null ? l.id() : null,
                l != null ? l.name() : null,
                l != null ? l.type() : null,
                l != null ? l.logo() : null,
                c != null ? c.name() : null
        );
    }

    public static List<SeasonDto> toSeasons(LeagueItem item, long leagueId) {
        if (item.seasons() == null) {
            return List.of();
        }
        return item.seasons().stream().map(s -> toSeason(s, leagueId)).toList();
    }

    private static StandingDto toStanding(StandingsLeagueItem.Row r, Long leagueId, Integer season) {
        StandingsLeagueItem.Stat all = r.all();
        StandingsLeagueItem.Stat home = r.home();
        StandingsLeagueItem.Stat away = r.away();
        StandingsLeagueItem.Team team = r.team();
        return new StandingDto(
                leagueId, season, team != null ? team.id() : null, r.rank(), r.group(),
                r.points(), r.goalsDiff(), r.form(), r.status(), r.description(),
                played(all), win(all), draw(all), lose(all), goalsFor(all), goalsAgainst(all),
                played(home), win(home), draw(home), lose(home), goalsFor(home), goalsAgainst(home),
                played(away), win(away), draw(away), lose(away), goalsFor(away), goalsAgainst(away)
        );
    }

    private static SeasonDto toSeason(LeagueItem.Season s, long leagueId) {
        LeagueItem.Coverage cov = s.coverage();
        LeagueItem.Fixtures fx = cov != null ? cov.fixtures() : null;
        return new SeasonDto(
                leagueId, s.year(), parseDate(s.start()), parseDate(s.end()), s.current(),
                fx != null ? fx.events() : null,
                fx != null ? fx.lineups() : null,
                fx != null ? fx.statisticsFixtures() : null,
                fx != null ? fx.statisticsPlayers() : null,
                cov != null ? cov.standings() : null
        );
    }

    private static Integer played(StandingsLeagueItem.Stat s) {
        return s != null ? s.played() : null;
    }

    private static Integer win(StandingsLeagueItem.Stat s) {
        return s != null ? s.win() : null;
    }

    private static Integer draw(StandingsLeagueItem.Stat s) {
        return s != null ? s.draw() : null;
    }

    private static Integer lose(StandingsLeagueItem.Stat s) {
        return s != null ? s.lose() : null;
    }

    private static Integer goalsFor(StandingsLeagueItem.Stat s) {
        return s != null && s.goals() != null ? s.goals().forGoals() : null;
    }

    private static Integer goalsAgainst(StandingsLeagueItem.Stat s) {
        return s != null && s.goals() != null ? s.goals().against() : null;
    }

    private static Integer toInt(Object value) {
        BigDecimal d = toDecimal(value);
        return d != null ? d.intValue() : null;
    }

    /** `value` din /fixtures/statistics e eterogen: numar, "62%", "1.8" sau null. */
    private static BigDecimal toDecimal(Object value) {
        if (value instanceof Number n) {
            return new BigDecimal(n.toString());
        }
        if (value instanceof String s) {
            String cleaned = s.replace("%", "").trim();
            if (cleaned.isEmpty()) {
                return null;
            }
            try {
                return new BigDecimal(cleaned);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private static OffsetDateTime parseOffset(String value) {
        return value != null ? OffsetDateTime.parse(value) : null;
    }

    private static LocalDate parseDate(String value) {
        return value != null ? LocalDate.parse(value) : null;
    }
}
