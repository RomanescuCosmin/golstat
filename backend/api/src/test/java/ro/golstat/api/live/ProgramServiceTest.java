package ro.golstat.api.live;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ro.golstat.api.entity.Fixture;
import ro.golstat.api.entity.League;
import ro.golstat.api.entity.Team;
import ro.golstat.api.repository.FixtureRepository;
import ro.golstat.api.repository.LeagueRepository;
import ro.golstat.api.repository.TeamRepository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProgramServiceTest {

    @Mock FixtureRepository fixtures;
    @Mock TeamRepository teams;
    @Mock LeagueRepository leagues;
    @InjectMocks ProgramService service;

    private static Fixture fixture(long id, long leagueId, long home, long away, String kickoff) {
        Fixture f = new Fixture();
        f.setId(id);
        f.setLeagueId(leagueId);
        f.setHomeTeamId(home);
        f.setAwayTeamId(away);
        f.setStatusShort("NS");
        f.setKickoff(OffsetDateTime.parse(kickoff));
        return f;
    }

    private static Team team(long id, String nume) {
        Team t = new Team();
        t.setId(id);
        t.setName(nume);
        return t;
    }

    private static League league(long id, String nume, String tara) {
        League l = new League();
        l.setId(id);
        l.setName(nume);
        l.setCountryName(tara);
        return l;
    }

    @Test
    void program_grupeazaPeZiApoiPeLiga_pastrandOrdinea() {
        // sortate cronologic, ca din query: ziua 07 (UCL x2, amicale x1), ziua 08 (UCL x1)
        when(fixtures.findUpcomingAll(any(), any(), any())).thenReturn(List.of(
                fixture(1L, 2L, 10L, 20L, "2026-07-07T19:00:00Z"),
                fixture(2L, 667L, 30L, 40L, "2026-07-07T20:00:00Z"),
                fixture(3L, 2L, 50L, 60L, "2026-07-07T21:00:00Z"),
                fixture(4L, 2L, 10L, 60L, "2026-07-08T19:00:00Z")));
        when(teams.findAllById(any())).thenReturn(List.of(
                team(10L, "Real"), team(20L, "Bayern"), team(30L, "PSG"),
                team(40L, "Milan"), team(50L, "City"), team(60L, "Inter")));
        when(leagues.findAllById(any())).thenReturn(List.of(
                league(2L, "UEFA Champions League", "World"),
                league(667L, "Friendlies Clubs", "World")));

        ProgramDto dto = service.program(7);

        assertEquals(2, dto.zile().size());
        ProgramDto.Zi zi1 = dto.zile().get(0);
        assertEquals(LocalDate.parse("2026-07-07"), zi1.data());
        assertEquals(2, zi1.ligi().size());
        // UCL prima (primul meci intalnit in zi), cu 2 meciuri in ordine de kickoff
        ProgramDto.Liga ucl = zi1.ligi().get(0);
        assertEquals(2L, ucl.leagueId());
        assertEquals("UEFA Champions League", ucl.nume());
        assertEquals(2, ucl.meciuri().size());
        assertEquals(1L, ucl.meciuri().get(0).fixtureId());
        assertEquals("Real", ucl.meciuri().get(0).gazde().nume());
        assertEquals(3L, ucl.meciuri().get(1).fixtureId());
        assertEquals(667L, zi1.ligi().get(1).leagueId());

        ProgramDto.Zi zi2 = dto.zile().get(1);
        assertEquals(LocalDate.parse("2026-07-08"), zi2.data());
        assertEquals(1, zi2.ligi().size());
        assertEquals(4L, zi2.ligi().get(0).meciuri().get(0).fixtureId());
    }

    @Test
    void program_fereastraGoala_returneazaZileGoale() {
        when(fixtures.findUpcomingAll(any(), any(), any())).thenReturn(List.of());
        when(teams.findAllById(any())).thenReturn(List.of());
        when(leagues.findAllById(any())).thenReturn(List.of());

        ProgramDto dto = service.program(7);

        assertTrue(dto.zile().isEmpty());
    }
}
