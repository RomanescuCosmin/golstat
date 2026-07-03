package ro.golstat.api.ingest;

import org.junit.jupiter.api.Test;
import ro.golstat.api.entity.Fixture;
import ro.golstat.api.entity.FixtureEvent;
import ro.golstat.api.entity.Standing;
import ro.golstat.api.entity.Team;
import ro.golstat.common.dto.FixtureDto;
import ro.golstat.common.dto.FixtureEventDto;
import ro.golstat.common.dto.StandingDto;
import ro.golstat.common.dto.TeamDto;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EntityMapperTest {

    @Test
    void toFixture_mapsFields() {
        OffsetDateTime kickoff = OffsetDateTime.parse("2024-08-10T18:00:00Z");
        FixtureDto d = new FixtureDto(100L, "Ref", "UTC", kickoff, 1L, 2024, "Etapa", 5L,
                "Match Finished", "FT", 90, 1L, 2L, 2, 1, 1, 0, 2, 1, null, null, null, null);

        Fixture f = EntityMapper.toFixture(d);

        assertEquals(100L, f.getId());
        assertEquals(kickoff, f.getKickoff());
        assertEquals(1L, f.getLeagueId());
        assertEquals("FT", f.getStatusShort());
        assertEquals(1L, f.getHomeTeamId());
        assertEquals(2L, f.getAwayTeamId());
        assertEquals(2, f.getGoalsHome());
        assertEquals(1, f.getScoreHtHome());
        assertEquals(5L, f.getVenueId());
    }

    @Test
    void toTeam_mapsFields() {
        Team t = EntityMapper.toTeam(new TeamDto(7L, "Echipa", "ECH", "Romania", 1900, false, "logo", 3L));
        assertEquals(7L, t.getId());
        assertEquals("Echipa", t.getName());
        assertEquals("ECH", t.getCode());
        assertEquals(false, t.getIsNational());
        assertEquals(3L, t.getVenueId());
    }

    @Test
    void toStanding_mapsKeyAndSplitFields() {
        StandingDto d = new StandingDto(1L, 2024, 9L, 3, "A", 15, 5, "WWL", "same", "desc",
                10, 5, 2, 3, 18, 11, 6, 4, 1, 1, 12, 6, 4, 1, 1, 2, 6, 5);
        Standing s = EntityMapper.toStanding(d);
        assertEquals(1L, s.getLeagueId());
        assertEquals(2024, s.getSeasonYear());
        assertEquals(9L, s.getTeamId());
        assertEquals(3, s.getRank());
        assertEquals(15, s.getPoints());
        assertEquals(12, s.getGoalsForHome());
        assertEquals(5, s.getGoalsAgainstAway());
    }

    @Test
    void toFixtureEvent_doesNotSetSurrogateId() {
        FixtureEvent e = EntityMapper.toFixtureEvent(
                new FixtureEventDto(100L, 1L, 50L, null, 23, null, "Goal", "Normal Goal", null));
        assertNull(e.getId());
        assertEquals(100L, e.getFixtureId());
        assertEquals(1L, e.getTeamId());
        assertEquals(23, e.getTimeElapsed());
        assertEquals("Goal", e.getType());
    }
}
