package ro.golstat.collector.provider.apifootball;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import ro.golstat.collector.provider.DataProvider;
import ro.golstat.common.GolstatConstants;
import ro.golstat.common.dto.FixtureDto;
import ro.golstat.common.dto.FixtureEventDto;
import ro.golstat.common.dto.LeagueDto;
import ro.golstat.common.dto.SeasonDto;
import ro.golstat.common.dto.StandingDto;
import ro.golstat.common.dto.TeamDto;
import ro.golstat.common.dto.VenueDto;

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
