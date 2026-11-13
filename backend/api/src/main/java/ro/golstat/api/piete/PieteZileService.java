package ro.golstat.api.piete;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.golstat.api.entity.Fixture;
import ro.golstat.api.entity.League;
import ro.golstat.api.entity.Team;
import ro.golstat.api.piete.PieteZileDto.CotaPiataDto;
import ro.golstat.api.piete.PieteZileDto.LigaDto;
import ro.golstat.api.piete.PieteZileDto.MeciPieteDto;
import ro.golstat.api.piete.PieteZileDto.ZiDto;
import ro.golstat.api.prediction.PredictieMeciDto.EchipaDto;
import ro.golstat.api.prediction.PredictionService;
import ro.golstat.api.preview.StatisticiAvansateBuilder;
import ro.golstat.api.preview.StatisticiAvansateBuilder.FerestreEchipa;
import ro.golstat.api.preview.StatisticiAvansateDto;
import ro.golstat.api.preview.StatisticiAvansateDto.CornereReprizeDto;
import ro.golstat.api.preview.StatisticiAvansateDto.LinieDto;
import ro.golstat.api.preview.StatisticiAvansateDto.PiataDto;
import ro.golstat.api.preview.StatisticiAvansateDto.PiataEchipaDto;
import ro.golstat.api.preview.StatisticiAvansateDto.PieteEchipeDto;
import ro.golstat.api.repository.FixtureRepository;
import ro.golstat.api.repository.FixtureTeamStatsRepository;
import ro.golstat.api.repository.LeagueRepository;
import ro.golstat.api.repository.TeamRepository;
import ro.golstat.api.stats.CountLeagueAverageService;
import ro.golstat.api.stats.CountLeagueAverages;
import ro.golstat.api.stats.FerestreBatchService;
import ro.golstat.api.stats.FerestreMeci;
import ro.golstat.api.stats.ForteAdversariService;
import ro.golstat.api.stats.LeagueAverageService;
import ro.golstat.api.stats.LeagueAverages;
import ro.golstat.api.stats.RefereeCardAverageRow;
import ro.golstat.common.GolstatConstants;
import ro.golstat.stats.cards.RefereeFactor;
import ro.golstat.stats.goals.ForteAdversari;
import ro.golstat.stats.match.MatchGoalModel;
import ro.golstat.stats.model.EventCountSample;
import ro.golstat.stats.odds.Odds;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Probabilitatile pe piete ale TUTUROR meciurilor viitoare dintr-o fereastra de cateva zile,
 * grupate pe zi — vederea inversa fata de pagina de meci (alegi piata, vezi meciurile).
 *
 * <p>Foloseste exact aceleasi ferestre, aceleasi linii si acelasi motor ca
 * {@code MatchPreviewService}, ca procentele sa coincida intre cele doua pagini; diferenta e doar
 * ca datele se incarca in BLOC ({@link FerestreBatchService}), altfel ~200 de meciuri ar insemna
 * mii de query-uri.
 */
@Service
public class PieteZileService {

    /** Aceeasi fereastra ca la pagina de meci — altfel procentele n-ar mai coincide. */
    static final int FEREASTRA = 7;
    static final int HISTORY_FETCH = 40;

    private static final List<String> TERMINAL = List.of(
            GolstatConstants.FixtureStatus.FINISHED,
            GolstatConstants.FixtureStatus.FINISHED_AET,
            GolstatConstants.FixtureStatus.FINISHED_PEN
    );

    private final FixtureRepository fixtures;
    private final TeamRepository teams;
    private final LeagueRepository leagues;
    private final FixtureTeamStatsRepository teamStats;
    private final FerestreBatchService ferestre;
    private final LeagueAverageService leagueAverages;
    private final CountLeagueAverageService countAverages;
    private final ForteAdversariService forteAdversari;

    public PieteZileService(FixtureRepository fixtures, TeamRepository teams, LeagueRepository leagues,
                            FixtureTeamStatsRepository teamStats, FerestreBatchService ferestre,
                            LeagueAverageService leagueAverages, CountLeagueAverageService countAverages,
                            ForteAdversariService forteAdversari) {
        this.forteAdversari = forteAdversari;
        this.fixtures = fixtures;
        this.teams = teams;
        this.leagues = leagues;
        this.teamStats = teamStats;
        this.ferestre = ferestre;
        this.leagueAverages = leagueAverages;
        this.countAverages = countAverages;
    }

    public PieteZileDto piete(int zile) {
        return piete(zile, false);
    }

    /** {@code doarCompetitia}: formele echipelor doar din competitia fiecarui meci. */
    public PieteZileDto piete(int zile, boolean doarCompetitia) {
        // zile calendaristice intregi, de azi — altfel o fereastra rulanta de 3×24h ar atinge 4 date diferite
        LocalDate azi = LocalDate.now(ZoneOffset.UTC);
        return piete(azi, azi.plusDays(zile - 1L), doarCompetitia);
    }

    /** Cele mai lungi perioade alese din interfata (zile calendaristice). */
    static final int MAX_ZILE_PERIOADA = 14;

    /**
     * Pietele meciurilor viitoare dintr-o perioada aleasa, {@code de}..{@code panaLa} inclusiv. Incepe cel
     * mai devreme ACUM (meciurile deja incepute nu mai au piete) si e plafonata la
     * {@value #MAX_ZILE_PERIOADA} zile, ca o perioada uriasa sa nu incarce sute de meciuri dintr-odata.
     */
    @Transactional(readOnly = true)
    public PieteZileDto piete(LocalDate de, LocalDate panaLa, boolean doarCompetitia) {
        OffsetDateTime acum = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime start = de.atStartOfDay().atOffset(ZoneOffset.UTC);
        if (start.isBefore(acum)) {
            start = acum;
        }
        LocalDate maxim = de.plusDays(MAX_ZILE_PERIOADA - 1L);
        LocalDate ultima = panaLa.isAfter(maxim) ? maxim : panaLa;
        OffsetDateTime pana = ultima.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        if (!pana.isAfter(start)) {
            return new PieteZileDto(List.of());
        }
        List<Fixture> meciuri = fixtures
                .findUpcomingAll(GolstatConstants.FixtureStatus.NOT_STARTED, start, pana).stream()
                .filter(f -> f.getHomeTeamId() != null && f.getAwayTeamId() != null
                        && f.getKickoff() != null && f.getLeagueId() != null && f.getSeasonYear() != null)
                .toList();
        if (meciuri.isEmpty()) {
            return new PieteZileDto(List.of());
        }

        Map<Long, Team> echipe = echipe(meciuri);
        Map<Long, League> ligi = ligi(meciuri);
        Map<Long, FerestreMeci> ferestrePerMeci = ferestre.ferestre(meciuri, FEREASTRA, HISTORY_FETCH, doarCompetitia);
        Map<String, RefereeCardAverageRow> arbitri = arbitri(meciuri, acum);
        ForteAdversari forte = forteAdversari.pentru(ferestrePerMeci.values().stream()
                .flatMap(m -> Stream.of(m.istoricGazde(), m.istoricOaspeti())).toList(), acum);

        Map<String, LeagueAverages> cacheGoluri = new HashMap<>();
        Map<String, CountLeagueAverages> cacheCounturi = new HashMap<>();
        Map<LocalDate, List<MeciPieteDto>> peZi = new LinkedHashMap<>();

        for (Fixture f : meciuri) {
            FerestreMeci fer = ferestrePerMeci.get(f.getId());
            if (fer == null) {
                continue;
            }
            // meciuri viitoare: tot ce s-a terminat inainte de kickoff s-a terminat deja acum, deci "acum" da
            // aceleasi medii ca pagina de meci (care taie la kickoff), cu un singur query per liga
            String cheie = f.getLeagueId() + ":" + f.getSeasonYear();
            LeagueAverages mediiGoluri = cacheGoluri.computeIfAbsent(cheie,
                    k -> leagueAverages.averages(f.getLeagueId(), f.getSeasonYear(), acum));
            CountLeagueAverages mediiCounturi = cacheCounturi.computeIfAbsent(cheie,
                    k -> countAverages.averages(f.getLeagueId(), f.getSeasonYear(), acum));

            StatisticiAvansateDto statistici = StatisticiAvansateBuilder.build(
                    fer.gazde(), fer.oaspeti(), mediiCounturi,
                    factorArbitru(f, arbitri, mediiCounturi.cartonasePeMeci()),
                    MatchGoalModel.predict(PredictionService.buildContext(
                            fer.istoricGazde(), fer.istoricOaspeti(), mediiGoluri, forte)),
                    null);

            List<CotaPiataDto> piete = piete(fer, statistici);
            if (piete.isEmpty()) {
                continue;
            }
            LocalDate zi = f.getKickoff().withOffsetSameInstant(ZoneOffset.UTC).toLocalDate();
            peZi.computeIfAbsent(zi, k -> new ArrayList<>()).add(meci(f, ligi, echipe, piete));
        }

        return new PieteZileDto(peZi.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new ZiDto(e.getKey(), e.getValue()))
                .toList());
    }

    /**
     * Aplatizeaza analiza pe piete in lista compacta. Esantionul fiecarei piete = cate meciuri din
     * istoricul celor doua echipe a folosit modelul (ultimele 7 acasa / in deplasare); esantion 0 → piata nu
     * se trimite, deci meciul nu apare in filtrul respectiv.
     */
    private static List<CotaPiataDto> piete(FerestreMeci fer, StatisticiAvansateDto statistici) {
        List<CotaPiataDto> out = new ArrayList<>();
        int esantionGoluri = fer.gazde().goluriLocatie().size() + fer.oaspeti().goluriLocatie().size();
        int esantionCornere = esantion(fer, f -> f.forma().cornere());
        int esantionCartonase = esantion(fer, f -> f.forma().cartonase());

        adaugaLinii(out, statistici.goluri(), CodPiata.GOLURI_PESTE, esantionGoluri, false);
        adaugaLinii(out, statistici.goluri(), CodPiata.GOLURI_SUB, esantionGoluri, true);
        if (statistici.gg() != null && esantionGoluri > 0) {
            adauga(out, CodPiata.GG, null, statistici.gg().probabilitate(), esantionGoluri);
            adauga(out, CodPiata.NG, null, 1 - statistici.gg().probabilitate(), esantionGoluri);
        }
        adaugaLinii(out, statistici.cornere(), CodPiata.CORNERE_PESTE, esantionCornere, false);
        adaugaLinii(out, statistici.faulturi(), CodPiata.FAULTURI_PESTE,
                esantion(fer, f -> f.forma().faulturi()), false);
        adaugaLinii(out, statistici.cartonase(), CodPiata.CARTONASE_PESTE, esantionCartonase, false);
        if (statistici.egaluri() != null && esantionGoluri > 0) {
            adauga(out, CodPiata.EGAL_PAUZA, null, statistici.egaluri().egalPauza(), esantionGoluri);
            adauga(out, CodPiata.EGAL_FINAL, null, statistici.egaluri().egalFinal(), esantionGoluri);
        }
        PieteEchipeDto echipe = statistici.echipe();
        if (echipe != null) {
            adaugaEchipa(out, echipe.goluri(), CodPiata.GOLURI_GAZDE_PESTE, CodPiata.GOLURI_OASPETI_PESTE,
                    esantionGoluri);
            adaugaEchipa(out, echipe.cornere(), CodPiata.CORNERE_GAZDE_PESTE, CodPiata.CORNERE_OASPETI_PESTE,
                    esantionCornere);
            adaugaEchipa(out, echipe.cartonase(), CodPiata.CARTONASE_GAZDE_PESTE,
                    CodPiata.CARTONASE_OASPETI_PESTE, esantionCartonase);
        }
        CornereReprizeDto reprize = statistici.cornereReprize();
        if (reprize != null) {
            adaugaLinii(out, reprize.repriza1(), CodPiata.CORNERE_REPRIZA1_PESTE,
                    esantion(fer, f -> f.forma().cornereRepriza1()), false);
            adaugaLinii(out, reprize.repriza2(), CodPiata.CORNERE_REPRIZA2_PESTE,
                    esantion(fer, f -> f.forma().cornereRepriza2()), false);
        }
        return List.copyOf(out);
    }

    private static int esantion(FerestreMeci fer, Function<FerestreEchipa, List<EventCountSample>> piata) {
        return piata.apply(fer.gazde()).size() + piata.apply(fer.oaspeti()).size();
    }

    private static void adaugaEchipa(List<CotaPiataDto> out, PiataEchipaDto piata, CodPiata gazde,
                                     CodPiata oaspeti, int esantion) {
        if (piata == null || esantion == 0) {
            return;
        }
        piata.gazde().forEach(l -> adauga(out, gazde, l.linie(), l.probabilitate(), esantion));
        piata.oaspeti().forEach(l -> adauga(out, oaspeti, l.linie(), l.probabilitate(), esantion));
    }

    private static void adaugaLinii(List<CotaPiataDto> out, PiataDto piata, CodPiata cod,
                                    int esantion, boolean sub) {
        if (piata == null || esantion == 0) {
            return;
        }
        for (LinieDto linie : piata.linii()) {
            adauga(out, cod, linie.linie(),
                    sub ? 1 - linie.probabilitate() : linie.probabilitate(), esantion);
        }
    }

    /** Probabilitatile la 0 (sau nefinite) n-au cota utila (1/0 = infinit) — le sarim. */
    private static void adauga(List<CotaPiataDto> out, CodPiata cod, Double linie, double p, int esantion) {
        if (!Double.isFinite(p) || p <= 0) {
            return;
        }
        out.add(new CotaPiataDto(cod, linie, p, rotunjit(Odds.fromProbability(p)), esantion));
    }

    private static MeciPieteDto meci(Fixture f, Map<Long, League> ligi, Map<Long, Team> echipe,
                                     List<CotaPiataDto> piete) {
        League liga = ligi.get(f.getLeagueId());
        return new MeciPieteDto(f.getId(), f.getKickoff(),
                new LigaDto(f.getLeagueId(), liga != null ? liga.getName() : null,
                        liga != null ? liga.getLogo() : null),
                echipa(echipe, f.getHomeTeamId()), echipa(echipe, f.getAwayTeamId()), piete);
    }

    private static EchipaDto echipa(Map<Long, Team> echipe, long id) {
        Team t = echipe.get(id);
        return t != null ? new EchipaDto(t.getId(), t.getName(), t.getLogo()) : new EchipaDto(id, null, null);
    }

    /** Mediile de cartonase ale tuturor arbitrilor din fereastra, intr-un singur query agregat. */
    private Map<String, RefereeCardAverageRow> arbitri(List<Fixture> meciuri, OffsetDateTime acum) {
        Set<String> nume = meciuri.stream()
                .map(Fixture::getReferee)
                .filter(r -> r != null && !r.isBlank())
                .collect(Collectors.toSet());
        if (nume.isEmpty()) {
            return Map.of();
        }
        return teamStats.refereeCardAverages(nume, TERMINAL, acum).stream()
                .filter(r -> r.getReferee() != null)
                .collect(Collectors.toMap(RefereeCardAverageRow::getReferee, Function.identity(), (a, b) -> a));
    }

    /** Aceeasi regula ca {@code RefereeService.factor}: arbitru necunoscut sau fara istoric → neutru. */
    private static double factorArbitru(Fixture f, Map<String, RefereeCardAverageRow> arbitri, double mediaLiga) {
        String nume = f.getReferee();
        if (nume == null || nume.isBlank()) {
            return RefereeFactor.NEUTRAL;
        }
        RefereeCardAverageRow agg = arbitri.get(nume);
        if (agg == null || agg.getAvgCards() == null || agg.getMatches() == null || agg.getMatches() == 0) {
            return RefereeFactor.NEUTRAL;
        }
        return RefereeFactor.of(agg.getAvgCards(), agg.getMatches(), mediaLiga);
    }

    private Map<Long, Team> echipe(List<Fixture> meciuri) {
        List<Long> ids = meciuri.stream()
                .flatMap(f -> Stream.of(f.getHomeTeamId(), f.getAwayTeamId()))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return teams.findAllById(ids).stream()
                .collect(Collectors.toMap(Team::getId, Function.identity(), (a, b) -> a));
    }

    private Map<Long, League> ligi(List<Fixture> meciuri) {
        List<Long> ids = meciuri.stream().map(Fixture::getLeagueId).filter(Objects::nonNull).distinct().toList();
        return leagues.findAllById(ids).stream()
                .collect(Collectors.toMap(League::getId, Function.identity(), (a, b) -> a));
    }

    private static double rotunjit(double valoare) {
        return Math.round(valoare * 100.0) / 100.0;
    }
}
