package ro.golstat.collector.provider;

import org.junit.jupiter.api.Test;
import ro.golstat.common.GolstatConstants;
import ro.golstat.common.dto.FixtureDto;
import ro.golstat.common.dto.FixtureEventDto;
import ro.golstat.common.dto.StandingDto;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StubDataProviderTest {

    private final StubDataProvider provider = new StubDataProvider();

    @Test
    void fixtures_wideRange_returnsAllFinished() {
        List<FixtureDto> all = provider.fixtures(1, 2024, LocalDate.of(2024, 8, 1), LocalDate.of(2024, 9, 30));
        assertEquals(6, all.size());
        assertTrue(all.stream().allMatch(f -> GolstatConstants.FixtureStatus.FINISHED.equals(f.statusShort())));
    }

    @Test
    void fixtures_narrowRange_filtersByDate() {
        // 15-31 aug → fixture 101 (17), 102 (24), 103 (31)
        List<FixtureDto> some = provider.fixtures(1, 2024, LocalDate.of(2024, 8, 15), LocalDate.of(2024, 8, 31));
        assertEquals(3, some.size());
        assertTrue(some.stream().allMatch(f -> {
            LocalDate d = f.kickoff().toLocalDate();
            return !d.isBefore(LocalDate.of(2024, 8, 15)) && !d.isAfter(LocalDate.of(2024, 8, 31));
        }));
    }

    @Test
    void fixtures_unknownLeague_isEmpty() {
        assertTrue(provider.fixtures(999, 2024, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31)).isEmpty());
    }

    @Test
    void events_matchTheScore() {
        // fixture 100: 2-1 → 3 goluri (2 gazda, 1 oaspete)
        List<FixtureEventDto> events = provider.fixtureEvents(100);
        assertEquals(3, events.size());
        assertTrue(events.stream().allMatch(e -> GolstatConstants.EventType.GOAL.equals(e.type())));
        assertEquals(2, events.stream().filter(e -> e.teamId() == 1L).count());
        assertEquals(1, events.stream().filter(e -> e.teamId() == 2L).count());
    }

    @Test
    void events_unknownFixture_isEmpty() {
        assertTrue(provider.fixtureEvents(9999).isEmpty());
    }

    @Test
    void standings_fourTeamsRankedOneToFour() {
        List<StandingDto> table = provider.standings(1, 2024);
        assertEquals(4, table.size());
        assertEquals(List.of(1, 2, 3, 4), table.stream().map(StandingDto::rank).toList());
    }

    @Test
    void standings_unknownLeague_isEmpty() {
        assertTrue(provider.standings(999, 2024).isEmpty());
    }

    @Test
    void teams_fourTeams() {
        assertEquals(4, provider.teams(1, 2024).size());
        assertTrue(provider.teams(999, 2024).isEmpty());
    }
}
