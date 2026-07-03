package ro.golstat.collector.provider;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import ro.golstat.common.GolstatConstants;
import ro.golstat.common.dto.FixtureDto;
import ro.golstat.common.dto.FixtureEventDto;
import ro.golstat.common.dto.LeagueDto;
import ro.golstat.common.dto.SeasonDto;
import ro.golstat.common.dto.StandingDto;
import ro.golstat.common.dto.TeamDto;
import ro.golstat.common.dto.VenueDto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Furnizor FICTIV pentru dezvoltare stub-first: o liga mica (4 echipe, sezon 2024) cu cateva
 * meciuri incheiate, evenimente de gol consistente cu scorul, si clasament. Ne lasa sa
 * construim si sa testam tot lantul de publish fara cheie API / fara sa ardem cota.
 * Activ doar pe profilul {@code stub}.
 */
@Component
@Profile("stub")
public class StubDataProvider implements DataProvider {

    private static final long LEAGUE_ID = 1L;
    private static final int SEASON = 2024;
    private static final long VENUE_ID = 1L;

    private final List<FixtureDto> fixtures = buildFixtures();
    private final List<StandingDto> standings = buildStandings();
    private final List<TeamDto> teams = buildTeams();

    @Override
    public List<FixtureDto> fixtures(long leagueId, int season, LocalDate from, LocalDate to) {
        return fixtures.stream()
                .filter(f -> f.leagueId() == leagueId && f.seasonYear() == season)
                .filter(f -> withinRange(f.kickoff().toLocalDate(), from, to))
                .toList();
    }

    @Override
    public List<FixtureEventDto> fixtureEvents(long fixtureId) {
        return fixtures.stream()
                .filter(f -> f.id() == fixtureId)
                .findFirst()
                .map(StubDataProvider::goalEvents)
                .orElse(List.of());
    }

    @Override
    public List<FixtureDto> liveFixtures() {
        return List.of();   // stub-ul nu simuleaza meciuri live
    }

    @Override
    public List<StandingDto> standings(long leagueId, int season) {
        if (leagueId != LEAGUE_ID || season != SEASON) {
            return List.of();
        }
        return standings;
    }

    @Override
    public List<TeamDto> teams(long leagueId, int season) {
        if (leagueId != LEAGUE_ID || season != SEASON) {
            return List.of();
        }
        return teams;
    }

    @Override
    public List<LeagueDto> leagues() {
        return List.of(new LeagueDto(LEAGUE_ID, "Liga Stub", "League", null, null));
    }

    @Override
    public List<SeasonDto> seasons(long leagueId) {
        if (leagueId != LEAGUE_ID) {
            return List.of();
        }
        return List.of(new SeasonDto(LEAGUE_ID, SEASON,
                LocalDate.of(2024, 8, 1), LocalDate.of(2025, 5, 31),
                true, true, true, true, true, true));
    }

    @Override
    public List<VenueDto> venues() {
        return List.of(new VenueDto(VENUE_ID, "Stadion Stub", null, "Orasul Stub", null, 20000, "grass", null));
    }

    private static boolean withinRange(LocalDate date, LocalDate from, LocalDate to) {
        return !date.isBefore(from) && !date.isAfter(to);
    }

    private static List<FixtureDto> buildFixtures() {
        List<FixtureDto> list = new ArrayList<>();
        list.add(finished(100, LocalDate.of(2024, 8, 10), 1, 2, 2, 1, 1, 0));
        list.add(finished(101, LocalDate.of(2024, 8, 17), 3, 1, 0, 3, 0, 1));
        list.add(finished(102, LocalDate.of(2024, 8, 24), 2, 4, 1, 1, 0, 1));
        list.add(finished(103, LocalDate.of(2024, 8, 31), 4, 3, 2, 2, 1, 1));
        list.add(finished(104, LocalDate.of(2024, 9, 7), 1, 4, 4, 0, 2, 0));
        list.add(finished(105, LocalDate.of(2024, 9, 14), 2, 3, 1, 0, 0, 0));
        return list;
    }

    private static FixtureDto finished(long id, LocalDate day, long home, long away,
                                       int goalsHome, int goalsAway, int htHome, int htAway) {
        OffsetDateTime kickoff = day.atTime(18, 0).atOffset(ZoneOffset.UTC);
        return new FixtureDto(
                id, "Arbitru " + id, "UTC", kickoff, LEAGUE_ID, SEASON, "Etapa", 1L,
                "Match Finished", GolstatConstants.FixtureStatus.FINISHED, 90,
                home, away, goalsHome, goalsAway,
                htHome, htAway, goalsHome, goalsAway,
                null, null, null, null
        );
    }

    /** Un eveniment Goal pentru fiecare gol marcat, ca sa fie consistent cu scorul. */
    private static List<FixtureEventDto> goalEvents(FixtureDto f) {
        List<FixtureEventDto> events = new ArrayList<>();
        for (int i = 0; i < f.goalsHome(); i++) {
            events.add(goal(f.id(), f.homeTeamId(), 10 + i * 20));
        }
        for (int i = 0; i < f.goalsAway(); i++) {
            events.add(goal(f.id(), f.awayTeamId(), 15 + i * 20));
        }
        return events;
    }

    private static FixtureEventDto goal(long fixtureId, long teamId, int minute) {
        return new FixtureEventDto(fixtureId, teamId, null, null, minute, null,
                GolstatConstants.EventType.GOAL, "Normal Goal", null);
    }

    private static List<StandingDto> buildStandings() {
        return List.of(
                standing(1, 1, 12),
                standing(2, 2, 7),
                standing(3, 3, 4),
                standing(4, 4, 2)
        );
    }

    private static List<TeamDto> buildTeams() {
        return List.of(
                team(1, "Echipa Unu"),
                team(2, "Echipa Doi"),
                team(3, "Echipa Trei"),
                team(4, "Echipa Patru")
        );
    }

    /** countryName/venueId null ca sa nu cerem randuri in country/venue (FK-uri). */
    private static TeamDto team(long id, String name) {
        return new TeamDto(id, name, null, null, null, false, null, null);
    }

    private static StandingDto standing(long teamId, int rank, int points) {
        return new StandingDto(
                LEAGUE_ID, SEASON, teamId, rank, null, points, null, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null, null, null
        );
    }
}
