package ro.golstat.api.stats;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ro.golstat.api.entity.Fixture;
import ro.golstat.api.entity.Standing;
import ro.golstat.api.repository.FixtureRepository;
import ro.golstat.api.repository.StandingRepository;
import ro.golstat.stats.model.MatchSample;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchHistoryServiceTest {

    @Mock FixtureRepository fixtures;
    @Mock StandingRepository standings;
    @InjectMocks MatchHistoryService service;

    private static Fixture fixture(long home, long away, int gh, int ga) {
        Fixture f = new Fixture();
        f.setHomeTeamId(home);
        f.setAwayTeamId(away);
        f.setGoalsHome(gh);
        f.setGoalsAway(ga);
        f.setScoreFtHome(gh);
        f.setScoreFtAway(ga);
        f.setScoreHtHome(0);
        f.setScoreHtAway(0);
        f.setKickoff(OffsetDateTime.parse("2024-08-10T18:00:00Z"));
        f.setLeagueId(1L);
        f.setSeasonYear(2024);
        return f;
    }

    private static Standing rank(int r) {
        Standing s = new Standing();
        s.setRank(r);
        return s;
    }

    @Test
    void mapsFixturesAndLooksUpOpponentRank() {
        when(fixtures.findRecentForTeam(anyLong(), any(), any(), any()))
                .thenReturn(List.of(fixture(1, 2, 2, 1), fixture(3, 1, 0, 3)));
        when(standings.findById(new Standing.Pk(1L, 2024, 2L))).thenReturn(Optional.of(rank(4)));
        when(standings.findById(new Standing.Pk(1L, 2024, 3L))).thenReturn(Optional.of(rank(7)));

        List<MatchSample> history = service.lastMatches(1, OffsetDateTime.parse("2024-09-01T00:00:00Z"), 7);

        assertEquals(2, history.size());
        // meciul 1: echipa 1 gazda vs 2 → rangul adversarului 2 = 4
        assertTrue(history.get(0).home());
        assertEquals(4, history.get(0).opponentRank());
        // meciul 2: echipa 1 oaspete vs 3 → rangul adversarului 3 = 7
        assertFalse(history.get(1).home());
        assertEquals(7, history.get(1).opponentRank());
    }

    @Test
    void missingStanding_opponentRankNull() {
        when(fixtures.findRecentForTeam(anyLong(), any(), any(), any()))
                .thenReturn(List.of(fixture(1, 2, 1, 0)));
        when(standings.findById(any())).thenReturn(Optional.empty());

        List<MatchSample> history = service.lastMatches(1, OffsetDateTime.now(), 7);

        assertNull(history.get(0).opponentRank());
    }
}
