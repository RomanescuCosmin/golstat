package ro.golstat.api.competitie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ro.golstat.api.competitie.ClasamentDetaliatDto.Grupa;
import ro.golstat.api.competitie.ClasamentDetaliatDto.MeciForma;
import ro.golstat.api.competitie.ClasamentDetaliatDto.Rand;
import ro.golstat.api.entity.Fixture;
import ro.golstat.api.entity.Standing;
import ro.golstat.api.entity.Team;
import ro.golstat.api.repository.FixtureRepository;
import ro.golstat.api.repository.StandingRepository;
import ro.golstat.api.repository.TeamRepository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClasamentDetaliatServiceTest {

    private static final long LIGA = 5L;
    private static final int SEZON = 2026;

    private final FixtureRepository fixtures = mock(FixtureRepository.class);
    private final StandingRepository standings = mock(StandingRepository.class);
    private final TeamRepository teams = mock(TeamRepository.class);
    private final ClasamentDetaliatService service = new ClasamentDetaliatService(fixtures, standings, teams);

    private final List<Standing> oficial = new ArrayList<>();
    private final List<Fixture> terminate = new ArrayList<>();

    @BeforeEach
    void setUp() {
        when(standings.findByLeagueIdAndSeasonYearOrderByRankAsc(LIGA, SEZON)).thenReturn(oficial);
        when(fixtures.findByLeagueIdAndSeasonYearAndStatusShortInOrderByKickoffDesc(
                eq(LIGA), eq(SEZON), any(), any())).thenReturn(terminate);
        when(teams.findAllById(any())).thenReturn(List.of(
                echipa(1, "Romania"), echipa(2, "Kosovo"), echipa(3, "Cipru"), echipa(4, "Lituania"),
                echipa(5, "Spania"), echipa(6, "Italia")));
    }

    private static Fixture meci(long id, long gazde, long oaspeti, int gg, int go, int zi) {
        Fixture f = new Fixture();
        f.setId(id);
        f.setHomeTeamId(gazde);
        f.setAwayTeamId(oaspeti);
        f.setGoalsHome(gg);
        f.setGoalsAway(go);
        f.setKickoff(OffsetDateTime.of(2026, 10, zi, 15, 0, 0, 0, ZoneOffset.UTC));
        return f;
    }

    private static Fixture meci(long id, long gazde, long oaspeti, int gg, int go, int zi, String runda) {
        Fixture f = meci(id, gazde, oaspeti, gg, go, zi);
        f.setRound(runda);
        return f;
    }

    private static Standing loc(long teamId, int rank, String grupa, int puncte, String zona) {
        Standing s = new Standing();
        s.setLeagueId(LIGA);
        s.setSeasonYear(SEZON);
        s.setTeamId(teamId);
        s.setRank(rank);
        s.setGroupName(grupa);
        s.setPoints(puncte);
        s.setDescription(zona);
        s.setPlayedAll(6);
        s.setWinAll(puncte / 3);
        s.setDrawAll(puncte % 3);
        s.setLoseAll(6 - puncte / 3 - puncte % 3);
        s.setGoalsForAll(10);
        s.setGoalsAgainstAll(4);
        s.setGoalsDiff(6);
        return s;
    }

    private static Team echipa(long id, String nume) {
        Team t = new Team();
        t.setId(id);
        t.setName(nume);
        t.setLogo(nume.toLowerCase() + ".png");
        return t;
    }

    // --- forma (static) ---

    @Test
    void forma_ceaMaiRecentaPrima_dinPerspectivaEchipei() {
        List<Fixture> meciuri = List.of(meci(3, 2, 1, 1, 1, 20), meci(2, 3, 2, 0, 2, 13), meci(1, 1, 3, 3, 0, 6));

        Map<Long, List<MeciForma>> forma = ClasamentDetaliatService.forma(meciuri, Map.of());

        assertEquals(List.of("E", "V"), forma.get(1L).stream().map(MeciForma::rezultat).toList());
        assertEquals(List.of("E", "V"), forma.get(2L).stream().map(MeciForma::rezultat).toList());
        assertEquals(List.of("I", "I"), forma.get(3L).stream().map(MeciForma::rezultat).toList());
        assertEquals(false, forma.get(1L).get(0).acasa());
    }

    @Test
    void forma_limitataLaUltimele5() {
        List<Fixture> meciuri = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            meciuri.add(meci(i, 1, 2, 1, 0, 28 - i));
        }

        assertEquals(ClasamentDetaliatService.MECIURI_FORMA,
                ClasamentDetaliatService.forma(meciuri, Map.of()).get(1L).size());
    }

    @Test
    void forma_scorInOrdineaGazdeOaspeti_siNumeleAdversarului() {
        Map<Long, List<MeciForma>> forma = ClasamentDetaliatService.forma(
                List.of(meci(7, 2, 1, 0, 3, 10)), Map.of(1L, echipa(1, "Romania"), 2L, echipa(2, "Kosovo")));

        MeciForma deplasare = forma.get(1L).get(0);
        assertEquals("V", deplasare.rezultat());
        assertFalse(deplasare.acasa());
        assertEquals("Kosovo", deplasare.adversar());
        assertEquals(0, deplasare.golGazde());
        assertEquals(3, deplasare.golOaspeti());
        assertEquals(LocalDate.of(2026, 10, 10), deplasare.data());
        assertEquals(7L, deplasare.fixtureId());
        assertEquals("I", forma.get(2L).get(0).rezultat());
    }

    @Test
    void forma_meciFaraScorEsteSarit() {
        Fixture faraScor = meci(9, 1, 2, 0, 0, 25);
        faraScor.setGoalsHome(null);

        Map<Long, List<MeciForma>> forma = ClasamentDetaliatService.forma(
                List.of(faraScor, meci(8, 1, 2, 2, 2, 20)), Map.of());

        assertEquals(List.of(8L), forma.get(1L).stream().map(MeciForma::fixtureId).toList());
    }

    // --- clasament ---

    @Test
    void faraClasamentOficial_niciOGrupa() {
        assertTrue(service.clasament(LIGA, SEZON).grupe().isEmpty());
    }

    @Test
    void oSingurăGrupa_faraNume_inOrdineaOficiala_cuToateColoanele() {
        oficial.addAll(List.of(loc(5, 1, null, 15, "Promotion"), loc(6, 2, null, 12, null),
                loc(1, 3, null, 4, "Relegation")));

        List<Grupa> grupe = service.clasament(LIGA, SEZON).grupe();

        assertEquals(1, grupe.size());
        assertNull(grupe.get(0).nume());
        List<Rand> r = grupe.get(0).randuri();
        assertEquals(List.of("Spania", "Italia", "Romania"), r.stream().map(Rand::nume).toList());
        assertEquals(List.of(1, 2, 3), r.stream().map(Rand::pozitie).toList());
        Rand primul = r.get(0);
        assertEquals("Promotion", primul.zona());
        assertEquals(15, primul.puncte());
        assertEquals(6, primul.jucate());
        assertEquals(5, primul.victorii());
        assertEquals(0, primul.egaluri());
        assertEquals(1, primul.infrangeri());
        assertEquals(10, primul.golMarcate());
        assertEquals(4, primul.golPrimite());
        assertEquals(6, primul.golaveraj());
        assertEquals("spania.png", primul.logo());
        assertEquals("Relegation", r.get(2).zona());
    }

    @Test
    void grupe_sortateDupaNume_fiecareInOrdineaLocului() {
        // furnizorul da ordinea pe rang peste toate grupele amestecat
        oficial.addAll(List.of(
                loc(3, 1, "League B, Group 1", 10, "Promotion"),
                loc(1, 1, "League A, Group 1", 12, null),
                loc(4, 2, "League B, Group 1", 7, null),
                loc(2, 2, "League A, Group 1", 9, null)));

        List<Grupa> grupe = service.clasament(LIGA, SEZON).grupe();

        assertEquals(List.of("League A, Group 1", "League B, Group 1"), grupe.stream().map(Grupa::nume).toList());
        assertEquals(List.of("Romania", "Kosovo"), grupe.get(0).randuri().stream().map(Rand::nume).toList());
        assertEquals(List.of("Cipru", "Lituania"), grupe.get(1).randuri().stream().map(Rand::nume).toList());
    }

    @Test
    void forma_doarDinFazaDeClasament_faraPlayoffSauCalificari() {
        oficial.addAll(List.of(loc(1, 1, null, 12, null), loc(2, 2, null, 9, null)));
        terminate.addAll(List.of(
                meci(30, 1, 2, 0, 1, 28, "Play-offs"),
                meci(29, 1, 2, 0, 1, 27, "Final"),
                meci(28, 1, 2, 0, 1, 26, "Qualification Round 2"),
                meci(27, 1, 2, 0, 1, 25, "Round of 16"),
                meci(26, 1, 2, 2, 0, 20, "League Stage - 6"),
                meci(25, 2, 1, 1, 1, 13, null)));

        Rand romania = service.clasament(LIGA, SEZON).grupe().get(0).randuri().get(0);

        assertEquals(List.of(26L, 25L), romania.forma().stream().map(MeciForma::fixtureId).toList());
        assertEquals(List.of("V", "E"), romania.forma().stream().map(MeciForma::rezultat).toList());
    }

    @Test
    void echipaNecunoscuta_numeNull_faraCrash() {
        when(teams.findAllById(any())).thenReturn(List.of());
        oficial.add(loc(99, 1, null, 3, null));

        Rand r = service.clasament(LIGA, SEZON).grupe().get(0).randuri().get(0);

        assertNull(r.nume());
        assertEquals(99L, r.teamId());
        assertTrue(r.forma().isEmpty());
    }

    @Test
    void altaLigaSauSezon_nuAmestecaDatele() {
        when(standings.findByLeagueIdAndSeasonYearOrderByRankAsc(anyLong(), anyInt())).thenReturn(List.of());
        assertTrue(service.clasament(LIGA + 1, SEZON).grupe().isEmpty());
    }
}
