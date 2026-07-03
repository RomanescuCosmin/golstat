package ro.golstat.api.ingest;

import org.junit.jupiter.api.Test;
import ro.golstat.api.entity.Fixture;
import ro.golstat.api.entity.FixtureEvent;
import ro.golstat.api.entity.FixtureLineup;
import ro.golstat.api.entity.FixtureLineupPlayer;
import ro.golstat.api.entity.FixtureTeamStats;
import ro.golstat.api.entity.Injury;
import ro.golstat.api.entity.Season;
import ro.golstat.api.entity.Standing;
import ro.golstat.api.entity.Team;
import ro.golstat.api.entity.Venue;
import ro.golstat.common.dto.FixtureDto;
import ro.golstat.common.dto.FixtureEventDto;
import ro.golstat.common.dto.FixtureLineupDto;
import ro.golstat.common.dto.FixtureLineupPlayerDto;
import ro.golstat.common.dto.FixtureTeamStatsDto;
import ro.golstat.common.dto.InjuryDto;
import ro.golstat.common.dto.SeasonDto;
import ro.golstat.common.dto.StandingDto;
import ro.golstat.common.dto.TeamDto;
import ro.golstat.common.dto.VenueDto;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    void toTeam_nullNational_defaultsToFalse() {
        // API-Football lasa uneori `national` null; coloana is_national e NOT NULL
        Team t = EntityMapper.toTeam(new TeamDto(7L, "Echipa", null, null, null, null, null, null));
        assertEquals(false, t.getIsNational());
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
    void toSeason_mapsCompositeKeyAndFlags() {
        Season s = EntityMapper.toSeason(new SeasonDto(1L, 2024,
                LocalDate.of(2024, 8, 1), LocalDate.of(2025, 5, 31), true, true, false, true, false, true));
        assertEquals(1L, s.getLeagueId());
        assertEquals(2024, s.getYear());
        assertEquals(true, s.getIsCurrent());
        assertEquals(true, s.getHasStandings());
        assertEquals(LocalDate.of(2024, 8, 1), s.getStartDate());
    }

    @Test
    void toVenue_mapsFields() {
        Venue v = EntityMapper.toVenue(new VenueDto(7L, "Stadion", "Adresa", "Oras", "Romania", 30000, "grass", "img"));
        assertEquals(7L, v.getId());
        assertEquals("Stadion", v.getName());
        assertEquals("Oras", v.getCity());
        assertEquals(30000, v.getCapacity());
    }

    @Test
    void toFixtureTeamStats_mapsCompositeKeyAndFields() {
        FixtureTeamStatsDto d = new FixtureTeamStatsDto(215L, 33L, 6, 5, 15, 4, 10, 5,
                11, 7, 2, new BigDecimal("62"), 1, null, 3, 598, 532,
                new BigDecimal("89"), new BigDecimal("1.8"));

        FixtureTeamStats s = EntityMapper.toFixtureTeamStats(d);

        assertEquals(215L, s.getFixtureId());
        assertEquals(33L, s.getTeamId());
        assertEquals(6, s.getShotsOnGoal());
        assertEquals(15, s.getShotsTotal());
        assertEquals(11, s.getFouls());
        assertEquals(7, s.getCornerKicks());
        assertEquals(new BigDecimal("62"), s.getBallPossession());
        assertEquals(1, s.getYellowCards());
        assertNull(s.getRedCards());
        assertEquals(598, s.getPassesTotal());
        assertEquals(new BigDecimal("89"), s.getPassesPercentage());
        assertEquals(new BigDecimal("1.8"), s.getExpectedGoals());
    }

    @Test
    void toFixtureLineup_mapsCompositeKeyAndFields() {
        FixtureLineup l = EntityMapper.toFixtureLineup(
                new FixtureLineupDto(215L, 50L, "4-3-3", 4L, java.util.List.of()));
        assertEquals(215L, l.getFixtureId());
        assertEquals(50L, l.getTeamId());
        assertEquals("4-3-3", l.getFormation());
        assertEquals(4L, l.getCoachId());
    }

    @Test
    void toFixtureLineupPlayer_mapsFields() {
        FixtureLineupPlayer p = EntityMapper.toFixtureLineupPlayer(
                new FixtureLineupPlayerDto(215L, 50L, 617L, "Ederson", 31, "G", "1:1", false));
        assertEquals(215L, p.getFixtureId());
        assertEquals(50L, p.getTeamId());
        assertEquals(617L, p.getPlayerId());
        assertEquals("Ederson", p.getPlayerName());
        assertEquals(31, p.getNumber());
        assertEquals("G", p.getPosition());
        assertEquals("1:1", p.getGrid());
        assertEquals(false, p.getIsSubstitute());
    }

    @Test
    void toFixtureLineupPlayer_nullSubstitute_defaultsToFalse() {
        // is_substitute e NOT NULL in schema
        FixtureLineupPlayer p = EntityMapper.toFixtureLineupPlayer(
                new FixtureLineupPlayerDto(215L, 50L, 617L, "Ederson", null, null, null, null));
        assertEquals(false, p.getIsSubstitute());
    }

    @Test
    void toInjury_doesNotSetSurrogateId() {
        Injury i = EntityMapper.toInjury(new InjuryDto(865L, "D. Costa", 157L, 686314L, 78L, 2026,
                "Missing Fixture", "Broken ankle", LocalDate.of(2026, 7, 7)));
        assertNull(i.getId());
        assertEquals(865L, i.getPlayerId());
        assertEquals(157L, i.getTeamId());
        assertEquals(686314L, i.getFixtureId());
        assertEquals(78L, i.getLeagueId());
        assertEquals(2026, i.getSeasonYear());
        assertEquals("Missing Fixture", i.getType());
        assertEquals("Broken ankle", i.getReason());
        assertEquals(LocalDate.of(2026, 7, 7), i.getReportedAt());
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
