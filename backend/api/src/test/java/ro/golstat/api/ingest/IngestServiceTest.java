package ro.golstat.api.ingest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ro.golstat.api.entity.Fixture;
import ro.golstat.api.entity.FixtureEvent;
import ro.golstat.api.entity.Standing;
import ro.golstat.api.entity.Team;
import ro.golstat.api.entity.League;
import ro.golstat.api.entity.Season;
import ro.golstat.api.entity.Venue;
import ro.golstat.api.repository.CountryRepository;
import ro.golstat.api.repository.FixtureEventRepository;
import ro.golstat.api.repository.FixtureRepository;
import ro.golstat.api.repository.LeagueRepository;
import ro.golstat.api.repository.SeasonRepository;
import ro.golstat.api.repository.StandingRepository;
import ro.golstat.api.repository.TeamRepository;
import ro.golstat.api.repository.VenueRepository;
import ro.golstat.common.dto.FixtureDto;
import ro.golstat.common.dto.FixtureEventDto;
import ro.golstat.common.dto.SeasonDto;
import ro.golstat.common.dto.StandingDto;
import ro.golstat.common.dto.TeamDto;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IngestServiceTest {

    @Mock TeamRepository teams;
    @Mock FixtureRepository fixtures;
    @Mock FixtureEventRepository events;
    @Mock StandingRepository standings;
    @Mock LeagueRepository leagues;
    @Mock SeasonRepository seasons;
    @Mock VenueRepository venues;
    @Mock CountryRepository countries;
    @InjectMocks IngestService ingest;

    private static FixtureDto fixture(long id, long home, long away) {
        return new FixtureDto(id, "Ref", "UTC", OffsetDateTime.parse("2024-08-10T18:00:00Z"),
                1L, 2024, "Etapa", null, "Match Finished", "FT", 90,
                home, away, 1, 0, 0, 0, 1, 0, null, null, null, null);
    }

    private static FixtureEventDto event(long fixtureId, long teamId) {
        return new FixtureEventDto(fixtureId, teamId, null, null, 10, null, "Goal", "Normal Goal", null);
    }

    private static StandingDto standing(long league, int season, long team) {
        return new StandingDto(league, season, team, 1, null, 10, null, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null, null, null);
    }

    @Test
    void ingestFixture_missingTeams_insertsPlaceholders() {
        when(teams.existsById(anyLong())).thenReturn(false);
        ingest.ingestFixture(fixture(100, 1, 2));
        verify(teams).existsById(1L);
        verify(teams).existsById(2L);
        verify(teams, times(2)).save(any(Team.class)); // doua echipe placeholder
        verify(fixtures).save(any(Fixture.class));
    }

    @Test
    void ingestFixture_existingTeams_noPlaceholder() {
        when(teams.existsById(anyLong())).thenReturn(true);
        ingest.ingestFixture(fixture(100, 1, 2));
        verify(teams, never()).save(any());
        verify(fixtures).save(any(Fixture.class));
    }

    @Test
    void ingestEvents_replacesByFixture() {
        ingest.ingestEvents(List.of(event(100, 1), event(100, 2)));
        verify(events).deleteByFixtureId(100L);
        verify(events, times(2)).save(any(FixtureEvent.class));
    }

    @Test
    void ingestEvents_empty_doesNothing() {
        ingest.ingestEvents(List.of());
        verifyNoInteractions(events);
    }

    @Test
    void ingestStanding_ensuresTeamThenSaves() {
        when(teams.existsById(3L)).thenReturn(false);
        ingest.ingestStanding(standing(1, 2024, 3));
        verify(teams).save(any(Team.class));
        verify(standings).save(any(Standing.class));
    }

    @Test
    void ingestTeam_saves() {
        ingest.ingestTeam(new TeamDto(5L, "Echipa Cinci", null, null, null, false, null, null));
        verify(teams).save(any(Team.class));
    }

    @Test
    void ingestSeason_ensuresLeagueThenSaves() {
        ingest.ingestSeason(new SeasonDto(1L, 2024, null, null, true, null, null, null, null, null));
        verify(leagues).save(any(League.class));   // liga placeholder (nu exista)
        verify(seasons).save(any(Season.class));
    }

    @Test
    void ingestFixture_ensuresVenueAndSeason() {
        FixtureDto withVenue = new FixtureDto(100L, "Ref", "UTC", OffsetDateTime.parse("2024-08-10T18:00:00Z"),
                1L, 2024, "Etapa", 7L, "Match Finished", "FT", 90,
                1L, 2L, 1, 0, 0, 0, 1, 0, null, null, null, null);
        ingest.ingestFixture(withVenue);
        verify(venues).save(any(Venue.class));     // stadion 7 placeholder
        verify(seasons).save(any(Season.class));   // sezon 1/2024 placeholder
        verify(fixtures).save(any(Fixture.class));
    }
}
