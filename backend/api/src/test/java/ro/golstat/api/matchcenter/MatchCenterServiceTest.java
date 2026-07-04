package ro.golstat.api.matchcenter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ro.golstat.api.entity.Fixture;
import ro.golstat.api.entity.FixtureEvent;
import ro.golstat.api.entity.FixtureTeamStats;
import ro.golstat.api.entity.Player;
import ro.golstat.api.entity.Team;
import ro.golstat.api.repository.FixtureEventRepository;
import ro.golstat.api.repository.FixtureRepository;
import ro.golstat.api.repository.FixtureTeamStatsRepository;
import ro.golstat.api.repository.PlayerRepository;
import ro.golstat.api.repository.TeamRepository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchCenterServiceTest {

    private static final OffsetDateTime KICKOFF = OffsetDateTime.parse("2023-05-14T18:00:00Z");

    @Mock FixtureRepository fixtures;
    @Mock TeamRepository teams;
    @Mock FixtureTeamStatsRepository teamStats;
    @Mock FixtureEventRepository events;
    @Mock PlayerRepository players;
    @InjectMocks MatchCenterService service;

    private static Team team(long id, String nume, String logo) {
        Team t = new Team();
        t.setId(id);
        t.setName(nume);
        t.setLogo(logo);
        return t;
    }

    private static Fixture ftFixture() {
        Fixture f = new Fixture();
        f.setId(100L);
        f.setLeagueId(39L);
        f.setHomeTeamId(10L);
        f.setAwayTeamId(20L);
        f.setGoalsHome(2);
        f.setGoalsAway(1);
        f.setStatusShort("FT");
        f.setStatusLong("Match Finished");
        f.setStatusElapsed(90);
        f.setKickoff(KICKOFF);
        return f;
    }

    private static FixtureTeamStats stats(long teamId, Integer posesie, Integer suturiTotal, Integer pePoarta,
                                          Integer cornere, Integer galbene, Integer precizie, BigDecimal xg) {
        FixtureTeamStats s = new FixtureTeamStats();
        s.setFixtureId(100L);
        s.setTeamId(teamId);
        s.setBallPossession(posesie != null ? BigDecimal.valueOf(posesie) : null);
        s.setShotsTotal(suturiTotal);
        s.setShotsOnGoal(pePoarta);
        s.setCornerKicks(cornere);
        s.setYellowCards(galbene);
        s.setPassesPercentage(precizie != null ? BigDecimal.valueOf(precizie) : null);
        s.setExpectedGoals(xg);
        return s;
    }

    private static FixtureEvent event(long id, Long teamId, Integer min, String tip, String detaliu,
                                      Long playerId, Long assistId) {
        FixtureEvent e = new FixtureEvent();
        e.setId(id);
        e.setFixtureId(100L);
        e.setTeamId(teamId);
        e.setTimeElapsed(min);
        e.setType(tip);
        e.setDetail(detaliu);
        e.setPlayerId(playerId);
        e.setAssistId(assistId);
        return e;
    }

    private static Player player(long id, String nume) {
        Player p = new Player();
        p.setId(id);
        p.setName(nume);
        return p;
    }

    @Test
    void matchCenter_meciFinalizat_mapeazaStatisticiSiCronologie() {
        when(fixtures.findById(100L)).thenReturn(Optional.of(ftFixture()));
        when(teams.findAllById(any())).thenReturn(List.of(
                team(10L, "FC Gazde", "http://logo/10.png"), team(20L, "FC Oaspeti", "http://logo/20.png")));
        when(teamStats.findByFixtureIdIn(List.of(100L))).thenReturn(List.of(
                stats(10L, 55, 14, 6, 7, 2, 84, BigDecimal.valueOf(1.8)),
                stats(20L, 45, 9, 3, 4, 3, 79, null)));
        when(events.findTimeline(100L)).thenReturn(List.of(
                event(1L, 10L, 23, "Goal", "Normal Goal", 617L, 618L),
                event(2L, 20L, 70, "Card", "Yellow Card", 700L, null)));
        when(players.findAllById(any())).thenReturn(List.of(
                player(617L, "E. Haaland"), player(618L, "K. De Bruyne"), player(700L, "Adversar")));

        MeciCentralDto dto = service.matchCenter(100L);

        assertEquals(100L, dto.fixtureId());
        assertEquals(39L, dto.leagueId());
        assertTrue(dto.terminat());
        assertFalse(dto.inDesfasurare());
        assertEquals(2, dto.golGazde());
        assertEquals(1, dto.golOaspeti());
        assertEquals("Match Finished", dto.statusLung());
        assertEquals(90, dto.minut());
        assertEquals("FC Gazde", dto.gazde().nume());

        MeciCentralDto.Echipa gazde = dto.statistici().gazde();
        assertEquals(55, gazde.posesie());
        assertEquals(14, gazde.suturiTotal());
        assertEquals(6, gazde.suturiPePoarta());
        assertEquals(7, gazde.cornere());
        assertEquals(84, gazde.preciziePase());
        assertEquals(1.8, gazde.xg(), 1e-9);
        // xG lipsa pe oaspeti → null-safe
        assertNull(dto.statistici().oaspeti().xg());

        assertEquals(2, dto.evenimente().size());
        EvenimentDto gol = dto.evenimente().get(0);
        assertTrue(gol.gazde());
        assertEquals(23, gol.minut());
        assertEquals("Goal", gol.tip());
        assertEquals("E. Haaland", gol.jucator());
        assertEquals("K. De Bruyne", gol.asist());
        assertFalse(dto.evenimente().get(1).gazde());
    }

    @Test
    void matchCenter_faraStatistici_statisticiNull() {
        when(fixtures.findById(100L)).thenReturn(Optional.of(ftFixture()));
        when(teams.findAllById(any())).thenReturn(List.of());
        when(teamStats.findByFixtureIdIn(List.of(100L))).thenReturn(List.of());
        when(events.findTimeline(100L)).thenReturn(List.of());
        when(players.findAllById(any())).thenReturn(List.of());

        MeciCentralDto dto = service.matchCenter(100L);

        assertNull(dto.statistici());
        assertEquals(List.of(), dto.evenimente());
        // echipe lipsa din DB → doar id
        assertNull(dto.gazde().nume());
        assertEquals(10L, dto.gazde().id());
    }

    @Test
    void matchCenter_meciInexistent_arunca404() {
        when(fixtures.findById(999L)).thenReturn(Optional.empty());

        assertThrows(MeciNotFoundException.class, () -> service.matchCenter(999L));
    }
}
