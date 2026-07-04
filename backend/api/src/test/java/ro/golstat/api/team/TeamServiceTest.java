package ro.golstat.api.team;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import ro.golstat.api.entity.Fixture;
import ro.golstat.api.entity.Player;
import ro.golstat.api.entity.PlayerSeasonStats;
import ro.golstat.api.entity.Standing;
import ro.golstat.api.entity.Team;
import ro.golstat.api.entity.TeamSeasonStats;
import ro.golstat.api.repository.CoachRepository;
import ro.golstat.api.repository.FixtureEventRepository;
import ro.golstat.api.repository.FixtureLineupRepository;
import ro.golstat.api.repository.FixtureRepository;
import ro.golstat.api.repository.FixtureTeamStatsRepository;
import ro.golstat.api.repository.LeagueRepository;
import ro.golstat.api.repository.PlayerRepository;
import ro.golstat.api.repository.PlayerSeasonStatsRepository;
import ro.golstat.api.repository.StandingRepository;
import ro.golstat.api.repository.TeamRepository;
import ro.golstat.api.repository.TeamSeasonStatsRepository;
import ro.golstat.api.repository.VenueRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TeamServiceTest {

    @Mock TeamRepository teams;
    @Mock LeagueRepository leagues;
    @Mock VenueRepository venues;
    @Mock CoachRepository coaches;
    @Mock StandingRepository standings;
    @Mock TeamSeasonStatsRepository teamSeasonStats;
    @Mock PlayerSeasonStatsRepository playerSeasonStats;
    @Mock PlayerRepository players;
    @Mock FixtureRepository fixtures;
    @Mock FixtureTeamStatsRepository teamStats;
    @Mock FixtureEventRepository events;
    @Mock FixtureLineupRepository lineups;
    @InjectMocks TeamService service;

    private static Team team(long id, String nume) {
        Team t = new Team();
        t.setId(id);
        t.setName(nume);
        t.setLogo("http://logo/" + id + ".png");
        t.setCountryName("England");
        return t;
    }

    private static TeamSeasonStats seasonStats(long teamId, long leagueId, int an) {
        TeamSeasonStats s = new TeamSeasonStats();
        s.setTeamId(teamId);
        s.setLeagueId(leagueId);
        s.setSeasonYear(an);
        s.setPlayedTotal(38);
        s.setWinsTotal(28);
        s.setDrawsTotal(5);
        s.setLosesTotal(5);
        s.setGoalsForTotal(94);
        s.setGoalsAgainstTotal(33);
        s.setCleanSheetTotal(18);
        s.setYellowCardsTotal(60);
        s.setRedCardsTotal(2);
        return s;
    }

    @Test
    void pagina_echipaInexistenta_arunca404() {
        when(teams.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EchipaNotFoundException.class, () -> service.pagina(999L, null, null));
    }

    @Test
    void pagina_faraDateDeSezon_degradeazaLaNullDarRaspunde() {
        when(teams.findById(1L)).thenReturn(Optional.of(team(1L, "Man City")));
        when(teamSeasonStats.findByTeamId(1L)).thenReturn(List.of());
        when(fixtures.findRecentForTeam(eq(1L), any(), any(), any())).thenReturn(List.of());
        when(fixtures.findNextForTeam(eq(1L), any(), any(), any())).thenReturn(List.of());
        when(lineups.recentCoachIds(eq(1L), any())).thenReturn(List.of());

        PaginaEchipaDto dto = service.pagina(1L, null, null);

        // antetul exista mereu daca echipa exista
        assertEquals("Man City", dto.antet().nume());
        assertEquals("England", dto.antet().tara());
        assertNull(dto.antet().liga());
        assertNull(dto.antet().antrenor());
        assertNull(dto.antet().stadion());
        // blocuri de sezon lipsa → null/[]
        assertNull(dto.sumar());
        assertEquals(List.of(), dto.forma());
        assertNull(dto.urmatorulMeci());
        assertEquals(List.of(), dto.clasament());
        assertNull(dto.statistici());
        // distributia golurilor: mereu 7 intervale, toate 0
        assertEquals(7, dto.goluriPeInterval().size());
        assertTrue(dto.goluriPeInterval().stream().allMatch(b -> b.goluri() == 0));
        assertEquals("90+", dto.goluriPeInterval().get(6).interval());
        // top jucatori: toate categoriile null
        assertNull(dto.topJucatori().golgheter());
        assertNull(dto.topJucatori().pasator());
    }

    @Test
    void pagina_sezonPrezentClasamentLipsa_sumarFaraPozitieDar200() {
        when(teams.findById(1L)).thenReturn(Optional.of(team(1L, "Man City")));
        when(teamSeasonStats.findByTeamId(1L)).thenReturn(List.of(seasonStats(1L, 39L, 2023)));
        when(teamSeasonStats.findByTeamIdAndLeagueIdAndSeasonYear(1L, 39L, 2023))
                .thenReturn(Optional.of(seasonStats(1L, 39L, 2023)));
        when(standings.findByLeagueIdAndSeasonYearAndTeamId(39L, 2023, 1L)).thenReturn(Optional.empty());
        when(standings.findByLeagueIdAndSeasonYearOrderByRankAsc(39L, 2023)).thenReturn(List.of());
        when(fixtures.findRecentForTeam(eq(1L), any(), any(), any())).thenReturn(List.of());
        when(fixtures.findNextForTeam(eq(1L), any(), any(), any())).thenReturn(List.of());
        when(lineups.recentCoachIds(eq(1L), any())).thenReturn(List.of());
        when(teamStats.findForTeamSeason(anyLong(), anyLong(), anyInt(), any())).thenReturn(List.of());
        when(events.goalMinutes(anyLong(), anyLong(), anyInt(), any(), any())).thenReturn(List.of());
        when(playerSeasonStats.findByTeamIdAndLeagueIdAndSeasonYear(1L, 39L, 2023)).thenReturn(List.of());

        PaginaEchipaDto dto = service.pagina(1L, null, null);

        assertEquals(39L, dto.antet().leagueId());
        assertEquals(2023, dto.antet().sezon());
        PaginaEchipaDto.Sumar sumar = dto.sumar();
        assertNotNull(sumar);
        assertNull(sumar.pozitie());
        assertNull(sumar.puncte());
        assertEquals(38, sumar.jucate());
        assertEquals(28, sumar.victorii());
        assertEquals(94, sumar.goluriMarcate());
        assertEquals(33, sumar.goluriPrimite());
        assertEquals(61, sumar.golaveraj());
        // barele de sezon: goluri/meci + cartonase din season stats; suturi/posesie/pase null (fara meciuri)
        assertNotNull(dto.statistici());
        assertEquals(18, dto.statistici().cleanSheets());
        assertEquals(60, dto.statistici().galbene());
        assertNull(dto.statistici().suturiPeMeci());
    }

    @Test
    void pagina_forma_mapeazaRezultatSiAdversar() {
        when(teams.findById(1L)).thenReturn(Optional.of(team(1L, "Man City")));
        when(teamSeasonStats.findByTeamId(1L)).thenReturn(List.of());
        when(fixtures.findNextForTeam(eq(1L), any(), any(), any())).thenReturn(List.of());
        when(lineups.recentCoachIds(eq(1L), any())).thenReturn(List.of());
        // context: fara season stats → derivat din ultimul meci (leagueId 39, sezon 2023)
        Fixture acasa = fixture(200L, 1L, 20L, 3, 0, 39L, 2023);   // V acasa
        Fixture deplasare = fixture(201L, 30L, 1L, 2, 2, 39L, 2023); // E deplasare
        when(fixtures.findRecentForTeam(eq(1L), any(), any(), any())).thenReturn(List.of(acasa, deplasare));
        when(teams.findAllById(any())).thenReturn(List.of(team(20L, "Adversar A"), team(30L, "Adversar B")));
        when(teamStats.findForTeamSeason(anyLong(), anyLong(), anyInt(), any())).thenReturn(List.of());
        when(events.goalMinutes(anyLong(), anyLong(), anyInt(), any(), any())).thenReturn(List.of());
        when(playerSeasonStats.findByTeamIdAndLeagueIdAndSeasonYear(anyLong(), anyLong(), anyInt())).thenReturn(List.of());
        when(standings.findByLeagueIdAndSeasonYearOrderByRankAsc(anyLong(), anyInt())).thenReturn(List.of());

        PaginaEchipaDto dto = service.pagina(1L, null, null);

        List<PaginaEchipaDto.MeciForma> forma = dto.forma();
        assertEquals(2, forma.size());
        assertTrue(forma.get(0).acasa());
        assertEquals("V", forma.get(0).rezultat());
        assertEquals(3, forma.get(0).golMarcate());
        assertEquals("Adversar A", forma.get(0).adversar().nume());
        assertTrue(!forma.get(1).acasa());
        assertEquals("E", forma.get(1).rezultat());
        assertEquals(2, forma.get(1).golMarcate());
        assertEquals("Adversar B", forma.get(1).adversar().nume());
    }

    @Test
    void pagina_topJucatori_alegeMaximeleSiRezolvaNume() {
        when(teams.findById(1L)).thenReturn(Optional.of(team(1L, "Man City")));
        when(teamSeasonStats.findByTeamId(1L)).thenReturn(List.of(seasonStats(1L, 39L, 2023)));
        when(teamSeasonStats.findByTeamIdAndLeagueIdAndSeasonYear(1L, 39L, 2023))
                .thenReturn(Optional.of(seasonStats(1L, 39L, 2023)));
        when(standings.findByLeagueIdAndSeasonYearAndTeamId(39L, 2023, 1L)).thenReturn(Optional.empty());
        when(standings.findByLeagueIdAndSeasonYearOrderByRankAsc(39L, 2023)).thenReturn(List.of());
        when(fixtures.findRecentForTeam(eq(1L), any(), any(), any())).thenReturn(List.of());
        when(fixtures.findNextForTeam(eq(1L), any(), any(), any())).thenReturn(List.of());
        when(lineups.recentCoachIds(eq(1L), any())).thenReturn(List.of());
        when(teamStats.findForTeamSeason(anyLong(), anyLong(), anyInt(), any())).thenReturn(List.of());
        when(events.goalMinutes(anyLong(), anyLong(), anyInt(), any(), any())).thenReturn(List.of());
        when(playerSeasonStats.findByTeamIdAndLeagueIdAndSeasonYear(1L, 39L, 2023)).thenReturn(List.of(
                playerStats(617L, 36, 8, 3000, 4, 0),
                playerStats(618L, 7, 18, 2800, 2, 1)));
        when(players.findAllById(any())).thenReturn(List.of(
                player(617L, "E. Haaland", "h.png"), player(618L, "K. De Bruyne", "kdb.png")));

        PaginaEchipaDto.TopJucatori top = service.pagina(1L, null, null).topJucatori();

        assertEquals("E. Haaland", top.golgheter().nume());
        assertEquals(36, top.golgheter().valoare());
        assertEquals("h.png", top.golgheter().foto());
        assertEquals("K. De Bruyne", top.pasator().nume());
        assertEquals(18, top.pasator().valoare());
        // cartonase: Haaland 4+0 vs De Bruyne 2+1 → Haaland
        assertEquals(617L, top.cartonase().playerId());
        assertEquals(4, top.cartonase().valoare());
    }

    private static Fixture fixture(long id, long home, long away, int gh, int ga, long leagueId, int an) {
        Fixture f = new Fixture();
        f.setId(id);
        f.setHomeTeamId(home);
        f.setAwayTeamId(away);
        f.setGoalsHome(gh);
        f.setGoalsAway(ga);
        f.setLeagueId(leagueId);
        f.setSeasonYear(an);
        f.setStatusShort("FT");
        f.setKickoff(OffsetDateTime.parse("2023-05-14T18:00:00Z"));
        return f;
    }

    private static PlayerSeasonStats playerStats(long id, int goluri, int pase, int minute, int galbene, int rosii) {
        PlayerSeasonStats s = new PlayerSeasonStats();
        s.setPlayerId(id);
        s.setTeamId(1L);
        s.setLeagueId(39L);
        s.setSeasonYear(2023);
        s.setGoalsTotal(goluri);
        s.setGoalsAssists(pase);
        s.setMinutes(minute);
        s.setCardsYellow(galbene);
        s.setCardsRed(rosii);
        return s;
    }

    private static Player player(long id, String nume, String foto) {
        Player p = new Player();
        p.setId(id);
        p.setName(nume);
        p.setPhoto(foto);
        return p;
    }
}
