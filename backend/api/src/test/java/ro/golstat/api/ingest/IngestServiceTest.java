package ro.golstat.api.ingest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ro.golstat.api.entity.Coach;
import ro.golstat.api.entity.Fixture;
import ro.golstat.api.entity.FixtureEvent;
import ro.golstat.api.entity.FixtureLineup;
import ro.golstat.api.entity.FixtureLineupPlayer;
import ro.golstat.api.entity.FixtureTeamStats;
import ro.golstat.api.entity.Injury;
import ro.golstat.api.entity.Player;
import ro.golstat.api.entity.Standing;
import ro.golstat.api.entity.Team;
import ro.golstat.api.entity.League;
import ro.golstat.api.entity.Season;
import ro.golstat.api.entity.Venue;
import ro.golstat.api.repository.CoachRepository;
import ro.golstat.api.repository.CountryRepository;
import ro.golstat.api.repository.FixtureEventRepository;
import ro.golstat.api.repository.FixtureLineupPlayerRepository;
import ro.golstat.api.repository.FixtureLineupRepository;
import ro.golstat.api.repository.FixtureRepository;
import ro.golstat.api.repository.FixtureTeamStatsRepository;
import ro.golstat.api.repository.InjuryRepository;
import ro.golstat.api.repository.LeagueRepository;
import ro.golstat.api.entity.PlayerSeasonStats;
import ro.golstat.api.entity.TeamSeasonStats;
import ro.golstat.api.repository.PlayerRepository;
import ro.golstat.api.repository.PlayerSeasonStatsRepository;
import ro.golstat.api.repository.SeasonRepository;
import ro.golstat.api.repository.StandingRepository;
import ro.golstat.api.repository.TeamRepository;
import ro.golstat.api.repository.TeamSeasonStatsRepository;
import ro.golstat.api.repository.VenueRepository;
import ro.golstat.common.dto.CoachDto;
import ro.golstat.common.dto.FixtureDto;
import ro.golstat.common.dto.FixtureEventDto;
import ro.golstat.common.dto.FixtureLineupDto;
import ro.golstat.common.dto.FixtureLineupPlayerDto;
import ro.golstat.common.dto.FixtureTeamStatsDto;
import ro.golstat.common.dto.InjuryDto;
import ro.golstat.common.dto.PlayerDto;
import ro.golstat.common.dto.PlayerSeasonStatsDto;
import ro.golstat.common.dto.SeasonDto;
import ro.golstat.common.dto.StandingDto;
import ro.golstat.common.dto.TeamDto;
import ro.golstat.common.dto.TeamSeasonStatsDto;

import java.time.LocalDate;
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
    @Mock FixtureTeamStatsRepository teamStats;
    @Mock StandingRepository standings;
    @Mock LeagueRepository leagues;
    @Mock SeasonRepository seasons;
    @Mock VenueRepository venues;
    @Mock CountryRepository countries;
    @Mock FixtureLineupRepository lineups;
    @Mock FixtureLineupPlayerRepository lineupPlayers;
    @Mock InjuryRepository injuries;
    @Mock PlayerRepository players;
    @Mock CoachRepository coaches;
    @Mock TeamSeasonStatsRepository teamSeasonStats;
    @Mock PlayerSeasonStatsRepository playerSeasonStats;
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
    void ingestFixtureTeamStats_ensuresTeamsThenUpserts() {
        when(teams.existsById(anyLong())).thenReturn(false);
        ingest.ingestFixtureTeamStats(List.of(stats(100, 1), stats(100, 2)));
        verify(teams).existsById(1L);
        verify(teams).existsById(2L);
        verify(teams, times(2)).save(any(Team.class));
        verify(teamStats, times(2)).save(any(FixtureTeamStats.class));
    }

    @Test
    void ingestFixtureTeamStats_empty_doesNothing() {
        ingest.ingestFixtureTeamStats(List.of());
        verifyNoInteractions(teamStats);
        verifyNoInteractions(teams);
    }

    private static FixtureTeamStatsDto stats(long fixtureId, long teamId) {
        return new FixtureTeamStatsDto(fixtureId, teamId, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null);
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

    private static FixtureLineupDto lineup(long fixtureId, long teamId, Long coachId, FixtureLineupPlayerDto... players) {
        return new FixtureLineupDto(fixtureId, teamId, "4-4-2", coachId, List.of(players));
    }

    private static FixtureLineupPlayerDto lineupPlayer(long fixtureId, long teamId, long playerId, boolean substitute) {
        return new FixtureLineupPlayerDto(fixtureId, teamId, playerId, "Jucator " + playerId, 9, "F", "4:1", substitute);
    }

    private static InjuryDto injury(long playerId, Long teamId, Long fixtureId, String type) {
        return new InjuryDto(playerId, "Jucator " + playerId, teamId, fixtureId, 1L, 2026,
                type, "Knee Injury", LocalDate.of(2026, 6, 10));
    }

    @Test
    void ingestFixtureLineups_replacesByFixture_playersAfterLineups() {
        when(teams.existsById(anyLong())).thenReturn(true);
        when(fixtures.existsById(100L)).thenReturn(true);
        when(coaches.existsById(5L)).thenReturn(true);

        ingest.ingestFixtureLineups(List.of(
                lineup(100, 1, 5L, lineupPlayer(100, 1, 10, false), lineupPlayer(100, 1, 11, true)),
                lineup(100, 2, 5L, lineupPlayer(100, 2, 20, false))));

        verify(lineupPlayers).deleteByFixtureId(100L);
        verify(lineups).deleteByFixtureId(100L);
        verify(lineups, times(2)).save(any(FixtureLineup.class));
        verify(lineupPlayers, times(3)).save(any(FixtureLineupPlayer.class));
    }

    @Test
    void ingestFixtureLineups_missingParents_insertsPlaceholders() {
        when(teams.existsById(anyLong())).thenReturn(false);
        when(fixtures.existsById(100L)).thenReturn(false);
        when(coaches.existsById(5L)).thenReturn(false);
        when(seasons.existsById(any())).thenReturn(false);
        when(leagues.existsById(0L)).thenReturn(false);

        ingest.ingestFixtureLineups(List.of(lineup(100, 1, 5L), lineup(100, 2, 5L)));

        verify(teams, times(2)).save(any(Team.class));      // ambele echipe placeholder
        verify(fixtures).save(any(Fixture.class));          // meci placeholder (sezon santinela)
        verify(coaches).save(any(Coach.class));             // antrenorul 5 o singura data
        verify(lineups, times(2)).save(any(FixtureLineup.class));
    }

    @Test
    void ingestFixtureLineups_empty_doesNothing() {
        ingest.ingestFixtureLineups(List.of());
        verifyNoInteractions(lineups);
        verifyNoInteractions(lineupPlayers);
    }

    @Test
    void ingestInjuries_replacesByLeagueSeason_andEnsuresPlayer() {
        when(players.existsById(anyLong())).thenReturn(false);
        when(teams.existsById(anyLong())).thenReturn(true);
        when(fixtures.existsById(anyLong())).thenReturn(true);

        ingest.ingestInjuries(List.of(injury(900, 1L, 100L, "Missing Fixture"),
                injury(901, 2L, 100L, "Questionable")));

        verify(injuries).deleteByLeagueIdAndSeasonYear(1L, 2026);
        verify(players, times(2)).save(any(Player.class));   // placeholder cu nume, pentru afisare
        verify(injuries, times(2)).save(any(Injury.class));
    }

    @Test
    void ingestInjuries_duplicatePlayerFixtureType_savedOnce() {
        when(players.existsById(anyLong())).thenReturn(true);
        when(teams.existsById(anyLong())).thenReturn(true);
        when(fixtures.existsById(anyLong())).thenReturn(true);

        // UNIQUE (player_id, fixture_id, type) in schema → dublura din lot se ignora
        ingest.ingestInjuries(List.of(injury(900, 1L, 100L, "Missing Fixture"),
                injury(900, 1L, 100L, "Missing Fixture")));

        verify(injuries, times(1)).save(any(Injury.class));
    }

    @Test
    void ingestInjuries_missingFixtureWithoutTeam_dropsFixtureFk() {
        when(players.existsById(anyLong())).thenReturn(true);
        when(fixtures.existsById(777L)).thenReturn(false);

        ingest.ingestInjuries(List.of(injury(900, null, 777L, "Missing Fixture")));

        // fara echipa nu se poate crea meci placeholder → FK-ul de meci se lasa null
        verify(fixtures, never()).save(any());
        verify(injuries).save(org.mockito.ArgumentMatchers.argThat(i -> i.getFixtureId() == null));
    }

    @Test
    void ingestInjuries_empty_doesNothing() {
        ingest.ingestInjuries(List.of());
        verifyNoInteractions(injuries);
    }

    @Test
    void ingestPlayers_savesEach() {
        ingest.ingestPlayers(List.of(
                new PlayerDto(617L, "E. Haaland", null, null, null, null, null, null, null, null, null, null, "h.png"),
                new PlayerDto(618L, "K. De Bruyne", null, null, null, null, null, null, null, null, null, null, "kdb.png")));
        verify(players, times(2)).save(any(Player.class));
    }

    @Test
    void ingestCoach_saves() {
        ingest.ingestCoach(new CoachDto(900L, "P. Guardiola", null, null, null, null, null));
        verify(coaches).save(any(Coach.class));
    }

    @Test
    void ingestTeamSeasonStats_ensuresParentsThenSaves() {
        when(teams.existsById(1L)).thenReturn(false);
        ingest.ingestTeamSeasonStats(teamSeasonStats(1L, 39L, 2023));
        verify(teams).save(any(Team.class));       // echipa placeholder
        verify(seasons).save(any(Season.class));   // sezon placeholder
        verify(teamSeasonStats).save(any(TeamSeasonStats.class));
    }

    @Test
    void ingestPlayerSeasonStats_ensuresPlayerThenSaves() {
        when(players.existsById(anyLong())).thenReturn(false);
        when(teams.existsById(anyLong())).thenReturn(true);
        ingest.ingestPlayerSeasonStats(List.of(
                playerSeasonStats(617L, 1L, 39L, 2023), playerSeasonStats(618L, 1L, 39L, 2023)));
        verify(players, times(2)).save(any(Player.class));   // FK dur pe player_id → placeholder
        verify(playerSeasonStats, times(2)).save(any(PlayerSeasonStats.class));
    }

    @Test
    void ingestPlayerSeasonStats_empty_doesNothing() {
        ingest.ingestPlayerSeasonStats(List.of());
        verifyNoInteractions(playerSeasonStats);
    }

    private static TeamSeasonStatsDto teamSeasonStats(long teamId, long leagueId, int season) {
        return new TeamSeasonStatsDto(teamId, leagueId, season, "WWDLW",
                null, null, 38, null, null, 28, null, null, 5, null, null, 5,
                null, null, 94, null, null, java.math.BigDecimal.valueOf(2.47),
                null, null, 33, null, null, null,
                null, null, 18, null, null, null, 60, 2);
    }

    private static PlayerSeasonStatsDto playerSeasonStats(long playerId, long teamId, long leagueId, int season) {
        return new PlayerSeasonStatsDto(playerId, teamId, leagueId, season, "Attacker",
                30, 28, 2500, java.math.BigDecimal.valueOf(7.5), false,
                20, null, 8, null, 60, 30, 800, 40, 85,
                null, null, null, null, null, null, null, null, null, 3, 0, 0,
                null, null, null, null, null);
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
