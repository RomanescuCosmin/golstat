package ro.golstat.collector.provider.apifootball;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import ro.golstat.collector.provider.DataProvider;
import ro.golstat.common.GolstatConstants;
import ro.golstat.common.dto.CoachDto;
import ro.golstat.common.dto.FixtureDto;
import ro.golstat.common.dto.FixtureEventDto;
import ro.golstat.common.dto.FixtureLineupDto;
import ro.golstat.common.dto.FixtureLiveDto;
import ro.golstat.common.dto.FixtureTeamStatsDto;
import ro.golstat.common.dto.InjuryDto;
import ro.golstat.common.dto.LeagueDto;
import ro.golstat.common.dto.PlayerSezonDto;
import ro.golstat.common.dto.SeasonDto;
import ro.golstat.common.dto.StandingDto;
import ro.golstat.common.dto.TeamDto;
import ro.golstat.common.dto.TeamSeasonStatsDto;
import ro.golstat.common.dto.VenueDto;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Furnizorul real (API-Football). Activ oriunde NU e profilul {@code stub}, deci inlocuieste
 * {@link ro.golstat.collector.provider.StubDataProvider} fara alte modificari. Traduce parametrii
 * de domeniu in query-uri API-Football si deleaga maparea raspunsului la {@link ApiFootballMapper}.
 */
@Component
@Profile("!stub")
public class ApiFootballProvider implements DataProvider {

    private final ApiFootballClient client;
    private final ApiFootballProperties props;

    public ApiFootballProvider(ApiFootballClient client, ApiFootballProperties props) {
        this.client = client;
        this.props = props;
    }

    @Override
    public List<FixtureDto> fixtures(long leagueId, int season, LocalDate from, LocalDate to) {
        Map<String, Object> params = Map.of(
                "league", leagueId, "season", season,
                "from", from.toString(), "to", to.toString());
        // fereastra contine meciuri viitoare/live → TTL scurt
        return client.get(GolstatConstants.ApiFootball.FIXTURES, params, FixtureItem.class, props.ttlUpcoming()).stream()
                .map(ApiFootballMapper::toFixture)
                .toList();
    }

    @Override
    public List<FixtureEventDto> fixtureEvents(long fixtureId) {
        // evenimentele unui meci terminat sunt imuabile → TTL lung
        return client.get(GolstatConstants.ApiFootball.FIXTURES_EVENTS, Map.of("fixture", fixtureId),
                        EventItem.class, props.ttlHistoric()).stream()
                .map(e -> ApiFootballMapper.toEvent(e, fixtureId))
                .toList();
    }

    @Override
    public List<FixtureTeamStatsDto> fixtureStatistics(long fixtureId) {
        // statisticile unui meci terminat sunt imuabile → TTL lung
        return client.get(GolstatConstants.ApiFootball.FIXTURES_STATISTICS, Map.of("fixture", fixtureId),
                        StatisticsItem.class, props.ttlHistoric()).stream()
                .map(s -> ApiFootballMapper.toFixtureTeamStats(s, fixtureId))
                .toList();
    }

    @Override
    public List<FixtureLineupDto> fixtureLineups(long fixtureId) {
        // formatia anuntata a unui meci nu se mai schimba → TTL lung
        return client.get(GolstatConstants.ApiFootball.FIXTURES_LINEUPS, Map.of("fixture", fixtureId),
                        LineupItem.class, props.ttlHistoric()).stream()
                .map(l -> ApiFootballMapper.toFixtureLineup(l, fixtureId))
                .toList();
    }

    @Override
    public List<InjuryDto> injuries(long leagueId, int season) {
        // lista de indisponibili se schimba de la o zi la alta → TTL scurt
        Map<String, Object> params = Map.of("league", leagueId, "season", season);
        return client.get(GolstatConstants.ApiFootball.INJURIES, params, InjuryItem.class, props.ttlUpcoming()).stream()
                .map(ApiFootballMapper::toInjury)
                .toList();
    }

    @Override
    public List<FixtureDto> fixturesByIds(java.util.Collection<Long> fixtureIds) {
        if (fixtureIds == null || fixtureIds.isEmpty()) {
            return List.of();
        }
        // API-Football: ids=id1-id2-... (max 20). Stare curenta → fara cache (TTL 0).
        String ids = fixtureIds.stream().limit(20).map(String::valueOf)
                .collect(java.util.stream.Collectors.joining("-"));
        return client.get(GolstatConstants.ApiFootball.FIXTURES, Map.of("ids", ids),
                        FixtureItem.class, Duration.ZERO).stream()
                .map(ApiFootballMapper::toFixture)
                .toList();
    }

    @Override
    public List<FixtureLiveDto> liveFixtures() {
        // `live=all` → toate meciurile live din lume intr-un singur request; ttl=0 = fara cache.
        // Evenimentele vin INLINE (gratis) → le pastram in FixtureLiveDto.
        return client.get(GolstatConstants.ApiFootball.FIXTURES, Map.of("live", "all"),
                        FixtureItem.class, Duration.ZERO).stream()
                .map(ApiFootballMapper::toFixtureLive)
                .toList();
    }

    @Override
    public List<FixtureTeamStatsDto> liveFixtureStatistics(long fixtureId) {
        // statisticile se schimba in timpul jocului → TTL 0 (ocoleste cache-ul istoric de 24h).
        return client.get(GolstatConstants.ApiFootball.FIXTURES_STATISTICS, Map.of("fixture", fixtureId),
                        StatisticsItem.class, Duration.ZERO).stream()
                .map(s -> ApiFootballMapper.toFixtureTeamStats(s, fixtureId))
                .toList();
    }

    @Override
    public List<TeamSeasonStatsDto> teamStatistics(long leagueId, int season, long teamId) {
        Map<String, Object> params = Map.of("league", leagueId, "season", season, "team", teamId);
        return client.get(GolstatConstants.ApiFootball.TEAMS_STATISTICS, params,
                        TeamStatisticsItem.class, props.ttlTeamStats()).stream()
                .map(ApiFootballMapper::toTeamSeasonStats)
                .toList();
    }

    @Override
    public List<PlayerSezonDto> players(long teamId, int season) {
        Map<String, Object> params = Map.of("team", teamId, "season", season);
        return client.getPaged(GolstatConstants.ApiFootball.PLAYERS, params,
                        PlayerItem.class, props.ttlPlayers()).stream()
                .map(ApiFootballMapper::toPlayerSezon)
                .toList();
    }

    @Override
    public List<CoachDto> coaches(long teamId) {
        // /coachs?team=X → antrenorii legati de echipa; pastram doar pe cel CURENT (career end==null pe X).
        return client.get(GolstatConstants.ApiFootball.COACHS, Map.of("team", teamId),
                        CoachItem.class, props.ttlCoaches()).stream()
                .filter(c -> ApiFootballMapper.esteAntrenorCurent(c, teamId))
                .map(ApiFootballMapper::toCoach)
                .toList();
    }

    @Override
    public List<StandingDto> standings(long leagueId, int season) {
        Map<String, Object> params = Map.of("league", leagueId, "season", season);
        return client.get(GolstatConstants.ApiFootball.STANDINGS, params, StandingsLeagueItem.class, props.cacheTtl()).stream()
                .flatMap(item -> ApiFootballMapper.toStandings(item).stream())
                .toList();
    }

    @Override
    public List<TeamDto> teams(long leagueId, int season) {
        Map<String, Object> params = Map.of("league", leagueId, "season", season);
        return client.get(GolstatConstants.ApiFootball.TEAMS, params, TeamItem.class, props.ttlHistoric()).stream()
                .map(ApiFootballMapper::toTeam)
                .toList();
    }

    @Override
    public List<LeagueDto> leagues() {
        return client.get(GolstatConstants.ApiFootball.LEAGUES, Map.of(), LeagueItem.class, props.ttlHistoric()).stream()
                .map(ApiFootballMapper::toLeague)
                .toList();
    }

    @Override
    public List<SeasonDto> seasons(long leagueId) {
        return client.get(GolstatConstants.ApiFootball.LEAGUES, Map.of("id", leagueId), LeagueItem.class, props.ttlHistoric()).stream()
                .flatMap(item -> ApiFootballMapper.toSeasons(item, leagueId).stream())
                .toList();
    }

    @Override
    public List<VenueDto> venues() {
        // /venues cere OBLIGATORIU un parametru de cautare (nu exista "toate stadioanele"), deci nu-l
        // apelam aici. Randurile de venue se creeaza ca placeholder din venueId-ul meciului la ingest
        // si se imbogatesc separat — nu ardem un request.
        return List.of();
    }
}
