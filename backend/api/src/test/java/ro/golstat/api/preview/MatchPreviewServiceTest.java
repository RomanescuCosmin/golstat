package ro.golstat.api.preview;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ro.golstat.api.entity.Fixture;
import ro.golstat.api.entity.FixtureLineup;
import ro.golstat.api.entity.FixtureLineupPlayer;
import ro.golstat.api.entity.FixtureTeamStats;
import ro.golstat.api.entity.Injury;
import ro.golstat.api.entity.Player;
import ro.golstat.api.entity.Team;
import ro.golstat.api.prediction.PredictieMeciDto;
import ro.golstat.api.prediction.PredictieMeciDto.EchipaDto;
import ro.golstat.api.prediction.PredictionNotFoundException;
import ro.golstat.api.prediction.PredictionService;
import ro.golstat.api.repository.FixtureLineupPlayerRepository;
import ro.golstat.api.repository.FixtureLineupRepository;
import ro.golstat.api.repository.FixtureRepository;
import ro.golstat.api.repository.InjuryRepository;
import ro.golstat.api.repository.PlayerRepository;
import ro.golstat.api.repository.TeamRepository;
import ro.golstat.api.stats.CountLeagueAverageService;
import ro.golstat.api.stats.CountLeagueAverages;
import ro.golstat.api.stats.IstoricCounturi;
import ro.golstat.api.stats.MatchHistoryService;
import ro.golstat.api.stats.RefereeService;
import ro.golstat.api.stats.StatsHistoryService;
import ro.golstat.stats.cards.RefereeFactor;
import ro.golstat.stats.market.OverUnder;
import ro.golstat.stats.math.NegativeBinomial;
import ro.golstat.stats.math.Poisson;
import ro.golstat.stats.model.EventCountSample;
import ro.golstat.stats.model.MatchSample;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchPreviewServiceTest {

    private static final OffsetDateTime KICKOFF = OffsetDateTime.parse("2025-08-16T14:00:00Z");

    @Mock PredictionService predictions;
    @Mock FixtureRepository fixtures;
    @Mock MatchHistoryService history;
    @Mock StatsHistoryService statsHistory;
    @Mock CountLeagueAverageService countAverages;
    @Mock RefereeService referees;
    @Mock TeamRepository teams;
    @Mock FixtureLineupRepository lineups;
    @Mock FixtureLineupPlayerRepository lineupPlayers;
    @Mock InjuryRepository injuries;
    @Mock PlayerRepository players;
    @InjectMocks MatchPreviewService service;

    private static MatchSample sample(int day, boolean home, int gf, int ga) {
        return new MatchSample(LocalDate.of(2025, 5, day), home, gf, ga, 0, 0, null);
    }

    private static EventCountSample count(int countFor, int countAgainst) {
        return new EventCountSample(LocalDate.of(2025, 5, 1), true, countFor, countAgainst, null);
    }

    private static FixtureTeamStats randStatistici(Integer posesie, Integer suturi, Integer pePoarta,
                                                   Integer cornere, Integer galbene, Integer rosii) {
        FixtureTeamStats s = new FixtureTeamStats();
        s.setBallPossession(posesie != null ? BigDecimal.valueOf(posesie) : null);
        s.setShotsTotal(suturi);
        s.setShotsOnGoal(pePoarta);
        s.setCornerKicks(cornere);
        s.setYellowCards(galbene);
        s.setRedCards(rosii);
        return s;
    }

    private static double overRate(List<OverUnder> linii, double linie) {
        return linii.stream().filter(l -> l.line() == linie).findFirst().orElseThrow().overRate();
    }

    private static Team team(long id, String nume, String logo) {
        Team t = new Team();
        t.setId(id);
        t.setName(nume);
        t.setLogo(logo);
        return t;
    }

    private static Fixture nsFixture() {
        Fixture f = new Fixture();
        f.setId(100L);
        f.setHomeTeamId(10L);
        f.setAwayTeamId(20L);
        f.setKickoff(KICKOFF);
        return f;
    }

    private static Fixture playedFixture(long id, long home, long away, Integer ftHome, Integer ftAway,
                                         Integer goalsHome, Integer goalsAway) {
        Fixture f = new Fixture();
        f.setId(id);
        f.setHomeTeamId(home);
        f.setAwayTeamId(away);
        f.setKickoff(OffsetDateTime.parse("2024-11-0" + (id % 9 + 1) + "T18:00:00Z"));
        f.setScoreFtHome(ftHome);
        f.setScoreFtAway(ftAway);
        f.setGoalsHome(goalsHome);
        f.setGoalsAway(goalsAway);
        return f;
    }

    private static PredictieMeciDto predictie() {
        return new PredictieMeciDto(100L,
                new EchipaDto(10L, "FC Gazde", "http://logo/10.png"),
                new EchipaDto(20L, "FC Oaspeti", "http://logo/20.png"),
                KICKOFF, 1.6, 1.2, null, null, null, List.of(), null, 8, 6);
    }

    private void stubHappyPath() {
        when(predictions.predict(100L)).thenReturn(Optional.of(predictie()));
        when(fixtures.findById(100L)).thenReturn(Optional.of(nsFixture()));
        when(statsHistory.istoric(anyLong(), any(), anyInt())).thenReturn(IstoricCounturi.gol());
        when(countAverages.averages(any(), any())).thenReturn(new CountLeagueAverages(10.0, 24.0, 4.0));
        when(referees.factor(any(), anyDouble())).thenReturn(RefereeFactor.NEUTRAL);
    }

    @Test
    void previzualizare_formaVei_siMediiPeToataFereastra() {
        stubHappyPath();
        // 6 meciuri: primele 5 devin badge-uri, mediile se calculeaza pe toate 6
        when(history.lastMatches(eq(10L), eq(KICKOFF), eq(10))).thenReturn(List.of(
                sample(6, true, 2, 1),   // V
                sample(5, false, 1, 1),  // E
                sample(4, true, 0, 3),   // I
                sample(3, true, 3, 0),   // V
                sample(2, false, 2, 2),  // E
                sample(1, true, 0, 1))); // I (doar in medii)
        when(history.lastMatches(eq(20L), eq(KICKOFF), eq(10))).thenReturn(List.of());
        when(fixtures.findHeadToHead(anyLong(), anyLong(), any(), any(), any())).thenReturn(List.of());
        when(teams.findAllById(any())).thenReturn(List.of());

        PrevizualizareMeciDto dto = service.previzualizare(100L);

        FormaEchipaDto forma = dto.formaGazde();
        assertEquals(5, forma.meciuri().size());
        assertEquals(List.of("V", "E", "I", "V", "E"),
                forma.meciuri().stream().map(FormaMeciDto::rezultat).toList());
        FormaMeciDto primul = forma.meciuri().get(0);
        assertEquals(LocalDate.of(2025, 5, 6), primul.data());
        assertTrue(primul.acasa());
        assertEquals(2, primul.golMarcate());
        assertEquals(1, primul.golPrimite());
        // marcate: (2+1+0+3+2+0)/6 = 1.33; primite: (1+1+3+0+2+1)/6 = 1.33
        assertEquals(1.33, forma.goluriMarcatePeMeci(), 1e-9);
        assertEquals(1.33, forma.goluriPrimitePeMeci(), 1e-9);
        // istoric gol → medii 0, fara badge-uri
        assertEquals(0, dto.formaOaspeti().meciuri().size());
        assertEquals(0.0, dto.formaOaspeti().goluriMarcatePeMeci(), 1e-9);
        assertEquals(predictie(), dto.predictie());
    }

    @Test
    void previzualizare_h2h_mapatCorect_inclusivGazdeInversate() {
        stubHappyPath();
        when(history.lastMatches(anyLong(), any(), anyInt())).thenReturn(List.of());
        // meciul 2: gazdele/oaspetii inversati fata de meciul previzualizat; fara scoreFt → cade pe goals
        when(fixtures.findHeadToHead(eq(10L), eq(20L), any(), eq(KICKOFF), any())).thenReturn(List.of(
                playedFixture(1L, 10L, 20L, 2, 1, 2, 1),
                playedFixture(2L, 20L, 10L, null, null, 3, 0)));
        when(teams.findAllById(List.of(10L, 20L))).thenReturn(List.of(
                team(10L, "FC Gazde", "http://logo/10.png"), team(20L, "FC Oaspeti", "http://logo/20.png")));

        List<IntalnireDirectaDto> h2h = service.previzualizare(100L).intalniriDirecte();

        assertEquals(2, h2h.size());
        IntalnireDirectaDto prima = h2h.get(0);
        assertEquals(1L, prima.fixtureId());
        assertEquals(new EchipaDto(10L, "FC Gazde", "http://logo/10.png"), prima.gazde());
        assertEquals(new EchipaDto(20L, "FC Oaspeti", "http://logo/20.png"), prima.oaspeti());
        assertEquals(2, prima.golGazde());
        assertEquals(1, prima.golOaspeti());
        IntalnireDirectaDto inversata = h2h.get(1);
        assertEquals(new EchipaDto(20L, "FC Oaspeti", "http://logo/20.png"), inversata.gazde());
        assertEquals(new EchipaDto(10L, "FC Gazde", "http://logo/10.png"), inversata.oaspeti());
        assertEquals(3, inversata.golGazde());
        assertEquals(0, inversata.golOaspeti());
    }

    @Test
    void previzualizare_h2hPreferaScorulLa90_nuPrelungirile() {
        stubHappyPath();
        when(history.lastMatches(anyLong(), any(), anyInt())).thenReturn(List.of());
        // meci de cupa terminat 2-2 la 90 min si 3-2 dupa prelungiri (goals include prelungirile)
        when(fixtures.findHeadToHead(anyLong(), anyLong(), any(), any(), any()))
                .thenReturn(List.of(playedFixture(3L, 10L, 20L, 2, 2, 3, 2)));
        when(teams.findAllById(any())).thenReturn(List.of());

        IntalnireDirectaDto dto = service.previzualizare(100L).intalniriDirecte().get(0);

        assertEquals(2, dto.golGazde());
        assertEquals(2, dto.golOaspeti());
        // echipa lipsa din DB → doar id
        assertEquals(new EchipaDto(10L, null, null), dto.gazde());
    }

    @Test
    void previzualizare_faraIntalniriDirecte_listaGoala() {
        stubHappyPath();
        when(history.lastMatches(anyLong(), any(), anyInt())).thenReturn(List.of());
        when(fixtures.findHeadToHead(anyLong(), anyLong(), any(), any(), any())).thenReturn(List.of());
        when(teams.findAllById(any())).thenReturn(List.of());

        assertEquals(List.of(), service.previzualizare(100L).intalniriDirecte());
    }

    @Test
    void previzualizare_fereastraGoala_pietePeMediaLigii_faraCrash() {
        stubHappyPath();
        when(history.lastMatches(anyLong(), any(), anyInt())).thenReturn(List.of());
        when(fixtures.findHeadToHead(anyLong(), anyLong(), any(), any(), any())).thenReturn(List.of());
        when(teams.findAllById(any())).thenReturn(List.of());

        PrevizualizareMeciDto dto = service.previzualizare(100L);

        // n=0 → blend-ul cade integral pe modelul cu media ligii
        assertEquals(List.of(8.5, 9.5, 10.5), dto.cornere().stream().map(OverUnder::line).toList());
        assertEquals(Poisson.probabilityOver(10.0, 8.5), overRate(dto.cornere(), 8.5), 1e-9);
        assertEquals(NegativeBinomial.probabilityOver(24.0, MatchPreviewService.DISPERSIE_FAULTURI, 24.5),
                overRate(dto.faulturi(), 24.5), 1e-9);
        assertEquals(NegativeBinomial.probabilityOver(4.0, MatchPreviewService.DISPERSIE_CARTONASE, 4.5),
                overRate(dto.cartonase(), 4.5), 1e-9);
        for (OverUnder ou : dto.faulturi()) {
            assertEquals(1.0, ou.overRate() + ou.underRate(), 1e-9);
        }
        // fara statistici → toate mediile null ("fara date")
        assertEquals(new StatisticiCheieDto.StatisticiEchipaDto(null, null, null, null, null),
                dto.statisticiCheie().gazde());
    }

    @Test
    void previzualizare_fereastraCunoscuta_blendNumericSiStatisticiCheie() {
        stubHappyPath();
        when(history.lastMatches(anyLong(), any(), anyInt())).thenReturn(List.of());
        when(fixtures.findHeadToHead(anyLong(), anyLong(), any(), any(), any())).thenReturn(List.of());
        when(teams.findAllById(any())).thenReturn(List.of());
        // gazdele: 5 meciuri cu total 9 cornere; oaspetii fara date → fereastra combinata n=5
        when(statsHistory.istoric(eq(10L), eq(KICKOFF), eq(10))).thenReturn(new IstoricCounturi(
                Collections.nCopies(5, count(5, 4)), List.of(), List.of(),
                List.of(randStatistici(55, 12, 5, 6, 2, 0), randStatistici(45, 10, 3, 4, 1, 1))));

        PrevizualizareMeciDto dto = service.previzualizare(100L);

        // w = 5/(5+3); toate totalurile 9 > 8.5 → empiric 1; model Poisson(9)
        double w = 5.0 / 8.0;
        assertEquals(w * 1.0 + (1 - w) * Poisson.probabilityOver(9.0, 8.5),
                overRate(dto.cornere(), 8.5), 1e-9);
        StatisticiCheieDto.StatisticiEchipaDto gazde = dto.statisticiCheie().gazde();
        assertEquals(50.0, gazde.posesieMedie(), 1e-9);
        assertEquals(11.0, gazde.suturiPeMeci(), 1e-9);
        assertEquals(4.0, gazde.suturiPePoarta(), 1e-9);
        assertEquals(5.0, gazde.cornerePeMeci(), 1e-9);
        // cartonase = galbene + rosii: (2 + 2) / 2
        assertEquals(2.0, gazde.cartonasePeMeci(), 1e-9);
        assertEquals(new StatisticiCheieDto.StatisticiEchipaDto(null, null, null, null, null),
                dto.statisticiCheie().oaspeti());
    }

    @Test
    void previzualizare_arbitruDur_intraInProcentulDeCartonase() {
        when(predictions.predict(100L)).thenReturn(Optional.of(predictie()));
        Fixture meci = nsFixture();
        meci.setReferee("M. Oliver");
        when(fixtures.findById(100L)).thenReturn(Optional.of(meci));
        when(statsHistory.istoric(anyLong(), any(), anyInt())).thenReturn(IstoricCounturi.gol());
        when(countAverages.averages(any(), any())).thenReturn(new CountLeagueAverages(10.0, 24.0, 4.0));
        when(referees.factor("M. Oliver", 4.0)).thenReturn(1.3);
        when(history.lastMatches(anyLong(), any(), anyInt())).thenReturn(List.of());
        when(fixtures.findHeadToHead(anyLong(), anyLong(), any(), any(), any())).thenReturn(List.of());
        when(teams.findAllById(any())).thenReturn(List.of());

        PrevizualizareMeciDto dto = service.previzualizare(100L);

        // fereastra goala → media ligii 4.0, apoi factorul real de arbitru 1.3 intra in medie
        assertEquals(NegativeBinomial.probabilityOver(4.0 * 1.3,
                        MatchPreviewService.DISPERSIE_CARTONASE, 4.5),
                overRate(dto.cartonase(), 4.5), 1e-9);
        assertTrue(overRate(dto.cartonase(), 4.5)
                > NegativeBinomial.probabilityOver(4.0, MatchPreviewService.DISPERSIE_CARTONASE, 4.5));
    }

    @Test
    void previzualizare_faraPredictie_arunca404() {
        when(predictions.predict(999L)).thenReturn(Optional.empty());

        assertThrows(PredictionNotFoundException.class, () -> service.previzualizare(999L));
    }

    private static FixtureLineup lineup(long teamId, String formatie, Long coachId) {
        FixtureLineup l = new FixtureLineup();
        l.setFixtureId(100L);
        l.setTeamId(teamId);
        l.setFormation(formatie);
        l.setCoachId(coachId);
        return l;
    }

    private static FixtureLineupPlayer jucator(long teamId, long playerId, String nume, Integer numar,
                                               String pozitie, String grid, boolean rezerva) {
        FixtureLineupPlayer p = new FixtureLineupPlayer();
        p.setFixtureId(100L);
        p.setTeamId(teamId);
        p.setPlayerId(playerId);
        p.setPlayerName(nume);
        p.setNumber(numar);
        p.setPosition(pozitie);
        p.setGrid(grid);
        p.setIsSubstitute(rezerva);
        return p;
    }

    private static Injury injury(long playerId, long teamId, String type, String reason, LocalDate reportedAt) {
        Injury i = new Injury();
        i.setPlayerId(playerId);
        i.setTeamId(teamId);
        i.setLeagueId(39L);
        i.setSeasonYear(2025);
        i.setType(type);
        i.setReason(reason);
        i.setReportedAt(reportedAt);
        return i;
    }

    private static Player player(long id, String nume) {
        Player p = new Player();
        p.setId(id);
        p.setName(nume);
        return p;
    }

    @Test
    void previzualizare_faraLineups_echipaDeStartNull() {
        stubHappyPath();
        when(history.lastMatches(anyLong(), any(), anyInt())).thenReturn(List.of());
        when(fixtures.findHeadToHead(anyLong(), anyLong(), any(), any(), any())).thenReturn(List.of());
        when(teams.findAllById(any())).thenReturn(List.of());

        assertNull(service.previzualizare(100L).echipeDeStart());
    }

    @Test
    void previzualizare_lineupDoarPentruOEchipa_echipaDeStartNull() {
        stubHappyPath();
        when(history.lastMatches(anyLong(), any(), anyInt())).thenReturn(List.of());
        when(fixtures.findHeadToHead(anyLong(), anyLong(), any(), any(), any())).thenReturn(List.of());
        when(teams.findAllById(any())).thenReturn(List.of());
        when(lineups.findByFixtureId(100L)).thenReturn(List.of(lineup(10L, "4-3-3", null)));

        assertNull(service.previzualizare(100L).echipeDeStart());
    }

    @Test
    void previzualizare_cuLineups_compuneEchipaDeStartCuIndisponibiliSiArbitru() {
        when(predictions.predict(100L)).thenReturn(Optional.of(predictie()));
        Fixture meci = nsFixture();
        meci.setReferee("M. Oliver");
        meci.setLeagueId(39L);
        meci.setSeasonYear(2025);
        when(fixtures.findById(100L)).thenReturn(Optional.of(meci));
        when(statsHistory.istoric(anyLong(), any(), anyInt())).thenReturn(IstoricCounturi.gol());
        when(countAverages.averages(any(), any())).thenReturn(new CountLeagueAverages(10.0, 24.0, 4.0));
        when(referees.factor(any(), anyDouble())).thenReturn(RefereeFactor.NEUTRAL);
        when(history.lastMatches(anyLong(), any(), anyInt())).thenReturn(List.of());
        when(fixtures.findHeadToHead(anyLong(), anyLong(), any(), any(), any())).thenReturn(List.of());
        when(teams.findAllById(any())).thenReturn(List.of());

        when(lineups.findByFixtureId(100L)).thenReturn(List.of(
                lineup(10L, "4-3-3", 4L), lineup(20L, "4-4-2", 5L)));
        when(lineupPlayers.findByFixtureId(100L)).thenReturn(List.of(
                jucator(10L, 617L, "Ederson", 31, "G", "1:1", false),
                jucator(10L, 618L, "K. Walker", 2, "D", "2:1", false),
                jucator(10L, 619L, "Rezerva Gazde", 13, "G", null, true),
                jucator(20L, 700L, "Portar Oaspeti", 1, "G", "1:1", false)));
        // 801: doua raportari → o intrare (cea mai recenta); 804: mai veche de 30 de zile → exclusa
        when(injuries.findByLeagueIdAndSeasonYearAndTeamIdIn(eq(39L), eq(2025), any())).thenReturn(List.of(
                injury(801L, 10L, "Missing Fixture", "Knee Injury", LocalDate.of(2025, 8, 10)),
                injury(801L, 10L, "Missing Fixture", "Knee Injury", LocalDate.of(2025, 7, 20)),
                injury(802L, 20L, "Missing Fixture", "Suspended", LocalDate.of(2025, 8, 12)),
                injury(803L, 20L, "Questionable", "Knock", LocalDate.of(2025, 8, 14)),
                injury(804L, 20L, "Missing Fixture", "Old Injury", LocalDate.of(2025, 5, 1))));
        when(players.findAllById(any())).thenReturn(List.of(
                player(801L, "A. Accidentat"), player(802L, "S. Suspendat"), player(803L, "I. Incert")));

        EchipaDeStartDto dto = service.previzualizare(100L).echipeDeStart();

        assertEquals("M. Oliver", dto.arbitru());
        EchipaDeStartDto.EchipaLineupDto gazde = dto.gazde();
        assertEquals("4-3-3", gazde.formatie());
        assertEquals(2, gazde.titulari().size());
        assertEquals(new EchipaDeStartDto.JucatorDto(617L, "Ederson", 31, "G", "1:1"), gazde.titulari().get(0));
        assertEquals(List.of(new EchipaDeStartDto.JucatorDto(619L, "Rezerva Gazde", 13, "G", null)),
                gazde.rezerve());
        assertEquals(List.of(new EchipaDeStartDto.IndisponibilDto(801L, "A. Accidentat", "ACCIDENTAT", "Knee Injury")),
                gazde.indisponibili());
        EchipaDeStartDto.EchipaLineupDto oaspeti = dto.oaspeti();
        assertEquals("4-4-2", oaspeti.formatie());
        assertEquals(1, oaspeti.titulari().size());
        assertEquals(0, oaspeti.rezerve().size());
        // sortati dupa nume: I. Incert inaintea lui S. Suspendat; 804 nu apare
        assertEquals(List.of(
                        new EchipaDeStartDto.IndisponibilDto(803L, "I. Incert", "INCERT", "Knock"),
                        new EchipaDeStartDto.IndisponibilDto(802L, "S. Suspendat", "SUSPENDAT", "Suspended")),
                oaspeti.indisponibili());
    }

    @Test
    void motiv_clasificaTipulSiMotivul() {
        assertEquals("SUSPENDAT", MatchPreviewService.motiv("Missing Fixture", "Suspended"));
        assertEquals("SUSPENDAT", MatchPreviewService.motiv("Missing Fixture", "Red Card"));
        assertEquals("INCERT", MatchPreviewService.motiv("Questionable", "Knock"));
        assertEquals("ACCIDENTAT", MatchPreviewService.motiv("Missing Fixture", "Knee Injury"));
        assertEquals("ACCIDENTAT", MatchPreviewService.motiv(null, null));
    }
}
