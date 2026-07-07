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
import ro.golstat.api.prediction.PredictieMeciDto.ProcentCota;
import ro.golstat.api.prediction.PredictionNotFoundException;
import ro.golstat.api.prediction.PredictionService;
import ro.golstat.api.preview.StatisticiAvansateDto.FrecventaDto;
import ro.golstat.api.repository.FixtureLineupPlayerRepository;
import ro.golstat.api.repository.FixtureLineupRepository;
import ro.golstat.api.repository.FixtureRepository;
import ro.golstat.api.repository.FixtureTeamStatsRepository;
import ro.golstat.api.repository.InjuryRepository;
import ro.golstat.api.repository.PlayerRepository;
import ro.golstat.api.repository.TeamRepository;
import ro.golstat.api.stats.CountLeagueAverageService;
import ro.golstat.api.stats.CountLeagueAverages;
import ro.golstat.api.stats.IstoricCounturi;
import ro.golstat.api.stats.LeagueAverageService;
import ro.golstat.api.stats.LeagueAverages;
import ro.golstat.api.stats.MatchHistoryService;
import ro.golstat.api.stats.RefereeService;
import ro.golstat.api.stats.StatsHistoryService;
import ro.golstat.common.GolstatConstants;
import ro.golstat.stats.cards.RefereeFactor;
import ro.golstat.stats.goals.HalfMarkets;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    private static final int FETCH = MatchPreviewService.HISTORY_FETCH;

    @Mock PredictionService predictions;
    @Mock FixtureRepository fixtures;
    @Mock MatchHistoryService history;
    @Mock StatsHistoryService statsHistory;
    @Mock CountLeagueAverageService countAverages;
    @Mock LeagueAverageService leagueAverages;
    @Mock RefereeService referees;
    @Mock TeamRepository teams;
    @Mock FixtureLineupRepository lineups;
    @Mock FixtureLineupPlayerRepository lineupPlayers;
    @Mock InjuryRepository injuries;
    @Mock PlayerRepository players;
    @Mock FixtureTeamStatsRepository teamStats;
    @InjectMocks MatchPreviewService service;

    private static MatchSample sample(int day, boolean home, int gf, int ga) {
        return sample(day, home, gf, ga, 0, 0);
    }

    private static MatchSample sample(int day, boolean home, int gf, int ga, int gfHt, int gaHt) {
        return new MatchSample(LocalDate.of(2025, 5, day), home, gf, ga, gfHt, gaHt, null);
    }

    private static EventCountSample count(int day, boolean home, int countFor, int countAgainst) {
        return new EventCountSample(LocalDate.of(2025, 5, day), home, countFor, countAgainst, null);
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

    private static double overRate(StatisticiAvansateDto.PiataDto piata, double linie) {
        return piata.linii().stream()
                .filter(l -> l.linie() == linie).findFirst().orElseThrow().probabilitate();
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
        f.setLeagueId(39L);
        f.setSeasonYear(2025);
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
        return predictie(null);
    }

    private static PredictieMeciDto predictie(ProcentCota egal) {
        return new PredictieMeciDto(100L,
                new EchipaDto(10L, "FC Gazde", "http://logo/10.png"),
                new EchipaDto(20L, "FC Oaspeti", "http://logo/20.png"),
                KICKOFF, 1.6, 1.2, null, egal, null, List.of(), null, 8, 6, null);
    }

    private void stubHappyPath() {
        when(predictions.predict(100L)).thenReturn(Optional.of(predictie()));
        when(fixtures.findById(100L)).thenReturn(Optional.of(nsFixture()));
        when(statsHistory.istoric(anyLong(), any(), anyInt())).thenReturn(IstoricCounturi.gol());
        when(countAverages.averages(any(), any())).thenReturn(new CountLeagueAverages(10.0, 24.0, 4.0, 25.0, 9.0));
        when(leagueAverages.averages(anyLong(), anyInt())).thenReturn(new LeagueAverages(1.5, 1.1));
        when(referees.factor(any(), anyDouble())).thenReturn(RefereeFactor.NEUTRAL);
    }

    @Test
    void previzualizare_forma_peLocatieSiGenerala() {
        stubHappyPath();
        // 9 meciuri, 5 acasa + 4 in deplasare: locatia ia cele 5 de acasa, generala ultimele 7
        when(history.lastMatches(eq(10L), eq(KICKOFF), eq(FETCH))).thenReturn(List.of(
                sample(9, true, 2, 1),   // V acasa
                sample(8, false, 0, 2),  // I deplasare
                sample(7, true, 1, 1),   // E acasa
                sample(6, false, 3, 0),  // V deplasare
                sample(5, true, 0, 1),   // I acasa
                sample(4, true, 2, 0),   // V acasa
                sample(3, false, 1, 1),  // E deplasare
                sample(2, true, 1, 0),   // V acasa
                sample(1, false, 0, 0))); // E deplasare (in afara ferestrei generale de 7)
        when(history.lastMatches(eq(20L), eq(KICKOFF), eq(FETCH))).thenReturn(List.of());
        when(fixtures.findHeadToHead(anyLong(), anyLong(), any(), any(), any())).thenReturn(List.of());
        when(teams.findAllById(any())).thenReturn(List.of());

        FormaEchipaDto forma = service.previzualizare(100L).formaGazde();

        assertEquals(List.of("V", "E", "I", "V", "V"),
                forma.locatie().meciuri().stream().map(FormaMeciDto::rezultat).toList());
        assertTrue(forma.locatie().meciuri().stream().allMatch(FormaMeciDto::acasa));
        // acasa: marcate (2+1+0+2+1)/5 = 1.2; primite (1+1+1+0+0)/5 = 0.6
        assertEquals(1.2, forma.locatie().goluriMarcatePeMeci(), 1e-9);
        assertEquals(0.6, forma.locatie().goluriPrimitePeMeci(), 1e-9);

        assertEquals(7, forma.general().meciuri().size());
        assertEquals(LocalDate.of(2025, 5, 9), forma.general().meciuri().get(0).data());
        // general: marcate (2+0+1+3+0+2+1)/7 = 1.29; primite (1+2+1+0+1+0+1)/7 = 0.86
        assertEquals(1.29, forma.general().goluriMarcatePeMeci(), 1e-9);
        assertEquals(0.86, forma.general().goluriPrimitePeMeci(), 1e-9);
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
    void previzualizare_fereastraGoala_pietePeMediaLigii_faraCrash() {
        stubHappyPath();
        when(history.lastMatches(anyLong(), any(), anyInt())).thenReturn(List.of());
        when(fixtures.findHeadToHead(anyLong(), anyLong(), any(), any(), any())).thenReturn(List.of());
        when(teams.findAllById(any())).thenReturn(List.of());

        PrevizualizareMeciDto dto = service.previzualizare(100L);
        StatisticiAvansateDto statistici = dto.statistici();

        // n=0 → blend-ul cade integral pe modelul cu media ligii; liniile cerute de piata
        assertEquals(List.of(7.5, 8.5, 9.5, 10.5),
                statistici.cornere().linii().stream().map(StatisticiAvansateDto.LinieDto::linie).toList());
        assertEquals(Poisson.probabilityOver(10.0, 8.5), overRate(statistici.cornere(), 8.5), 1e-9);
        assertEquals(NegativeBinomial.probabilityOver(24.0, StatisticiAvansateBuilder.DISPERSIE_FAULTURI, 24.5),
                overRate(statistici.faulturi(), 24.5), 1e-9);
        assertEquals(NegativeBinomial.probabilityOver(4.0, StatisticiAvansateBuilder.DISPERSIE_CARTONASE, 4.5),
                overRate(statistici.cartonase(), 4.5), 1e-9);
        // goluri: media ligii 1.5 + 1.1 = 2.6
        assertEquals(List.of(1.5, 2.5, 3.5),
                statistici.goluri().linii().stream().map(StatisticiAvansateDto.LinieDto::linie).toList());
        assertEquals(Poisson.probabilityOver(2.6, 2.5), overRate(statistici.goluri(), 2.5), 1e-9);
        // frecventele goale si mediile null = "fara date"
        StatisticiAvansateDto.LinieDto linie = statistici.cornere().linii().get(0);
        assertEquals(new FrecventaDto(0, 0), linie.gazdeLocatie());
        assertNull(statistici.cornere().gazde().proprieLocatie());
        // fara istoric → sectiunile pe reprize lipsesc
        assertNull(statistici.egaluri());
        assertNull(statistici.reprize());
        // meci viitor (NS) → fara rezultat real
        assertNull(statistici.rezultat());
        // fara statistici → toate mediile null ("fara date")
        assertEquals(new StatisticiCheieDto.StatisticiEchipaDto(null, null, null, null, null),
                dto.statisticiCheie().gazde());
    }

    @Test
    void previzualizare_cornere_blendNumericSiFrecvente() {
        stubHappyPath();
        when(history.lastMatches(anyLong(), any(), anyInt())).thenReturn(List.of());
        when(fixtures.findHeadToHead(anyLong(), anyLong(), any(), any(), any())).thenReturn(List.of());
        when(teams.findAllById(any())).thenReturn(List.of());
        // gazdele: 5 meciuri ACASA cu totaluri 9 cornere; oaspetii fara date → fereastra combinata n=5
        List<EventCountSample> cornere = List.of(
                count(9, true, 5, 4), count(8, true, 5, 4), count(7, true, 5, 4),
                count(6, true, 5, 4), count(5, true, 5, 4));
        // aceleasi 5 meciuri au si suturi: 13 + 11 = 24 pe meci
        List<EventCountSample> suturi = List.of(
                count(9, true, 13, 11), count(8, true, 13, 11), count(7, true, 13, 11),
                count(6, true, 13, 11), count(5, true, 13, 11));
        when(statsHistory.istoric(eq(10L), eq(KICKOFF), eq(FETCH))).thenReturn(new IstoricCounturi(
                cornere, List.of(), List.of(), suturi, List.of(),
                List.of(randStatistici(55, 12, 5, 6, 2, 0), randStatistici(45, 10, 3, 4, 1, 1))));

        PrevizualizareMeciDto dto = service.previzualizare(100L);
        StatisticiAvansateDto.PiataDto piata = dto.statistici().cornere();

        // w = 5/(5+3); toate totalurile 9 > 8.5 → empiric 1; model Poisson(9)
        double w = 5.0 / 8.0;
        assertEquals(w * 1.0 + (1 - w) * Poisson.probabilityOver(9.0, 8.5), overRate(piata, 8.5), 1e-9);
        // frecvente pentru legenda: 9 > 8.5 in 5/5 acasa, 9 < 9.5 → 0/5
        StatisticiAvansateDto.LinieDto peste85 = piata.linii().get(1);
        assertEquals(8.5, peste85.linie());
        assertEquals(new FrecventaDto(5, 5), peste85.gazdeLocatie());
        assertEquals(new FrecventaDto(5, 5), peste85.gazdeGeneral());
        assertEquals(new FrecventaDto(0, 0), peste85.oaspetiLocatie());
        StatisticiAvansateDto.LinieDto peste95 = piata.linii().get(2);
        assertEquals(new FrecventaDto(0, 5), peste95.gazdeLocatie());
        // mediile echipei: proprii 5, total meci 9
        assertEquals(5.0, piata.gazde().proprieLocatie(), 1e-9);
        assertEquals(9.0, piata.gazde().totalLocatie(), 1e-9);
        assertNull(piata.oaspeti().proprieLocatie());
        // suturi: NegBin cu supra-dispersie; totaluri 24 > 22.5 in 5/5, medie proprie 13
        StatisticiAvansateDto.PiataDto piataSuturi = dto.statistici().suturi();
        assertEquals(w * 1.0 + (1 - w) * NegativeBinomial.probabilityOver(24.0,
                        StatisticiAvansateBuilder.DISPERSIE_SUTURI, 22.5),
                overRate(piataSuturi, 22.5), 1e-9);
        assertEquals(new FrecventaDto(5, 5), piataSuturi.linii().get(0).gazdeLocatie());
        assertEquals(13.0, piataSuturi.gazde().proprieLocatie(), 1e-9);
        assertEquals(24.0, piataSuturi.gazde().totalLocatie(), 1e-9);
        // suturi pe poarta fara date → model pe media ligii (9.0)
        assertEquals(Poisson.probabilityOver(9.0, 8.5),
                overRate(dto.statistici().suturiPePoarta(), 8.5), 1e-9);
        // statisticile cheie raman pe randurile proprii
        StatisticiCheieDto.StatisticiEchipaDto gazde = dto.statisticiCheie().gazde();
        assertEquals(50.0, gazde.posesieMedie(), 1e-9);
        assertEquals(11.0, gazde.suturiPeMeci(), 1e-9);
        assertEquals(4.0, gazde.suturiPePoarta(), 1e-9);
        assertEquals(5.0, gazde.cornerePeMeci(), 1e-9);
        // cartonase = galbene + rosii: (2 + 2) / 2
        assertEquals(2.0, gazde.cartonasePeMeci(), 1e-9);
    }

    @Test
    void previzualizare_cornere_fereastraPeLocatie_ignoraMeciurileDinDeplasare() {
        stubHappyPath();
        when(history.lastMatches(anyLong(), any(), anyInt())).thenReturn(List.of());
        when(fixtures.findHeadToHead(anyLong(), anyLong(), any(), any(), any())).thenReturn(List.of());
        when(teams.findAllById(any())).thenReturn(List.of());
        // gazdele: 2 meciuri acasa (total 12) + 2 in deplasare (total 6): locatia vede doar 12-urile
        when(statsHistory.istoric(eq(10L), eq(KICKOFF), eq(FETCH))).thenReturn(new IstoricCounturi(
                List.of(count(9, true, 7, 5), count(8, false, 3, 3),
                        count(7, true, 7, 5), count(6, false, 3, 3)),
                List.of(), List.of(), List.of(), List.of(), List.of()));

        StatisticiAvansateDto.PiataDto piata = service.previzualizare(100L).statistici().cornere();

        StatisticiAvansateDto.LinieDto peste105 = piata.linii().get(3);
        assertEquals(10.5, peste105.linie());
        assertEquals(new FrecventaDto(2, 2), peste105.gazdeLocatie());
        assertEquals(new FrecventaDto(2, 4), peste105.gazdeGeneral());
        // media pe locatie doar din meciurile de acasa
        assertEquals(7.0, piata.gazde().proprieLocatie(), 1e-9);
        assertEquals(12.0, piata.gazde().totalLocatie(), 1e-9);
        assertEquals(5.0, piata.gazde().proprieGeneral(), 1e-9);
        assertEquals(9.0, piata.gazde().totalGeneral(), 1e-9);
    }

    @Test
    void previzualizare_goluriGgEgaluriReprize_numericCunoscut() {
        when(predictions.predict(100L)).thenReturn(Optional.of(predictie(new ProcentCota(30.0, 3.33))));
        when(fixtures.findById(100L)).thenReturn(Optional.of(nsFixture()));
        when(statsHistory.istoric(anyLong(), any(), anyInt())).thenReturn(IstoricCounturi.gol());
        when(countAverages.averages(any(), any())).thenReturn(new CountLeagueAverages(10.0, 24.0, 4.0, 25.0, 9.0));
        when(leagueAverages.averages(anyLong(), anyInt())).thenReturn(new LeagueAverages(1.5, 1.1));
        when(referees.factor(any(), anyDouble())).thenReturn(RefereeFactor.NEUTRAL);
        when(fixtures.findHeadToHead(anyLong(), anyLong(), any(), any(), any())).thenReturn(List.of());
        when(teams.findAllById(any())).thenReturn(List.of());
        // gazdele acasa: 2-1 (pauza 1-0), 1-1 (pauza 0-0); oaspetii in deplasare: 0-0, 2-2 (pauza 1-1)
        List<MatchSample> gazde = List.of(sample(9, true, 2, 1, 1, 0), sample(8, true, 1, 1, 0, 0));
        List<MatchSample> oaspeti = List.of(sample(9, false, 0, 0, 0, 0), sample(8, false, 2, 2, 1, 1));
        when(history.lastMatches(eq(10L), eq(KICKOFF), eq(FETCH))).thenReturn(gazde);
        when(history.lastMatches(eq(20L), eq(KICKOFF), eq(FETCH))).thenReturn(oaspeti);

        StatisticiAvansateDto statistici = service.previzualizare(100L).statistici();

        // GG: marcat/primit pe ferestrele de locatie
        assertEquals(new FrecventaDto(2, 2), statistici.gg().gazdeMarcat());
        assertEquals(new FrecventaDto(2, 2), statistici.gg().gazdePrimit());
        assertEquals(new FrecventaDto(1, 2), statistici.gg().oaspetiMarcat());
        assertEquals(new FrecventaDto(1, 2), statistici.gg().oaspetiPrimit());
        // goluri: totaluri 3,2 / 0,4 → peste 2.5 in 1/2 acasa si 1/2 in deplasare
        StatisticiAvansateDto.LinieDto peste25 = statistici.goluri().linii().get(1);
        assertEquals(new FrecventaDto(1, 2), peste25.gazdeLocatie());
        assertEquals(new FrecventaDto(1, 2), peste25.oaspetiLocatie());
        assertEquals(1.5, statistici.goluri().gazde().proprieLocatie(), 1e-9);
        assertEquals(2.5, statistici.goluri().gazde().totalLocatie(), 1e-9);

        // egal la pauza: modelul HalfMarkets pe aceleasi ferestre
        HalfMarkets.HalfMarketStats reprize = HalfMarkets.of(gazde, oaspeti);
        assertEquals(reprize.htDrawRate(), statistici.egaluri().egalPauza(), 1e-9);
        assertEquals(new FrecventaDto(1, 2), statistici.egaluri().pauzaGazde());
        assertEquals(new FrecventaDto(2, 2), statistici.egaluri().pauzaOaspeti());
        // egal final: empiric 3/4 (1-1, 0-0, 2-2), model 0.30 → w=4/7: 4/7*0.75 + 3/7*0.30
        assertEquals(4.0 / 7.0 * 0.75 + 3.0 / 7.0 * 0.30, statistici.egaluri().egalFinal(), 1e-9);
        assertEquals(new FrecventaDto(1, 2), statistici.egaluri().finalGazde());
        assertEquals(new FrecventaDto(2, 2), statistici.egaluri().finalOaspeti());

        // reprize: ratele modelate + frecventele empirice
        assertEquals(reprize.goalInFirstHalfRate(), statistici.reprize().golRepriza1(), 1e-9);
        assertEquals(reprize.goalInSecondHalfRate(), statistici.reprize().golRepriza2(), 1e-9);
        assertEquals(new FrecventaDto(1, 2), statistici.reprize().repriza1Gazde());
        assertEquals(new FrecventaDto(1, 2), statistici.reprize().repriza1Oaspeti());
        assertEquals(new FrecventaDto(2, 2), statistici.reprize().repriza2Gazde());
        assertEquals(new FrecventaDto(1, 2), statistici.reprize().repriza2Oaspeti());
    }

    @Test
    void previzualizare_arbitruDur_intraInProcentulDeCartonase() {
        when(predictions.predict(100L)).thenReturn(Optional.of(predictie()));
        Fixture meci = nsFixture();
        meci.setReferee("M. Oliver");
        when(fixtures.findById(100L)).thenReturn(Optional.of(meci));
        when(statsHistory.istoric(anyLong(), any(), anyInt())).thenReturn(IstoricCounturi.gol());
        when(countAverages.averages(any(), any())).thenReturn(new CountLeagueAverages(10.0, 24.0, 4.0, 25.0, 9.0));
        when(leagueAverages.averages(anyLong(), anyInt())).thenReturn(new LeagueAverages(1.5, 1.1));
        when(referees.factor("M. Oliver", 4.0)).thenReturn(1.3);
        when(history.lastMatches(anyLong(), any(), anyInt())).thenReturn(List.of());
        when(fixtures.findHeadToHead(anyLong(), anyLong(), any(), any(), any())).thenReturn(List.of());
        when(teams.findAllById(any())).thenReturn(List.of());

        StatisticiAvansateDto.PiataDto cartonase = service.previzualizare(100L).statistici().cartonase();

        // fereastra goala → media ligii 4.0, apoi factorul real de arbitru 1.3 intra in medie
        assertEquals(NegativeBinomial.probabilityOver(4.0 * 1.3,
                        StatisticiAvansateBuilder.DISPERSIE_CARTONASE, 4.5),
                overRate(cartonase, 4.5), 1e-9);
        assertTrue(overRate(cartonase, 4.5)
                > NegativeBinomial.probabilityOver(4.0, StatisticiAvansateBuilder.DISPERSIE_CARTONASE, 4.5));
    }

    @Test
    void previzualizare_faraPredictie_arunca404() {
        when(predictions.predict(999L)).thenReturn(Optional.empty());

        assertThrows(PredictionNotFoundException.class, () -> service.previzualizare(999L));
    }

    @Test
    void previzualizare_meciTerminat_populeazaRezultatRealPePiete() {
        when(predictions.predict(100L)).thenReturn(Optional.of(predictie()));
        Fixture meci = nsFixture();
        meci.setStatusShort(GolstatConstants.FixtureStatus.FINISHED);
        meci.setScoreFtHome(2);
        meci.setScoreFtAway(1);
        meci.setScoreHtHome(1);
        meci.setScoreHtAway(0);
        when(fixtures.findById(100L)).thenReturn(Optional.of(meci));
        when(statsHistory.istoric(anyLong(), any(), anyInt())).thenReturn(IstoricCounturi.gol());
        when(countAverages.averages(any(), any())).thenReturn(new CountLeagueAverages(10.0, 24.0, 4.0, 25.0, 9.0));
        when(leagueAverages.averages(anyLong(), anyInt())).thenReturn(new LeagueAverages(1.5, 1.1));
        when(referees.factor(any(), anyDouble())).thenReturn(RefereeFactor.NEUTRAL);
        when(history.lastMatches(anyLong(), any(), anyInt())).thenReturn(List.of());
        when(fixtures.findHeadToHead(anyLong(), anyLong(), any(), any(), any())).thenReturn(List.of());
        when(teams.findAllById(any())).thenReturn(List.of());
        // statisticile reale ale meciului: gazde 6 cornere / oaspeti 4 → total 10; cartonase 2+0 si 1+1 → 4
        FixtureTeamStats gazde = randStatistici(60, 14, 6, 6, 2, 0);
        gazde.setTeamId(10L);
        gazde.setFixtureId(100L);
        gazde.setFouls(11);
        FixtureTeamStats oaspeti = randStatistici(40, 8, 3, 4, 1, 1);
        oaspeti.setTeamId(20L);
        oaspeti.setFixtureId(100L);
        oaspeti.setFouls(13);
        when(teamStats.findByFixtureIdIn(List.of(100L))).thenReturn(List.of(gazde, oaspeti));

        StatisticiAvansateDto.RezultatDto rezultat = service.previzualizare(100L).statistici().rezultat();

        assertEquals(3, rezultat.totalGoluri());
        assertTrue(rezultat.ambeleMarcheaza());
        assertFalse(rezultat.egalFinal());
        assertFalse(rezultat.egalPauza());
        assertTrue(rezultat.golRepriza1());   // 1-0 la pauza
        assertTrue(rezultat.golRepriza2());   // 2-1 final → gol in repriza 2
        assertEquals(10, rezultat.totalCornere());
        assertEquals(24, rezultat.totalFaulturi());
        assertEquals(4, rezultat.totalCartonase());
        assertEquals(22, rezultat.totalSuturi());
        assertEquals(9, rezultat.totalSuturiPePoarta());
    }

    @Test
    void previzualizare_meciTerminatFaraStatistici_totaluriCountNull() {
        when(predictions.predict(100L)).thenReturn(Optional.of(predictie()));
        Fixture meci = nsFixture();
        meci.setStatusShort(GolstatConstants.FixtureStatus.FINISHED);
        meci.setScoreFtHome(0);
        meci.setScoreFtAway(0);
        meci.setScoreHtHome(0);
        meci.setScoreHtAway(0);
        when(fixtures.findById(100L)).thenReturn(Optional.of(meci));
        when(statsHistory.istoric(anyLong(), any(), anyInt())).thenReturn(IstoricCounturi.gol());
        when(countAverages.averages(any(), any())).thenReturn(new CountLeagueAverages(10.0, 24.0, 4.0, 25.0, 9.0));
        when(leagueAverages.averages(anyLong(), anyInt())).thenReturn(new LeagueAverages(1.5, 1.1));
        when(referees.factor(any(), anyDouble())).thenReturn(RefereeFactor.NEUTRAL);
        when(history.lastMatches(anyLong(), any(), anyInt())).thenReturn(List.of());
        when(fixtures.findHeadToHead(anyLong(), anyLong(), any(), any(), any())).thenReturn(List.of());
        when(teams.findAllById(any())).thenReturn(List.of());
        when(teamStats.findByFixtureIdIn(List.of(100L))).thenReturn(List.of());

        StatisticiAvansateDto.RezultatDto rezultat = service.previzualizare(100L).statistici().rezultat();

        assertEquals(0, rezultat.totalGoluri());
        assertFalse(rezultat.ambeleMarcheaza());
        assertTrue(rezultat.egalFinal());
        assertNull(rezultat.totalCornere());
        assertNull(rezultat.totalCartonase());
    }

    private static FixtureLineup lineup(long teamId, String formatie, Long coachId) {
        return lineupLa(100L, teamId, formatie, coachId);
    }

    private static FixtureLineup lineupLa(long fixtureId, long teamId, String formatie, Long coachId) {
        FixtureLineup l = new FixtureLineup();
        l.setFixtureId(fixtureId);
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
        return player(id, nume, null);
    }

    private static Player player(long id, String nume, String foto) {
        Player p = new Player();
        p.setId(id);
        p.setName(nume);
        p.setPhoto(foto);
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
        when(countAverages.averages(any(), any())).thenReturn(new CountLeagueAverages(10.0, 24.0, 4.0, 25.0, 9.0));
        when(leagueAverages.averages(anyLong(), anyInt())).thenReturn(new LeagueAverages(1.5, 1.1));
        when(referees.factor(any(), anyDouble())).thenReturn(RefereeFactor.NEUTRAL);
        when(history.lastMatches(anyLong(), any(), anyInt())).thenReturn(List.of());
        when(fixtures.findHeadToHead(anyLong(), anyLong(), any(), any(), any())).thenReturn(List.of());
        when(teams.findAllById(any())).thenReturn(List.of());

        when(lineups.findByFixtureId(100L)).thenReturn(List.of(
                lineup(10L, "4-3-3", 4L), lineup(20L, "4-4-2", 5L)));
        when(lineupPlayers.findByFixtureIdAndTeamId(100L, 10L)).thenReturn(List.of(
                jucator(10L, 617L, "Ederson", 31, "G", "1:1", false),
                jucator(10L, 618L, "K. Walker", 2, "D", "2:1", false),
                jucator(10L, 619L, "Rezerva Gazde", 13, "G", null, true)));
        when(lineupPlayers.findByFixtureIdAndTeamId(100L, 20L)).thenReturn(List.of(
                jucator(20L, 700L, "Portar Oaspeti", 1, "G", "1:1", false)));
        // 801: doua raportari → o intrare (cea mai recenta); 804: mai veche de 30 de zile → exclusa
        when(injuries.findByLeagueIdAndSeasonYearAndTeamIdIn(eq(39L), eq(2025), any())).thenReturn(List.of(
                injury(801L, 10L, "Missing Fixture", "Knee Injury", LocalDate.of(2025, 8, 10)),
                injury(801L, 10L, "Missing Fixture", "Knee Injury", LocalDate.of(2025, 7, 20)),
                injury(802L, 20L, "Missing Fixture", "Suspended", LocalDate.of(2025, 8, 12)),
                injury(803L, 20L, "Questionable", "Knock", LocalDate.of(2025, 8, 14)),
                injury(804L, 20L, "Missing Fixture", "Old Injury", LocalDate.of(2025, 5, 1))));
        // acelasi stub serveste si pozele (doar 617 are foto), si numele indisponibililor
        when(players.findAllById(any())).thenReturn(List.of(
                player(617L, "Ederson", "http://foto/617.png"),
                player(801L, "A. Accidentat"), player(802L, "S. Suspendat"), player(803L, "I. Incert")));

        EchipaDeStartDto dto = service.previzualizare(100L).echipeDeStart();

        assertEquals("M. Oliver", dto.arbitru());
        assertFalse(dto.probabila());
        EchipaDeStartDto.EchipaLineupDto gazde = dto.gazde();
        assertEquals("4-3-3", gazde.formatie());
        assertEquals(2, gazde.titulari().size());
        assertEquals(new EchipaDeStartDto.JucatorDto(617L, "Ederson", 31, "G", "1:1",
                "http://foto/617.png"), gazde.titulari().get(0));
        assertEquals(List.of(new EchipaDeStartDto.JucatorDto(619L, "Rezerva Gazde", 13, "G", null, null)),
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
    void previzualizare_faraLineupAnuntat_construiesteEchipaProbabilaDinMeciulAnterior() {
        stubHappyPath();
        when(history.lastMatches(anyLong(), any(), anyInt())).thenReturn(List.of());
        when(fixtures.findHeadToHead(anyLong(), anyLong(), any(), any(), any())).thenReturn(List.of());
        when(teams.findAllById(any())).thenReturn(List.of());

        // fara lineup anuntat pentru meciul 100 → ultimul unsprezece din meciurile 90/91
        when(lineups.findByFixtureId(100L)).thenReturn(List.of());
        when(lineups.findRecentForTeam(eq(10L), any(), eq(KICKOFF), any()))
                .thenReturn(List.of(lineupLa(90L, 10L, "4-3-3", null)));
        when(lineups.findRecentForTeam(eq(20L), any(), eq(KICKOFF), any()))
                .thenReturn(List.of(lineupLa(91L, 20L, "3-5-2", null)));
        when(lineupPlayers.findByFixtureIdAndTeamId(90L, 10L)).thenReturn(List.of(
                jucator(10L, 617L, "Ederson", 31, "G", "1:1", false)));
        when(lineupPlayers.findByFixtureIdAndTeamId(91L, 20L)).thenReturn(List.of(
                jucator(20L, 700L, "Portar Oaspeti", 1, "G", "1:1", false)));
        when(players.findAllById(any())).thenReturn(List.of(
                player(700L, "Portar Oaspeti", "http://foto/700.png")));

        EchipaDeStartDto dto = service.previzualizare(100L).echipeDeStart();

        assertTrue(dto.probabila());
        assertEquals("4-3-3", dto.gazde().formatie());
        assertEquals("3-5-2", dto.oaspeti().formatie());
        assertEquals(new EchipaDeStartDto.JucatorDto(617L, "Ederson", 31, "G", "1:1", null),
                dto.gazde().titulari().get(0));
        assertEquals(new EchipaDeStartDto.JucatorDto(700L, "Portar Oaspeti", 1, "G", "1:1",
                "http://foto/700.png"), dto.oaspeti().titulari().get(0));
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
