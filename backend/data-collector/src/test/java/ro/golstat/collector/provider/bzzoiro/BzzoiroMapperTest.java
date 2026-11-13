package ro.golstat.collector.provider.bzzoiro;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import ro.golstat.collector.provider.bzzoiro.BzzoiroMapper.Echipe;
import ro.golstat.common.dto.FixtureDto;
import ro.golstat.common.dto.FixtureEventDto;
import ro.golstat.common.dto.FixtureLineupDto;
import ro.golstat.common.dto.FixtureTeamHalfStatsDto;
import ro.golstat.common.dto.FixtureTeamStatsDto;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BzzoiroMapperTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Echipe ECHIPE = new Echipe(17L, 3L);

    private static JsonNode json(String s) throws Exception {
        return JSON.readTree(s.replace('\'', '"'));
    }

    @Test
    void meciTerminatPrimesteIdDeLigaAlAplicatieiSiStatusFT() throws Exception {
        FixtureDto f = BzzoiroMapper.toFixture(json("""
                {'id':209632,'league_id':1,'season_id':1058,'home_team_id':17,'away_team_id':3,'venue_id':17,
                 'event_date':'2026-11-08T16:30:00+00:00','status':'finished','period':'FT','current_minute':96,
                 'home_score':4,'away_score':4,'home_score_ht':2,'away_score_ht':2,
                 'penalty_shootout':null,'extra_time_score':null,'round_label':'Regular season · Matchday 10'}
                """), 39L, 2026);

        assertEquals(39L, f.leagueId());
        assertEquals("FT", f.statusShort());
        assertEquals(4, f.scoreFtHome());
        assertEquals(2, f.scoreHtAway());
        assertEquals(17L, f.homeTeamId());
    }

    @Test
    void meciViitorNuAreScorFinal() throws Exception {
        FixtureDto f = BzzoiroMapper.toFixture(json("""
                {'id':1,'status':'notstarted','home_team_id':1,'away_team_id':2,'home_score':null,'away_score':null}
                """), 39L, 2026);

        assertEquals("NS", f.statusShort());
        assertEquals(null, f.scoreFtHome());
    }

    @Test
    void incidenteleDevinEvenimenteCuVocabularulApiFootball() throws Exception {
        List<FixtureEventDto> ev = BzzoiroMapper.toEvents(json("""
                {'incidents':[
                  {'type':'period','minute':90},
                  {'type':'card','minute':90,'added_time':7,'is_home':false,'card_type':'yellow','player_id':4055},
                  {'type':'card','minute':50,'is_home':true,'card_type':'yellow','player_id':9,'rescinded':true},
                  {'type':'goal','minute':90,'added_time':6,'is_home':true,'player_id':1801,'goal_type':'penalty',
                   'sequence':[{'pid':1794,'assist':true},{'pid':1801,'event':'goal'}]},
                  {'type':'substitution','minute':86,'is_home':false,'player_in_id':4539,'player_out_id':2471}
                ]}
                """), 209632L, ECHIPE);

        assertEquals(3, ev.size());
        FixtureEventDto cartonas = ev.get(0);
        assertEquals("Card", cartonas.type());
        assertEquals("Yellow Card", cartonas.detail());
        assertEquals(3L, cartonas.teamId());
        assertEquals(7, cartonas.timeExtra());
        FixtureEventDto gol = ev.get(1);
        assertEquals("Goal", gol.type());
        assertEquals("Penalty", gol.detail());
        assertEquals(17L, gol.teamId());
        assertEquals(1794L, gol.assistId());
        FixtureEventDto schimbare = ev.get(2);
        assertEquals("subst", schimbare.type());
        assertEquals(2471L, schimbare.playerId());
        assertEquals(4539L, schimbare.assistId());
    }

    @Test
    void statisticileDeMeciAuCornereFaulturiCartonasePeEchipa() throws Exception {
        List<FixtureTeamStatsDto> s = BzzoiroMapper.toTeamStats(json("""
                {'stats':{
                  'home':{'corner_kicks':8,'fouls':18,'yellow_cards':1,'red_cards':0,'ball_possession':56,
                          'passes':500,'accurate_passes':400,'expected_goals':3.55,'total_shots':26},
                  'away':{'corner_kicks':3,'fouls':11,'yellow_cards':2,'red_cards':null,'ball_possession':44}},
                 'first_half':{'home':{'corner_kicks':5}}}
                """), 209632L, ECHIPE);

        assertEquals(2, s.size());
        FixtureTeamStatsDto home = s.get(0);
        assertEquals(17L, home.teamId());
        assertEquals(8, home.cornerKicks());
        assertEquals(18, home.fouls());
        assertEquals(0, new BigDecimal("80.00").compareTo(home.passesPercentage()));
        FixtureTeamStatsDto away = s.get(1);
        assertEquals(3L, away.teamId());
        assertEquals(null, away.redCards());
    }

    @Test
    void meciCuPrelungiriAreScorFinalCuPrelungirileSiScorLa90Separat() throws Exception {
        // meci real 210765: 0-2 la 90', 2-1 in prelungiri, 2-3 final
        FixtureDto f = BzzoiroMapper.toFixture(json("""
                {'id':210765,'status':'finished','home_team_id':1,'away_team_id':2,'home_score':0,'away_score':2,
                 'home_score_ht':0,'away_score_ht':0,'extra_time_score':{'home':2,'away':1},'penalty_shootout':null}
                """), 848L, 2026);

        assertEquals("AET", f.statusShort());
        assertEquals(2, f.goalsHome());
        assertEquals(3, f.goalsAway());
        assertEquals(0, f.scoreFtHome());
        assertEquals(2, f.scoreFtAway());
        assertEquals(2, f.scoreEtHome());
    }

    @Test
    void valorileCamelCaseDeLaBzzoiroSuntRecunoscute() throws Exception {
        List<FixtureEventDto> ev = BzzoiroMapper.toEvents(json("""
                {'incidents':[
                  {'type':'card','minute':80,'is_home':true,'card_type':'yellowRed','player_id':5},
                  {'type':'goal','minute':30,'is_home':false,'goal_type':'ownGoal','player_id':6}
                ]}
                """), 1L, ECHIPE);

        assertEquals("Second Yellow card", ev.get(0).detail());
        assertEquals("Own Goal", ev.get(1).detail());
    }

    @Test
    void reprizeleVinDinStatsFirstHalfSiSecondHalf() throws Exception {
        List<FixtureTeamHalfStatsDto> r = BzzoiroMapper.toHalfStats(json("""
                {'event_id':209632,'stats':{
                  'home':{'corner_kicks':8},'away':{'corner_kicks':3},
                  'first_half':{'home':{'corner_kicks':4,'fouls':7,'yellow_cards':0,'total_shots':12},
                                'away':{'corner_kicks':1,'fouls':5,'yellow_cards':1}},
                  'second_half':{'home':{'corner_kicks':4,'fouls':11},
                                 'away':{'corner_kicks':null,'fouls':6}}}}
                """), 209632L, ECHIPE);

        assertEquals(3, r.size());
        FixtureTeamHalfStatsDto gazdeR1 = r.get(0);
        assertEquals(17L, gazdeR1.teamId());
        assertEquals(1, gazdeR1.half());
        assertEquals(4, gazdeR1.cornerKicks());
        assertEquals(7, gazdeR1.fouls());
        assertEquals(12, gazdeR1.shotsTotal());
        FixtureTeamHalfStatsDto oaspetiR1 = r.get(1);
        assertEquals(3L, oaspetiR1.teamId());
        assertEquals(1, oaspetiR1.yellowCards());
        FixtureTeamHalfStatsDto gazdeR2 = r.get(2);
        assertEquals(2, gazdeR2.half());
        assertEquals(17L, gazdeR2.teamId());
    }

    @Test
    void ligaContinentalaEsteCupa() throws Exception {
        assertEquals("Cup", BzzoiroMapper.toLeague(json("{'id':7,'name':'Champions League','country':'Europe'}"), 2L).type());
        assertEquals("League", BzzoiroMapper.toLeague(json("{'id':1,'name':'Premier League','country':'England'}"), 39L).type());
        assertEquals("https://sports.bzzoiro.com/img/league/7/",
                BzzoiroMapper.toLeague(json("{'id':7,'name':'Champions League','country':'Europe'}"), 2L).logo());
    }

    @Test
    void doarFormatiileConfirmateSePastreaza() throws Exception {
        String lineups = """
                {'lineup_status':'%s','lineups':{
                  'home':{'team_id':17,'formation':'4-2-3-1','players':[{'id':1,'short_name':'A','jersey_number':1,'position':'G'}],
                          'substitutes':[{'id':2,'short_name':'B','jersey_number':12,'position':'D'}]},
                  'away':{'team_id':3,'formation':'4-4-2','players':[],'substitutes':[]}}}
                """;

        List<FixtureLineupDto> confirmate = BzzoiroMapper.toLineups(json(lineups.formatted("confirmed")), 5L);
        assertEquals(2, confirmate.size());
        assertEquals("4-2-3-1", confirmate.get(0).formation());
        assertEquals(2, confirmate.get(0).players().size());
        assertTrue(confirmate.get(0).players().get(1).isSubstitute());

        assertTrue(BzzoiroMapper.toLineups(json(lineups.formatted("predicted")), 5L).isEmpty());
    }
}
