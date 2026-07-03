package ro.golstat.api.stats;

import org.junit.jupiter.api.Test;
import ro.golstat.api.entity.Fixture;
import ro.golstat.stats.model.MatchSample;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchSampleMapperTest {

    private static Fixture fixture(long home, long away, Integer gh, Integer ga, Integer htH, Integer htA) {
        Fixture f = new Fixture();
        f.setHomeTeamId(home);
        f.setAwayTeamId(away);
        f.setGoalsHome(gh);
        f.setGoalsAway(ga);
        f.setScoreFtHome(gh);
        f.setScoreFtAway(ga);
        f.setScoreHtHome(htH);
        f.setScoreHtAway(htA);
        f.setKickoff(OffsetDateTime.parse("2024-08-10T18:00:00Z"));
        f.setLeagueId(1L);
        f.setSeasonYear(2024);
        return f;
    }

    @Test
    void homePerspective() {
        // echipa 1 gazda, 2-1 (HT 1-0)
        MatchSample s = MatchSampleMapper.toSample(fixture(1, 2, 2, 1, 1, 0), 1, 5);
        assertTrue(s.home());
        assertEquals(2, s.goalsFor());
        assertEquals(1, s.goalsAgainst());
        assertEquals(1, s.goalsForHt());
        assertEquals(0, s.goalsAgainstHt());
        assertEquals(5, s.opponentRank());
        assertEquals(LocalDate.of(2024, 8, 10), s.date());
    }

    @Test
    void awayPerspective() {
        // echipa 1 oaspete: gazda 3 vs oaspete 1, scor 0-3 (HT 0-1) → pentru echipa 1: 3-0, HT 1-0
        MatchSample s = MatchSampleMapper.toSample(fixture(3, 1, 0, 3, 0, 1), 1, 7);
        assertFalse(s.home());
        assertEquals(3, s.goalsFor());
        assertEquals(0, s.goalsAgainst());
        assertEquals(1, s.goalsForHt());
        assertEquals(0, s.goalsAgainstHt());
    }

    @Test
    void finalScorePreferredOverGoalsWithExtraTime() {
        // goals include prelungirile (4), dar scoreFt (2) e scorul la 90 min
        Fixture f = fixture(1, 2, 4, 0, 1, 0);
        f.setScoreFtHome(2);
        MatchSample s = MatchSampleMapper.toSample(f, 1, null);
        assertEquals(2, s.goalsFor());
    }

    @Test
    void nullHalfTime_becomesZero() {
        MatchSample s = MatchSampleMapper.toSample(fixture(1, 2, 1, 0, null, null), 1, null);
        assertEquals(0, s.goalsForHt());
        assertEquals(0, s.goalsAgainstHt());
    }

    @Test
    void nullFinalScore_fallsBackToGoals() {
        Fixture f = fixture(1, 2, 3, 1, 1, 0);
        f.setScoreFtHome(null);
        f.setScoreFtAway(null);
        MatchSample s = MatchSampleMapper.toSample(f, 1, null);
        assertEquals(3, s.goalsFor());
        assertEquals(1, s.goalsAgainst());
    }
}
