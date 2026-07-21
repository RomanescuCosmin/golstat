package ro.golstat.api.piete;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ro.golstat.api.entity.Fixture;
import ro.golstat.api.entity.FixtureTeamStats;
import ro.golstat.api.entity.League;
import ro.golstat.api.entity.Standing;
import ro.golstat.api.entity.Team;
import ro.golstat.api.piete.PieteZileDto.CotaPiataDto;
import ro.golstat.api.piete.PieteZileDto.MeciPieteDto;
import ro.golstat.api.prediction.PredictionService;
import ro.golstat.api.preview.MatchPreviewService;
import ro.golstat.api.preview.PrevizualizareMeciDto;
import ro.golstat.api.preview.StatisticiAvansateDto;
import ro.golstat.api.preview.StatisticiAvansateDto.LinieDto;
import ro.golstat.api.preview.StatisticiAvansateDto.PiataDto;
import ro.golstat.api.repository.FixtureLineupPlayerRepository;
import ro.golstat.api.repository.FixtureEventRepository;
import ro.golstat.api.repository.FixtureLineupRepository;
import ro.golstat.api.repository.InjuryRepository;
import ro.golstat.api.repository.PlayerRepository;
import ro.golstat.api.stats.CountLeagueAverageService;
import ro.golstat.api.stats.FerestreBatchService;
import ro.golstat.api.stats.LeagueAverageService;
import ro.golstat.api.stats.MatchHistoryService;
import ro.golstat.api.stats.RefereeService;
import ro.golstat.api.stats.StatsHistoryService;
import ro.golstat.common.GolstatConstants;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testul central e {@link #aceleasiProcenteCaPaginaDeMeci()}: lista pe zile si pagina cap-la-cap
 * trebuie sa produca EXACT aceleasi probabilitati pe acelasi meci, altfel utilizatorul ar vedea
 * doua numere diferite pentru aceeasi piata. Restul testelor pazesc cazurile fara date si bugetul
 * de query-uri.
 */
class PieteZileServiceTest {

    private static final long LIGA = 39L;
    private static final int SEZON = 2026;
    private static final long GAZDE = 10L;
    private static final long OASPETI = 20L;
    private static final String ARBITRU = "M. Oliver";

    private DateDeTest date;
    private PieteZileService piete;
    private MatchPreviewService preview;
    private OffsetDateTime acum;

    @BeforeEach
    void setUp() {
        acum = OffsetDateTime.now(ZoneOffset.UTC);
        date = new DateDeTest();
        populeaza();
        date.conecteaza();
        piete = piete(date);
        preview = preview(date);
    }

    // --- echivalenta ---

    @Test
    void aceleasiProcenteCaPaginaDeMeci() {
        PieteZileDto rezultat = piete.piete(3);
        MeciPieteDto meci = primulMeci(rezultat);
        PrevizualizareMeciDto perMeci = preview.previzualizare(meci.fixtureId());
        StatisticiAvansateDto s = perMeci.statistici();

        verificaLinii(meci, CodPiata.GOLURI_PESTE, s.goluri(), false);
        verificaLinii(meci, CodPiata.GOLURI_SUB, s.goluri(), true);
        verificaLinii(meci, CodPiata.CORNERE_PESTE, s.cornere(), false);
        verificaLinii(meci, CodPiata.FAULTURI_PESTE, s.faulturi(), false);
        verificaLinii(meci, CodPiata.CARTONASE_PESTE, s.cartonase(), false);

        assertEquals(s.gg().probabilitate(), cota(meci, CodPiata.GG, null).probabilitate(),
                "GG trebuie sa fie identic cu pagina de meci");
        assertEquals(1 - s.gg().probabilitate(), cota(meci, CodPiata.NG, null).probabilitate());
        assertEquals(s.egaluri().egalPauza(), cota(meci, CodPiata.EGAL_PAUZA, null).probabilitate());
        assertEquals(s.egaluri().egalFinal(), cota(meci, CodPiata.EGAL_FINAL, null).probabilitate(),
                "egalul final depinde de P(egal) din modelul de goluri — cel mai fragil punct");
    }

    /** Egalitate EXACTA pe double: orice divergenta de calcul intre cele doua cai pica aici. */
    private void verificaLinii(MeciPieteDto meci, CodPiata cod, PiataDto piata, boolean sub) {
        assertFalse(piata.linii().isEmpty(), "piata " + cod + " ar trebui sa aiba linii");
        for (LinieDto linie : piata.linii()) {
            double asteptat = sub ? 1 - linie.probabilitate() : linie.probabilitate();
            if (asteptat <= 0) {
                continue;
            }
            assertEquals(asteptat, cota(meci, cod, linie.linie()).probabilitate(),
                    cod + " " + linie.linie());
        }
    }

    @Test
    void cotaEsteInversulProbabilitatii() {
        CotaPiataDto cota = cota(primulMeci(piete.piete(3)), CodPiata.GOLURI_PESTE, 2.5);
        assertEquals(Math.round(100.0 / cota.probabilitate()) / 100.0, cota.cota(), 0.011);
    }

    // --- cazuri fara date ---

    @Test
    void fataStatisticiDeMeciRamanDoarPieteleDeGoluri() {
        date.statistici.clear();
        MeciPieteDto meci = primulMeci(piete.piete(3));

        assertTrue(areP(meci, CodPiata.GOLURI_PESTE), "golurile vin din scor, nu din statistici");
        assertTrue(areP(meci, CodPiata.GG));
        assertFalse(areP(meci, CodPiata.CORNERE_PESTE), "fara statistici, cornerele n-au esantion");
        assertFalse(areP(meci, CodPiata.FAULTURI_PESTE));
        assertFalse(areP(meci, CodPiata.CARTONASE_PESTE));
    }

    @Test
    void echipaFaraIstoricNuProduceMeciul() {
        date.meciuri.removeIf(f -> GolstatConstants.FixtureStatus.TERMINAL.contains(f.getStatusShort()));
        assertTrue(piete.piete(3).zile().isEmpty(), "fara niciun meci terminal nu exista esantion");
    }

    @Test
    void nicioProbabilitateNuEsteZeroSauNefinita() {
        for (var zi : piete.piete(3).zile()) {
            for (MeciPieteDto meci : zi.meciuri()) {
                for (CotaPiataDto cota : meci.piete()) {
                    assertTrue(Double.isFinite(cota.probabilitate()) && cota.probabilitate() > 0,
                            cota.piata() + " are probabilitate invalida: " + cota.probabilitate());
                    assertTrue(Double.isFinite(cota.cota()), cota.piata() + " are cota invalida");
                    assertTrue(cota.esantion() > 0, cota.piata() + " nu ar trebui trimisa cu esantion 0");
                }
            }
        }
    }

    @Test
    void meciurileAnulateSauTerminateNuIntraInLista() {
        Fixture anulat = meci(900L, GAZDE, OASPETI, acum.plusDays(1), GolstatConstants.FixtureStatus.CANCELLED);
        date.meciuri.add(anulat);
        assertTrue(piete.piete(3).zile().stream()
                .flatMap(z -> z.meciuri().stream())
                .noneMatch(m -> m.fixtureId() == 900L));
    }

    @Test
    void meciurileFaraLigaSauSezonSuntSarite() {
        Fixture stricat = meci(901L, GAZDE, OASPETI, acum.plusDays(1), GolstatConstants.FixtureStatus.NOT_STARTED);
        stricat.setLeagueId(null);
        date.meciuri.add(stricat);

        PieteZileDto rezultat = piete.piete(3);
        assertTrue(rezultat.zile().stream().flatMap(z -> z.meciuri().stream())
                .noneMatch(m -> m.fixtureId() == 901L));
        assertFalse(rezultat.zile().isEmpty(), "restul listei ramane intacta");
    }

    @Test
    void arbitrulNecunoscutNuStricaCartonasele() {
        date.meciuri.stream()
                .filter(f -> GolstatConstants.FixtureStatus.NOT_STARTED.equals(f.getStatusShort()))
                .forEach(f -> f.setReferee("Arbitru Inexistent"));
        MeciPieteDto meci = primulMeci(piete.piete(3));
        assertTrue(areP(meci, CodPiata.CARTONASE_PESTE), "cade pe factorul neutru, nu dispare");
    }

    @Test
    void grupeazaPeZiCronologic() {
        List<PieteZileDto.ZiDto> zile = piete.piete(3).zile();
        assertTrue(zile.size() >= 2, "avem meciuri in doua zile diferite");
        for (int i = 1; i < zile.size(); i++) {
            assertTrue(zile.get(i - 1).data().isBefore(zile.get(i).data()), "zilele sunt cronologice");
        }
    }

    @Test
    void fereastraAcoperaExactZileCalendaristice() {
        // a 4-a zi: in afara ferestrei chiar daca e la mai putin de 3×24h distanta de "acum"
        Fixture prea_departe = meci(902L, GAZDE, OASPETI,
                acum.toLocalDate().plusDays(3).atStartOfDay().atOffset(ZoneOffset.UTC).plusHours(12),
                GolstatConstants.FixtureStatus.NOT_STARTED);
        date.meciuri.add(prea_departe);

        PieteZileDto rezultat = piete.piete(3);
        assertTrue(rezultat.zile().size() <= 3, "cel mult 3 zile calendaristice, au fost "
                + rezultat.zile().size());
        assertTrue(rezultat.zile().stream().flatMap(z -> z.meciuri().stream())
                .noneMatch(m -> m.fixtureId() == 902L), "meciul din a 4-a zi nu intra");
    }

    @Test
    void meciurileDeAziCareAuInceputDejaNuApar() {
        Fixture trecut = meci(903L, GAZDE, OASPETI, acum.minusHours(2),
                GolstatConstants.FixtureStatus.NOT_STARTED);
        date.meciuri.add(trecut);
        assertTrue(piete.piete(3).zile().stream().flatMap(z -> z.meciuri().stream())
                .noneMatch(m -> m.fixtureId() == 903L));
    }

    // --- bugetul de query-uri (paznicul anti N+1) ---

    @Test
    void numarulDeQueryuriNuCresteCuNumarulDeMeciuri() {
        date.apeluri.set(0);
        piete.piete(3);
        int cuPutineMeciuri = date.apeluri.get();

        for (int i = 0; i < 40; i++) {
            date.meciuri.add(meci(1000 + i, GAZDE, OASPETI, acum.plusDays(1).plusMinutes(i),
                    GolstatConstants.FixtureStatus.NOT_STARTED));
        }
        date.apeluri.set(0);
        piete.piete(3);
        int cuMulteMeciuri = date.apeluri.get();

        assertEquals(cuPutineMeciuri, cuMulteMeciuri,
                "de 20 de ori mai multe meciuri nu trebuie sa adauge niciun query");
        assertTrue(cuMulteMeciuri < 20, "bugetul total ramane mic, a fost " + cuMulteMeciuri);
    }

    // --- constructie ---

    private void populeaza() {
        date.ligi.add(liga());
        date.echipe.add(echipa(GAZDE, "Gazde FC"));
        date.echipe.add(echipa(OASPETI, "Oaspeti United"));
        date.clasament.add(clasament(GAZDE, 3));
        date.clasament.add(clasament(OASPETI, 12));

        long id = 1;
        // istoric: fiecare echipa joaca si acasa si in deplasare, ca ambele ferestre de locatie
        // sa aiba esantion (meciul retur inverseaza rolurile)
        for (int i = 0; i < 10; i++) {
            long adversar = 100 + i;
            date.echipe.add(echipa(adversar, "Adversar " + i));
            date.clasament.add(clasament(adversar, 5 + i));

            adaugaIstoric(id++, GAZDE, adversar, acum.minusDays(80L - i * 3), i, true);
            adaugaIstoric(id++, adversar, GAZDE, acum.minusDays(79L - i * 3), i, false);
            adaugaIstoric(id++, OASPETI, adversar, acum.minusDays(78L - i * 3), i, false);
            adaugaIstoric(id++, adversar, OASPETI, acum.minusDays(77L - i * 3), i, true);
        }

        Fixture maine = meci(500L, GAZDE, OASPETI, acum.plusDays(1), GolstatConstants.FixtureStatus.NOT_STARTED);
        Fixture poimaine = meci(501L, OASPETI, GAZDE, acum.plusDays(2), GolstatConstants.FixtureStatus.NOT_STARTED);
        date.meciuri.add(maine);
        date.meciuri.add(poimaine);
    }

    /** Un meci terminat cu scor si statistici deterministe; {@code variatie} sparge monotonia. */
    private void adaugaIstoric(long id, long gazde, long oaspeti, OffsetDateTime cand,
                               int i, boolean variatie) {
        Fixture f = meci(id, gazde, oaspeti, cand, GolstatConstants.FixtureStatus.FINISHED);
        scor(f, variatie ? 2 + i % 2 : 1 + i % 3, variatie ? i % 3 : 1 + i % 2,
                variatie ? 1 : 0, i % 2);
        date.meciuri.add(f);
        statistici(f, 5 + i % 4, 11 + i % 4, 1 + i % 3, 1 + i % 3);
    }

    private static Fixture meci(long id, long gazde, long oaspeti, OffsetDateTime kickoff, String status) {
        Fixture f = new Fixture();
        f.setId(id);
        f.setHomeTeamId(gazde);
        f.setAwayTeamId(oaspeti);
        f.setKickoff(kickoff);
        f.setStatusShort(status);
        f.setLeagueId(LIGA);
        f.setSeasonYear(SEZON);
        f.setReferee(ARBITRU);
        return f;
    }

    private static void scor(Fixture f, int gazde, int oaspeti, int htGazde, int htOaspeti) {
        f.setGoalsHome(gazde);
        f.setGoalsAway(oaspeti);
        f.setScoreFtHome(gazde);
        f.setScoreFtAway(oaspeti);
        f.setScoreHtHome(htGazde);
        f.setScoreHtAway(htOaspeti);
    }

    private void statistici(Fixture f, int cornere, int faulturi, int galbene, int suturi) {
        date.statistici.add(stat(f.getId(), f.getHomeTeamId(), cornere, faulturi, galbene, suturi));
        date.statistici.add(stat(f.getId(), f.getAwayTeamId(), cornere + 1, faulturi + 2, galbene, suturi + 1));
    }

    private static FixtureTeamStats stat(long fixtureId, long teamId, int cornere, int faulturi,
                                         int galbene, int suturi) {
        FixtureTeamStats s = new FixtureTeamStats();
        s.setFixtureId(fixtureId);
        s.setTeamId(teamId);
        s.setCornerKicks(cornere);
        s.setFouls(faulturi);
        s.setYellowCards(galbene);
        s.setRedCards(0);
        s.setShotsTotal(suturi + 8);
        s.setShotsOnGoal(suturi);
        return s;
    }

    private static Team echipa(long id, String nume) {
        Team t = new Team();
        t.setId(id);
        t.setName(nume);
        return t;
    }

    private static League liga() {
        League l = new League();
        l.setId(LIGA);
        l.setName("Premier League");
        return l;
    }

    private static Standing clasament(long teamId, int rang) {
        Standing s = new Standing();
        s.setLeagueId(LIGA);
        s.setSeasonYear(SEZON);
        s.setTeamId(teamId);
        s.setRank(rang);
        return s;
    }

    private static PieteZileService piete(DateDeTest date) {
        return new PieteZileService(date.fixtures, date.teams, date.leagues, date.teamStats,
                new FerestreBatchService(date.fixtures, date.teamStats, date.standings),
                new LeagueAverageService(date.fixtures), new CountLeagueAverageService(date.teamStats));
    }

    private static MatchPreviewService preview(DateDeTest date) {
        MatchHistoryService history = new MatchHistoryService(date.fixtures, date.standings);
        FixtureLineupRepository lineups = mock(FixtureLineupRepository.class);
        FixtureLineupPlayerRepository lineupPlayers = mock(FixtureLineupPlayerRepository.class);
        InjuryRepository injuries = mock(InjuryRepository.class);
        PlayerRepository players = mock(PlayerRepository.class);
        when(lineups.findByFixtureId(anyLong())).thenReturn(List.of());
        when(lineups.findRecentForTeam(anyLong(), any(), any(), any())).thenReturn(List.of());
        when(injuries.findByLeagueIdAndSeasonYearAndTeamIdIn(any(), any(), any())).thenReturn(List.of());
        when(players.findAllById(any())).thenReturn(List.of());

        return new MatchPreviewService(
                new PredictionService(date.fixtures, date.teams, history, new LeagueAverageService(date.fixtures)),
                date.fixtures, history, new StatsHistoryService(date.fixtures, date.teamStats),
                new CountLeagueAverageService(date.teamStats), new LeagueAverageService(date.fixtures),
                new RefereeService(date.teamStats), date.teams,
                lineups, lineupPlayers, injuries, players, date.teamStats,
                mock(FixtureEventRepository.class));
    }

    // --- ajutoare de aserție ---

    private static MeciPieteDto primulMeci(PieteZileDto rezultat) {
        assertFalse(rezultat.zile().isEmpty(), "asteptam meciuri in fereastra");
        return rezultat.zile().get(0).meciuri().get(0);
    }

    private static CotaPiataDto cota(MeciPieteDto meci, CodPiata cod, Double linie) {
        Optional<CotaPiataDto> gasit = meci.piete().stream()
                .filter(c -> c.piata() == cod)
                .filter(c -> linie == null ? c.linie() == null : linie.equals(c.linie()))
                .findFirst();
        assertTrue(gasit.isPresent(), "lipseste piata " + cod + " linia " + linie);
        return gasit.get();
    }

    private static boolean areP(MeciPieteDto meci, CodPiata cod) {
        return meci.piete().stream().anyMatch(c -> c.piata() == cod);
    }
}
