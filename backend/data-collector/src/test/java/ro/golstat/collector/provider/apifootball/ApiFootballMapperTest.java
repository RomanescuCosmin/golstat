package ro.golstat.collector.provider.apifootball;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import ro.golstat.common.dto.FixtureDto;
import ro.golstat.common.dto.FixtureEventDto;
import ro.golstat.common.dto.FixtureLineupDto;
import ro.golstat.common.dto.FixtureLineupPlayerDto;
import ro.golstat.common.dto.FixtureTeamStatsDto;
import ro.golstat.common.dto.InjuryDto;
import ro.golstat.common.dto.LeagueDto;
import ro.golstat.common.dto.SeasonDto;
import ro.golstat.common.dto.StandingDto;
import ro.golstat.common.dto.TeamDto;

import java.math.BigDecimal;
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
    void statistics_mapTypeValuePairs_perTeam() throws Exception {
        String body = """
                {"response":[
                  {"team":{"id":33,"name":"Manchester United"},"statistics":[
                    {"type":"Shots on Goal","value":6},
                    {"type":"Shots off Goal","value":5},
                    {"type":"Total Shots","value":15},
                    {"type":"Blocked Shots","value":4},
                    {"type":"Shots insidebox","value":10},
                    {"type":"Shots outsidebox","value":5},
                    {"type":"Fouls","value":11},
                    {"type":"Corner Kicks","value":7},
                    {"type":"Offsides","value":2},
                    {"type":"Ball Possession","value":"62%"},
                    {"type":"Yellow Cards","value":1},
                    {"type":"Red Cards","value":null},
                    {"type":"Goalkeeper Saves","value":3},
                    {"type":"Total passes","value":598},
                    {"type":"Passes accurate","value":532},
                    {"type":"Passes %","value":"89%"},
                    {"type":"expected_goals","value":"1.8"}
                  ]},
                  {"team":{"id":34,"name":"Newcastle"},"statistics":[
                    {"type":"Shots on Goal","value":2},
                    {"type":"Corner Kicks","value":3},
                    {"type":"Ball Possession","value":"38%"},
                    {"type":"Tip Necunoscut","value":"n/a"}
                  ]}
                ]}""";
        List<StatisticsItem> items = parse(body, StatisticsItem.class);
        List<FixtureTeamStatsDto> stats = items.stream()
                .map(i -> ApiFootballMapper.toFixtureTeamStats(i, 215L))
                .toList();

        assertEquals(2, stats.size());
        FixtureTeamStatsDto home = stats.get(0);
        assertEquals(215L, home.fixtureId());
        assertEquals(33L, home.teamId());
        assertEquals(6, home.shotsOnGoal());
        assertEquals(5, home.shotsOffGoal());
        assertEquals(15, home.shotsTotal());
        assertEquals(4, home.shotsBlocked());
        assertEquals(10, home.shotsInsidebox());
        assertEquals(5, home.shotsOutsidebox());
        assertEquals(11, home.fouls());
        assertEquals(7, home.cornerKicks());
        assertEquals(2, home.offsides());
        assertEquals(new BigDecimal("62"), home.ballPossession());
        assertEquals(1, home.yellowCards());
        assertNull(home.redCards());
        assertEquals(3, home.goalkeeperSaves());
        assertEquals(598, home.passesTotal());
        assertEquals(532, home.passesAccurate());
        assertEquals(new BigDecimal("89"), home.passesPercentage());
        assertEquals(new BigDecimal("1.8"), home.expectedGoals());

        FixtureTeamStatsDto away = stats.get(1);
        assertEquals(34L, away.teamId());
        assertEquals(2, away.shotsOnGoal());
        assertEquals(3, away.cornerKicks());
        assertEquals(new BigDecimal("38"), away.ballPossession());
        assertNull(away.fouls());   // tip absent → null, tip necunoscut → ignorat
    }

    @Test
    void lineups_mapStartXiAndSubstitutes_withFixtureIdFromRequest() throws Exception {
        String body = """
                {"response":[{
                  "team":{"id":50,"name":"Manchester City","logo":"logo.png"},
                  "formation":"4-3-3",
                  "startXI":[
                    {"player":{"id":617,"name":"Ederson","number":31,"pos":"G","grid":"1:1"}},
                    {"player":{"id":627,"name":"K. Walker","number":2,"pos":"D","grid":"2:4"}}
                  ],
                  "substitutes":[
                    {"player":{"id":50828,"name":"Z. Steffen","number":13,"pos":"G","grid":null}}
                  ],
                  "coach":{"id":4,"name":"Guardiola"}
                }]}""";
        FixtureLineupDto lineup = ApiFootballMapper.toFixtureLineup(parse(body, LineupItem.class).get(0), 215L);

        assertEquals(215L, lineup.fixtureId());
        assertEquals(50L, lineup.teamId());
        assertEquals("4-3-3", lineup.formation());
        assertEquals(4L, lineup.coachId());
        assertEquals(3, lineup.players().size());
        FixtureLineupPlayerDto portar = lineup.players().get(0);
        assertEquals(215L, portar.fixtureId());
        assertEquals(50L, portar.teamId());
        assertEquals(617L, portar.playerId());
        assertEquals("Ederson", portar.playerName());
        assertEquals(31, portar.number());
        assertEquals("G", portar.position());
        assertEquals("1:1", portar.grid());
        assertEquals(false, portar.isSubstitute());
        FixtureLineupPlayerDto rezerva = lineup.players().get(2);
        assertEquals(50828L, rezerva.playerId());
        assertEquals(true, rezerva.isSubstitute());
        assertNull(rezerva.grid());
    }

    @Test
    void lineups_missingCoachAndSubstitutes_tolerated() throws Exception {
        String body = """
                {"response":[{
                  "team":{"id":51},
                  "formation":null,
                  "startXI":[{"player":{"id":700,"name":"Jucator","number":9,"pos":"F","grid":"4:1"}}],
                  "substitutes":null,
                  "coach":null
                }]}""";
        FixtureLineupDto lineup = ApiFootballMapper.toFixtureLineup(parse(body, LineupItem.class).get(0), 900L);

        assertNull(lineup.formation());
        assertNull(lineup.coachId());
        assertEquals(1, lineup.players().size());
    }

    @Test
    void injuries_mapPlayerTeamFixtureLeague_andReportedDate() throws Exception {
        String body = """
                {"response":[
                  {"player":{"id":865,"name":"D. Costa","type":"Missing Fixture","reason":"Broken ankle"},
                   "team":{"id":157,"name":"Bayern Munich"},
                   "fixture":{"id":686314,"timezone":"UTC","date":"2026-07-07T21:00:00+02:00"},
                   "league":{"id":78,"season":2026}},
                  {"player":{"id":900,"name":"J. Incert","type":"Questionable","reason":"Knock"},
                   "team":{"id":157},
                   "fixture":null,
                   "league":{"id":78,"season":2026}}
                ]}""";
        List<InjuryDto> injuries = parse(body, InjuryItem.class).stream()
                .map(ApiFootballMapper::toInjury)
                .toList();

        assertEquals(2, injuries.size());
        InjuryDto prima = injuries.get(0);
        assertEquals(865L, prima.playerId());
        assertEquals("D. Costa", prima.playerName());
        assertEquals(157L, prima.teamId());
        assertEquals(686314L, prima.fixtureId());
        assertEquals(78L, prima.leagueId());
        assertEquals(2026, prima.seasonYear());
        assertEquals("Missing Fixture", prima.type());
        assertEquals("Broken ankle", prima.reason());
        assertEquals(LocalDate.of(2026, 7, 7), prima.reportedAt());
        InjuryDto aDoua = injuries.get(1);
        assertEquals("Questionable", aDoua.type());
        assertNull(aDoua.fixtureId());
        assertNull(aDoua.reportedAt());
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
