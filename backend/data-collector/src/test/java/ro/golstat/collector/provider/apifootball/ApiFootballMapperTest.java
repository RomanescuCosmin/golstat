package ro.golstat.collector.provider.apifootball;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import ro.golstat.common.dto.FixtureDto;
import ro.golstat.common.dto.FixtureEventDto;
import ro.golstat.common.dto.LeagueDto;
import ro.golstat.common.dto.SeasonDto;
import ro.golstat.common.dto.StandingDto;
import ro.golstat.common.dto.TeamDto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifica forma record-urilor + maparea, pe JSON API-Football realist (inclusiv campuri null). */
class ApiFootballMapperTest {

    // API-Football trimite 200 chiar la erori/rezultate goale → mapper-ul e lenient la campuri necunoscute.
    private final JsonMapper json = JsonMapper.builder()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .build();

    private <T> List<T> parse(String body, Class<T> itemType) throws Exception {
        JavaType type = json.getTypeFactory().constructParametricType(ApiFootballResponse.class, itemType);
        ApiFootballResponse<T> response = json.readValue(body, type);
        return response.response();
    }

    @Test
    void fixture_mapsScoresTeamsAndKickoff() throws Exception {
        String body = """
                {"response":[{
                  "fixture":{"id":215,"referee":"M. Oliver","timezone":"UTC",
                    "date":"2024-08-10T18:00:00+00:00",
                    "venue":{"id":556,"name":"Old Trafford","city":"Manchester"},
                    "status":{"long":"Match Finished","short":"FT","elapsed":90}},
                  "league":{"id":39,"season":2024,"round":"Regular Season - 1"},
                  "teams":{"home":{"id":33},"away":{"id":34}},
                  "goals":{"home":2,"away":1},
                  "score":{"halftime":{"home":1,"away":0},"fulltime":{"home":2,"away":1},
                           "extratime":{"home":null,"away":null},"penalty":{"home":null,"away":null}}
                }]}""";
        FixtureDto f = ApiFootballMapper.toFixture(parse(body, FixtureItem.class).get(0));

        assertEquals(215L, f.id());
        assertEquals("M. Oliver", f.referee());
        assertEquals(OffsetDateTime.parse("2024-08-10T18:00:00+00:00"), f.kickoff());
        assertEquals(39L, f.leagueId());
        assertEquals(2024, f.seasonYear());
        assertEquals("Regular Season - 1", f.round());
        assertEquals(556L, f.venueId());
        assertEquals("FT", f.statusShort());
        assertEquals(90, f.statusElapsed());
        assertEquals(33L, f.homeTeamId());
        assertEquals(34L, f.awayTeamId());
        assertEquals(2, f.goalsHome());
        assertEquals(1, f.goalsAway());
        assertEquals(1, f.scoreHtHome());
        assertEquals(2, f.scoreFtHome());
        assertNull(f.scoreEtHome());
        assertNull(f.scorePenAway());
    }

    @Test
    void fixture_notStarted_nullGoalsTolerated() throws Exception {
        String body = """
                {"response":[{
                  "fixture":{"id":900,"referee":null,"timezone":"UTC","date":"2025-01-01T15:00:00+00:00",
                    "venue":{"id":null,"name":null,"city":null},
                    "status":{"long":"Not Started","short":"NS","elapsed":null}},
                  "league":{"id":39,"season":2024,"round":"Regular Season - 20"},
                  "teams":{"home":{"id":40},"away":{"id":41}},
                  "goals":{"home":null,"away":null},
                  "score":{"halftime":{"home":null,"away":null},"fulltime":{"home":null,"away":null},
                           "extratime":{"home":null,"away":null},"penalty":{"home":null,"away":null}}
                }]}""";
        FixtureDto f = ApiFootballMapper.toFixture(parse(body, FixtureItem.class).get(0));

        assertEquals("NS", f.statusShort());
        assertNull(f.goalsHome());
        assertNull(f.venueId());
        assertNull(f.statusElapsed());
    }

    @Test
    void events_carryFixtureIdFromRequest() throws Exception {
        String body = """
                {"response":[
                  {"time":{"elapsed":23,"extra":null},"team":{"id":33},"player":{"id":874},
                   "assist":{"id":875},"type":"Goal","detail":"Normal Goal","comments":null},
                  {"time":{"elapsed":90,"extra":4},"team":{"id":34},"player":{"id":880},
                   "assist":{"id":null},"type":"Card","detail":"Yellow Card","comments":null}
                ]}""";
        List<EventItem> items = parse(body, EventItem.class);
        List<FixtureEventDto> events = items.stream().map(e -> ApiFootballMapper.toEvent(e, 215L)).toList();

        assertEquals(2, events.size());
        assertTrue(events.stream().allMatch(e -> e.fixtureId() == 215L));
        assertEquals(23, events.get(0).timeElapsed());
        assertEquals(874L, events.get(0).playerId());
        assertEquals("Goal", events.get(0).type());
        assertEquals(4, events.get(1).timeExtra());
        assertNull(events.get(1).assistId());
    }

    @Test
    void standings_flattenGroupsWithLeagueSeason() throws Exception {
        String body = """
                {"response":[{"league":{"id":39,"season":2024,"standings":[[
                  {"rank":1,"team":{"id":33},"points":12,"goalsDiff":8,"group":"Premier League",
                   "form":"WWWW","status":"same","description":"Champions League",
                   "all":{"played":5,"win":4,"draw":0,"lose":1,"goals":{"for":11,"against":3}},
                   "home":{"played":3,"win":3,"draw":0,"lose":0,"goals":{"for":7,"against":1}},
                   "away":{"played":2,"win":1,"draw":0,"lose":1,"goals":{"for":4,"against":2}}},
                  {"rank":2,"team":{"id":34},"points":7,"goalsDiff":2,"group":"Premier League",
                   "form":"WLDW","status":"same","description":null,
                   "all":{"played":5,"win":2,"draw":1,"lose":2,"goals":{"for":6,"against":4}},
                   "home":{"played":3,"win":2,"draw":0,"lose":1,"goals":{"for":4,"against":2}},
                   "away":{"played":2,"win":0,"draw":1,"lose":1,"goals":{"for":2,"against":2}}}
                ]]}}]}""";
        List<StandingsLeagueItem> items = parse(body, StandingsLeagueItem.class);
        List<StandingDto> table = items.stream()
                .flatMap(i -> ApiFootballMapper.toStandings(i).stream())
                .toList();

        assertEquals(2, table.size());
        StandingDto first = table.get(0);
        assertEquals(39L, first.leagueId());
        assertEquals(2024, first.seasonYear());
        assertEquals(33L, first.teamId());
        assertEquals(1, first.rank());
        assertEquals(12, first.points());
        assertEquals("Premier League", first.groupName());
        assertEquals(11, first.goalsForAll());
        assertEquals(3, first.goalsAgainstAll());
        assertEquals(7, first.goalsForHome());
        assertEquals(4, first.goalsForAway());
        assertNull(table.get(1).description());
    }

    @Test
    void team_mapsCatalogFields() throws Exception {
        String body = """
                {"response":[{"team":{"id":33,"name":"Manchester United","code":"MUN",
                   "country":"England","founded":1878,"national":false,"logo":"logo.png"},
                   "venue":{"id":556}}]}""";
        TeamDto t = ApiFootballMapper.toTeam(parse(body, TeamItem.class).get(0));

        assertEquals(33L, t.id());
        assertEquals("Manchester United", t.name());
        assertEquals("England", t.countryName());
        assertEquals(1878, t.founded());
        assertEquals(false, t.isNational());
        assertEquals(556L, t.venueId());
    }

    @Test
    void league_andSeasons_mapWithLeagueId() throws Exception {
        String body = """
                {"response":[{
                  "league":{"id":39,"name":"Premier League","type":"League","logo":"pl.png"},
                  "country":{"name":"England"},
                  "seasons":[
                    {"year":2023,"start":"2023-08-11","end":"2024-05-19","current":false,
                     "coverage":{"fixtures":{"events":true,"lineups":true,
                       "statistics_fixtures":true,"statistics_players":true},"standings":true}},
                    {"year":2024,"start":"2024-08-16","end":"2025-05-25","current":true,
                     "coverage":{"fixtures":{"events":true,"lineups":false,
                       "statistics_fixtures":true,"statistics_players":false},"standings":true}}
                  ]}]}""";
        LeagueItem item = parse(body, LeagueItem.class).get(0);

        LeagueDto league = ApiFootballMapper.toLeague(item);
        assertEquals(39L, league.id());
        assertEquals("Premier League", league.name());
        assertEquals("England", league.countryName());

        List<SeasonDto> seasons = ApiFootballMapper.toSeasons(item, 39L);
        assertEquals(2, seasons.size());
        SeasonDto current = seasons.get(1);
        assertEquals(39L, current.leagueId());
        assertEquals(2024, current.year());
        assertEquals(LocalDate.of(2024, 8, 16), current.startDate());
        assertEquals(LocalDate.of(2025, 5, 25), current.endDate());
        assertEquals(true, current.isCurrent());
        assertEquals(false, current.hasLineups());
        assertEquals(true, current.hasStandings());
    }
}
