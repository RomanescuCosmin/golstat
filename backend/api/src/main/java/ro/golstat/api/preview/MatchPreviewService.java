package ro.golstat.api.preview;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import ro.golstat.api.entity.Fixture;
import ro.golstat.api.entity.FixtureLineup;
import ro.golstat.api.entity.FixtureLineupPlayer;
import ro.golstat.api.entity.FixtureTeamHalfStats;
import ro.golstat.api.entity.FixtureTeamStats;
import ro.golstat.api.entity.Injury;
import ro.golstat.api.entity.Player;
import ro.golstat.api.entity.Team;
import ro.golstat.api.prediction.PredictieMeciDto;
import ro.golstat.api.prediction.PredictieMeciDto.EchipaDto;
import ro.golstat.api.prediction.PredictieMeciMapper;
import ro.golstat.api.prediction.PredictionNotFoundException;
import ro.golstat.api.prediction.PredictionService;
import ro.golstat.api.preview.StatisticiAvansateBuilder.FerestreEchipa;
import ro.golstat.api.repository.FixtureEventRepository;
import ro.golstat.api.repository.FixtureLineupPlayerRepository;
import ro.golstat.api.repository.FixtureLineupRepository;
import ro.golstat.api.repository.FixtureRepository;
import ro.golstat.api.repository.FixtureTeamHalfStatsRepository;
import ro.golstat.api.repository.FixtureTeamStatsRepository;
import ro.golstat.api.repository.InjuryRepository;
import ro.golstat.api.repository.PlayerRepository;
import ro.golstat.api.repository.TeamRepository;
import ro.golstat.api.stats.CountLeagueAverageService;
import ro.golstat.api.stats.CountLeagueAverages;
import ro.golstat.api.stats.IstoricCounturi;
import ro.golstat.api.stats.ForteAdversariService;
import ro.golstat.api.stats.LeagueAverageService;
import ro.golstat.api.stats.LeagueAverages;
import ro.golstat.api.stats.MatchHistoryService;
import ro.golstat.api.stats.RefereeService;
import ro.golstat.api.stats.RefereeProfile;
import ro.golstat.api.stats.StatsHistoryService;
import ro.golstat.common.FormatieGrid;
import ro.golstat.common.GolstatConstants;
import ro.golstat.stats.form.MatchWindow;
import ro.golstat.stats.match.MatchGoalModel;
import ro.golstat.stats.match.MatchPrediction;
import ro.golstat.stats.model.MatchLocation;
import ro.golstat.stats.model.MatchSample;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Previzualizarea unui meci VIITOR pentru pagina de meci: predictia de goluri
 * ({@link PredictionService}) + forma ultimelor {@value #FEREASTRA} meciuri per echipa (pe locatia
 * din meci si generala) + intalnirile directe + analiza pe piete pe aceleasi ferestre
 * ({@link StatisticiAvansateBuilder}) + statisticile cheie + echipa de start.
 */
@Service
public class MatchPreviewService {

    /** Fereastra de analiza: ultimele N meciuri (pe locatie, respectiv generale). */
    static final int FEREASTRA = 7;
    /** Cate meciuri tragem din istoric, ca sa ramana destule dupa filtrul de locatie. */
    static final int HISTORY_FETCH = 40;
    /** Pe cate meciuri (cu statistici) se calculeaza mediile din statisticile cheie. */
    static final int FEREASTRA_STATISTICI = 10;
    /** Cate intalniri directe afisam. */
    static final int H2H_LIMIT = 5;
    /** Raportarile de indisponibili mai vechi de atat (fata de kickoff) nu mai descriu meciul curent. */
    static final int INDISPONIBILI_ZILE = 30;

    private static final List<String> TERMINAL = List.of(
            GolstatConstants.FixtureStatus.FINISHED,
            GolstatConstants.FixtureStatus.FINISHED_AET,
            GolstatConstants.FixtureStatus.FINISHED_PEN
    );

    private final PredictionService predictions;
    private final FixtureRepository fixtures;
    private final MatchHistoryService history;
    private final StatsHistoryService statsHistory;
    private final CountLeagueAverageService countAverages;
    private final LeagueAverageService leagueAverages;
    private final RefereeService referees;
    private final TeamRepository teams;
    private final FixtureLineupRepository lineups;
    private final FixtureLineupPlayerRepository lineupPlayers;
    private final InjuryRepository injuries;
    private final PlayerRepository players;
    private final FixtureTeamStatsRepository teamStats;
    private final FixtureEventRepository events;
    private final FixtureTeamHalfStatsRepository halfStats;
    private final ForteAdversariService forteAdversari;

    /** Adresa pozelor de jucator la furnizor, cu {id}; goala = doar pozele din baza. */
    @Value("${golstat.foto-jucator-url:}")
    private String fotoJucatorUrl;

    public MatchPreviewService(PredictionService predictions, FixtureRepository fixtures,
                               MatchHistoryService history, StatsHistoryService statsHistory,
                               CountLeagueAverageService countAverages, LeagueAverageService leagueAverages,
                               RefereeService referees, TeamRepository teams,
                               FixtureLineupRepository lineups, FixtureLineupPlayerRepository lineupPlayers,
                               InjuryRepository injuries, PlayerRepository players,
                               FixtureTeamStatsRepository teamStats, FixtureEventRepository events,
                               FixtureTeamHalfStatsRepository halfStats, ForteAdversariService forteAdversari) {
        this.halfStats = halfStats;
        this.forteAdversari = forteAdversari;
        this.predictions = predictions;
        this.fixtures = fixtures;
        this.history = history;
        this.statsHistory = statsHistory;
        this.countAverages = countAverages;
        this.leagueAverages = leagueAverages;
        this.referees = referees;
        this.teams = teams;
        this.lineups = lineups;
        this.lineupPlayers = lineupPlayers;
        this.injuries = injuries;
        this.players = players;
        this.teamStats = teamStats;
        this.events = events;
    }

    /** Previzualizarea unui meci dupa id; {@link PredictionNotFoundException} daca nu are predictie. */
    public PrevizualizareMeciDto previzualizare(long fixtureId) {
        return previzualizare(fixtureId, false);
    }

    /**
     * {@code doarCompetitia} = ferestrele de forma iau doar meciurile din competitia acestui meci (filtrul
     * „Doar {competitia}“ din interfata); altfel toate competitiile. Predictia se recalculeaza pe aceleasi
     * ferestre, ca blocul de 1X2 sa nu contrazica procentele de dedesubt.
     */
    public PrevizualizareMeciDto previzualizare(long fixtureId, boolean doarCompetitia) {
        PredictieMeciDto predictie = predictions.predict(fixtureId)
                .orElseThrow(() -> new PredictionNotFoundException(fixtureId));
        Fixture meci = fixtures.findById(fixtureId)
                .orElseThrow(() -> new PredictionNotFoundException(fixtureId));
        Long liga = doarCompetitia ? meci.getLeagueId() : null;

        List<MatchSample> istoricGazde =
                history.lastMatches(meci.getHomeTeamId(), meci.getKickoff(), HISTORY_FETCH, liga);
        List<MatchSample> istoricOaspeti =
                history.lastMatches(meci.getAwayTeamId(), meci.getKickoff(), HISTORY_FETCH, liga);
        IstoricCounturi counturiGazde =
                statsHistory.istoric(meci.getHomeTeamId(), meci.getKickoff(), HISTORY_FETCH, liga);
        IstoricCounturi counturiOaspeti =
                statsHistory.istoric(meci.getAwayTeamId(), meci.getKickoff(), HISTORY_FETCH, liga);
        CountLeagueAverages mediiCounturi = countAverages.averages(meci.getLeagueId(), meci.getSeasonYear(), meci.getKickoff());
        LeagueAverages mediiGoluri = leagueAverages.averages(meci.getLeagueId(), meci.getSeasonYear(), meci.getKickoff());
        double factorArbitru = referees.factor(meci.getReferee(), mediiCounturi.cartonasePeMeci(), meci.getKickoff());
        MatchPrediction modelGoluri = MatchGoalModel.predict(
                PredictionService.buildContext(istoricGazde, istoricOaspeti, mediiGoluri,
                        forteAdversari.pentru(List.of(istoricGazde, istoricOaspeti), meci.getKickoff())));
        if (doarCompetitia) {
            predictie = PredictieMeciMapper.toDto(meci, modelGoluri,
                    teams.findById(meci.getHomeTeamId()).orElse(null),
                    teams.findById(meci.getAwayTeamId()).orElse(null));
        }

        StatisticiAvansateDto statistici = StatisticiAvansateBuilder.build(
                ferestre(istoricGazde, counturiGazde, MatchLocation.HOME),
                ferestre(istoricOaspeti, counturiOaspeti, MatchLocation.AWAY),
                mediiCounturi, factorArbitru,
                modelGoluri, rezultatStatistici(meci));

        Map<Long, EchipaDto> adversari = adversari(istoricGazde, istoricOaspeti);
        return new PrevizualizareMeciDto(predictie,
                forma(istoricGazde, MatchLocation.HOME, adversari),
                forma(istoricOaspeti, MatchLocation.AWAY, adversari),
                intalniriDirecte(meci),
                statistici,
                new StatisticiCheieDto(statisticiEchipa(counturiGazde.statisticiEchipa()),
                        statisticiEchipa(counturiOaspeti.statisticiEchipa())),
                echipeDeStart(meci),
                arbitru(meci, mediiCounturi.cartonasePeMeci(), factorArbitru));
    }

    /** Cardul arbitrului: istoricul lui din meciurile terminate dinaintea acestui meci. */
    private ArbitruDto arbitru(Fixture meci, double cartonaseMediaLigii, double factor) {
        if (meci.getReferee() == null || meci.getReferee().isBlank()) {
            return null;
        }
        RefereeProfile profil = teamStats.refereeProfile(meci.getReferee(), TERMINAL, meci.getKickoff());
        int meciuri = profil != null && profil.getMatches() != null ? profil.getMatches().intValue() : 0;
        return new ArbitruDto(meci.getReferee(), meciuri,
                meciuri > 0 ? rotunjitSauNull(profil.getAvgCards()) : null,
                meciuri > 0 ? rotunjitSauNull(profil.getAvgFouls()) : null,
                meciuri > 0 && profil.getReds() != null ? profil.getReds().intValue() : 0,
                rotunjit(cartonaseMediaLigii), rotunjit(factor));
    }

    private static Double rotunjitSauNull(Double x) {
        return x != null ? Math.round(x * 100.0) / 100.0 : null;
    }

    /** Ferestrele unei echipe pentru analiza pe piete: ultimele {@value #FEREASTRA}, locatie + general. */
    private static FerestreEchipa ferestre(List<MatchSample> istoric, IstoricCounturi counturi,
                                           MatchLocation locatie) {
        return StatisticiAvansateBuilder.ferestre(istoric, counturi, locatie, FEREASTRA);
    }

    /**
     * Totalurile reale ale meciului pentru marcarea hit/miss; {@code null} la meciuri ne-terminale.
     * Golurile la 90 min (prefera scoreFt, exclude prelungirile); reprizele din scorul la pauza
     * ({@code null} cand lipseste); count-urile sumate din fixture_team_stats ale acestui meci
     * ({@code null} cand meciul n-are statistici colectate — ex. amicale internationale).
     */
    private StatisticiAvansateDto.RezultatDto rezultatStatistici(Fixture meci) {
        if (meci.getStatusShort() == null
                || !GolstatConstants.FixtureStatus.TERMINAL.contains(meci.getStatusShort())) {
            return null;
        }
        Integer gazde90 = scorFinal(meci.getScoreFtHome(), meci.getGoalsHome());
        Integer oaspeti90 = scorFinal(meci.getScoreFtAway(), meci.getGoalsAway());
        if (gazde90 == null || oaspeti90 == null) {
            return null;
        }
        int totalGoluri = gazde90 + oaspeti90;
        boolean ambeleMarcheaza = gazde90 > 0 && oaspeti90 > 0;
        boolean egalFinal = gazde90.equals(oaspeti90);

        Boolean egalPauza = null;
        Boolean golRepriza1 = null;
        Boolean golRepriza2 = null;
        Integer htGazde = meci.getScoreHtHome();
        Integer htOaspeti = meci.getScoreHtAway();
        if (htGazde != null && htOaspeti != null) {
            int golPauza = htGazde + htOaspeti;
            egalPauza = htGazde.equals(htOaspeti);
            golRepriza1 = golPauza > 0;
            golRepriza2 = totalGoluri - golPauza > 0;
        }

        Map<Long, FixtureTeamStats> perEchipa = teamStats.findByFixtureIdIn(List.of(meci.getId())).stream()
                .filter(s -> s.getTeamId() != null)
                .collect(Collectors.toMap(FixtureTeamStats::getTeamId, Function.identity(), (a, b) -> a));
        FixtureTeamStats gazdeStats = perEchipa.get(meci.getHomeTeamId());
        FixtureTeamStats oaspetiStats = perEchipa.get(meci.getAwayTeamId());

        return new StatisticiAvansateDto.RezultatDto(
                totalGoluri, ambeleMarcheaza, egalFinal, egalPauza, golRepriza1, golRepriza2,
                totalCount(gazdeStats, oaspetiStats, FixtureTeamStats::getCornerKicks),
                totalCount(gazdeStats, oaspetiStats, FixtureTeamStats::getFouls),
                cartonase(meci, gazdeStats, oaspetiStats),
                totalCount(gazdeStats, oaspetiStats, FixtureTeamStats::getShotsTotal),
                totalCount(gazdeStats, oaspetiStats, FixtureTeamStats::getShotsOnGoal),
                rezultatEchipe(meci, gazde90, oaspeti90, gazdeStats, oaspetiStats));
    }

    private StatisticiAvansateDto.RezultatEchipeDto rezultatEchipe(Fixture meci, int gazde90, int oaspeti90,
                                                                   FixtureTeamStats gazde, FixtureTeamStats oaspeti) {
        Map<String, FixtureTeamHalfStats> reprize = halfStats.findByFixtureIdIn(List.of(meci.getId())).stream()
                .filter(r -> r.getTeamId() != null && r.getHalf() != null)
                .collect(Collectors.toMap(r -> r.getTeamId() + ":" + r.getHalf(), Function.identity(), (a, b) -> a));
        return new StatisticiAvansateDto.RezultatEchipeDto(gazde90, oaspeti90,
                gazde != null ? gazde.getCornerKicks() : null,
                oaspeti != null ? oaspeti.getCornerKicks() : null,
                gazde != null ? StatsHistoryService.totalCartonase(gazde) : null,
                oaspeti != null ? StatsHistoryService.totalCartonase(oaspeti) : null,
                cornereRepriza(reprize, meci.getHomeTeamId(), 1),
                cornereRepriza(reprize, meci.getAwayTeamId(), 1),
                cornereRepriza(reprize, meci.getHomeTeamId(), 2),
                cornereRepriza(reprize, meci.getAwayTeamId(), 2));
    }

    private static Integer cornereRepriza(Map<String, FixtureTeamHalfStats> reprize, Long teamId, int repriza) {
        FixtureTeamHalfStats r = reprize.get(teamId + ":" + repriza);
        return r != null ? r.getCornerKicks() : null;
    }

    /**
     * Cartonasele meciului, cu plasa de siguranta pe cronologie: cand furnizorul n-a publicat
     * statistici de echipa dar A publicat evenimentele, numarul real e deja la noi si n-are rost sa
     * afisam "fara date". Cornerele si faulturile NU se pot recupera asa — nu exista ca evenimente.
     */
    private Integer cartonase(Fixture meci, FixtureTeamStats gazde, FixtureTeamStats oaspeti) {
        Integer dinStatistici = totalCount(gazde, oaspeti, StatsHistoryService::totalCartonase);
        if (dinStatistici != null) {
            return dinStatistici;
        }
        long dinEvenimente = events.countCards(meci.getId(), GolstatConstants.EventType.CARD);
        // zero cartonase e un rezultat legitim, dar zero EVENIMENTE inseamna cronologie necolectata
        return dinEvenimente > 0 ? (int) dinEvenimente : null;
    }

    /** Suma unei statistici pe ambele echipe; {@code null} daca lipseste pe oricare parte. */
    private static Integer totalCount(FixtureTeamStats gazde, FixtureTeamStats oaspeti,
                                      Function<FixtureTeamStats, Integer> camp) {
        if (gazde == null || oaspeti == null) {
            return null;
        }
        Integer a = camp.apply(gazde);
        Integer b = camp.apply(oaspeti);
        return a != null && b != null ? a + b : null;
    }

    static FormaEchipaDto forma(List<MatchSample> istoric, MatchLocation locatie) {
        return forma(istoric, locatie, Map.of());
    }

    static FormaEchipaDto forma(List<MatchSample> istoric, MatchLocation locatie, Map<Long, EchipaDto> adversari) {
        return new FormaEchipaDto(
                fereastraForma(MatchWindow.lastN(istoric, FEREASTRA, locatie), adversari),
                fereastraForma(MatchWindow.lastN(istoric, FEREASTRA), adversari));
    }

    /** Adversarii din ultimele meciuri ale ambelor echipe, cu nume si emblema — un singur query. */
    private Map<Long, EchipaDto> adversari(List<MatchSample> a, List<MatchSample> b) {
        List<Long> ids = Stream.concat(a.stream(), b.stream())
                .map(MatchSample::opponentId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return teams.findAllById(ids).stream()
                .collect(Collectors.toMap(Team::getId, t -> new EchipaDto(t.getId(), t.getName(), t.getLogo())));
    }

    private static FormaEchipaDto.FereastraFormaDto fereastraForma(List<MatchSample> fereastra,
                                                                   Map<Long, EchipaDto> adversari) {
        List<FormaMeciDto> meciuri = fereastra.stream()
                .map(s -> formaMeci(s, adversari))
                .toList();
        double marcate = fereastra.stream().mapToInt(MatchSample::goalsFor).average().orElse(0.0);
        double primite = fereastra.stream().mapToInt(MatchSample::goalsAgainst).average().orElse(0.0);
        return new FormaEchipaDto.FereastraFormaDto(meciuri, rotunjit(marcate), rotunjit(primite));
    }

    private static FormaMeciDto formaMeci(MatchSample s, Map<Long, EchipaDto> adversari) {
        String rezultat = s.goalsFor() > s.goalsAgainst() ? "V"
                : s.goalsFor() == s.goalsAgainst() ? "E" : "I";
        EchipaDto adversar = s.opponentId() == null ? null
                : adversari.getOrDefault(s.opponentId(), new EchipaDto(s.opponentId(), null, null));
        return new FormaMeciDto(s.date(), s.home(), s.goalsFor(), s.goalsAgainst(), rezultat, adversar);
    }

    /**
     * Formatia anuntata a meciului; cand lipseste (meci viitor), echipa PROBABILA — ultimul
     * unsprezece de start al echipei ({@code probabila = true}). {@code null} doar cand nici
     * macar o formatie anterioara nu exista pentru una din echipe.
     */
    private EchipaDeStartDto echipeDeStart(Fixture meci) {
        Map<Long, FixtureLineup> anuntate = lineups.findByFixtureId(meci.getId()).stream()
                .filter(l -> l.getTeamId() != null)
                .collect(Collectors.toMap(FixtureLineup::getTeamId, Function.identity()));
        LineupEchipa gazde = lineupEchipa(meci, meci.getHomeTeamId(), anuntate);
        LineupEchipa oaspeti = lineupEchipa(meci, meci.getAwayTeamId(), anuntate);
        if (gazde == null || oaspeti == null) {
            return null;
        }
        Map<Long, String> fotografii = fotografii(gazde.jucatori(), oaspeti.jucatori());
        Map<Long, List<EchipaDeStartDto.IndisponibilDto>> indisponibili = indisponibili(meci);
        return new EchipaDeStartDto(
                echipaLineup(gazde, fotografii,
                        indisponibili.getOrDefault(meci.getHomeTeamId(), List.of())),
                echipaLineup(oaspeti, fotografii,
                        indisponibili.getOrDefault(meci.getAwayTeamId(), List.of())),
                meci.getReferee(),
                gazde.probabila() || oaspeti.probabila());
    }

    /** Formatia unei echipe cu jucatorii ei; {@code probabila} = luata din meciul anterior. */
    private record LineupEchipa(FixtureLineup lineup, List<FixtureLineupPlayer> jucatori,
                                boolean probabila) {
    }

    private LineupEchipa lineupEchipa(Fixture meci, Long teamId, Map<Long, FixtureLineup> anuntate) {
        if (teamId == null) {
            return null;
        }
        FixtureLineup anuntata = anuntate.get(teamId);
        if (anuntata != null) {
            return new LineupEchipa(anuntata,
                    lineupPlayers.findByFixtureIdAndTeamId(meci.getId(), teamId), false);
        }
        return lineups.findRecentForTeam(teamId, TERMINAL, meci.getKickoff(), PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .map(anterior -> new LineupEchipa(anterior,
                        lineupPlayers.findByFixtureIdAndTeamId(anterior.getFixtureId(), teamId), true))
                .orElse(null);
    }

    /** Pozele de profil ale jucatorilor din ambele formatii (id → URL); jucatorii fara poza lipsesc. */
    private Map<Long, String> fotografii(List<FixtureLineupPlayer> gazde,
                                         List<FixtureLineupPlayer> oaspeti) {
        List<Long> ids = Stream.concat(gazde.stream(), oaspeti.stream())
                .map(FixtureLineupPlayer::getPlayerId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return players.findAllById(ids).stream()
                .filter(p -> p.getPhoto() != null)
                .collect(Collectors.toMap(Player::getId, Player::getPhoto));
    }

    private EchipaDeStartDto.EchipaLineupDto echipaLineup(LineupEchipa echipa,
                                                                 Map<Long, String> fotografii,
                                                                 List<EchipaDeStartDto.IndisponibilDto> indisponibili) {
        return new EchipaDeStartDto.EchipaLineupDto(
                echipa.lineup().getFormation(),
                jucatoriDto(echipa.jucatori(), false, fotografii, echipa.lineup().getFormation()),
                jucatoriDto(echipa.jucatori(), true, fotografii, null),
                indisponibili);
    }

    private List<EchipaDeStartDto.JucatorDto> jucatoriDto(List<FixtureLineupPlayer> jucatori,
                                                          boolean rezerve,
                                                          Map<Long, String> fotografii,
                                                          String formatie) {
        List<FixtureLineupPlayer> lista = jucatori.stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsSubstitute()) == rezerve)
                .sorted(Comparator.comparingInt(p -> FormatieGrid.rand(p.getPosition())))
                .toList();
        // formatiile salvate fara grid (bzzoiro, inainte de calculul la import): il deducem din post + formatie
        List<String> grid = !rezerve && lista.stream().anyMatch(p -> p.getGrid() == null)
                ? FormatieGrid.grid(lista.stream().map(FixtureLineupPlayer::getPosition).toList(), formatie)
                : null;
        List<EchipaDeStartDto.JucatorDto> out = new ArrayList<>();
        for (int i = 0; i < lista.size(); i++) {
            FixtureLineupPlayer p = lista.get(i);
            out.add(new EchipaDeStartDto.JucatorDto(p.getPlayerId(), p.getPlayerName(), p.getNumber(),
                    p.getPosition(), grid != null ? grid.get(i) : p.getGrid(), foto(p.getPlayerId(), fotografii)));
        }
        return out;
    }

    /** Poza din baza; altfel adresa furnizorului ({@code golstat.foto-jucator-url}, cu {id}), daca e configurata. */
    private String foto(Long playerId, Map<Long, String> fotografii) {
        if (playerId == null) {
            return null;
        }
        String dinBaza = fotografii.get(playerId);
        if (dinBaza != null || fotoJucatorUrl == null || fotoJucatorUrl.isBlank()) {
            return dinBaza;
        }
        return fotoJucatorUrl.replace("{id}", String.valueOf(playerId));
    }

    /** Cea mai recenta raportare per jucator, doar din fereastra recenta, grupata pe echipa. */
    private Map<Long, List<EchipaDeStartDto.IndisponibilDto>> indisponibili(Fixture meci) {
        List<Injury> randuri = injuries.findByLeagueIdAndSeasonYearAndTeamIdIn(
                meci.getLeagueId(), meci.getSeasonYear(),
                List.of(meci.getHomeTeamId(), meci.getAwayTeamId()));
        LocalDate limita = meci.getKickoff() != null
                ? meci.getKickoff().toLocalDate().minusDays(INDISPONIBILI_ZILE) : null;
        Map<Long, Injury> perJucator = new HashMap<>();
        for (Injury injury : randuri) {
            if (injury.getPlayerId() == null || injury.getTeamId() == null) {
                continue;
            }
            if (limita != null && injury.getReportedAt() != null && injury.getReportedAt().isBefore(limita)) {
                continue;
            }
            perJucator.merge(injury.getPlayerId(), injury, MatchPreviewService::maiRecenta);
        }
        Map<Long, String> nume = players.findAllById(perJucator.keySet()).stream()
                .filter(p -> p.getName() != null)
                .collect(Collectors.toMap(Player::getId, Player::getName));
        Map<Long, List<EchipaDeStartDto.IndisponibilDto>> grupate = perJucator.values().stream()
                .map(i -> Map.entry(i.getTeamId(), new EchipaDeStartDto.IndisponibilDto(
                        i.getPlayerId(), nume.get(i.getPlayerId()), motiv(i.getType(), i.getReason()), i.getReason())))
                .collect(Collectors.groupingBy(Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
        grupate.values().forEach(lista -> lista.sort(
                Comparator.comparing(EchipaDeStartDto.IndisponibilDto::nume,
                        Comparator.nullsLast(Comparator.naturalOrder()))));
        return grupate;
    }

    private static Injury maiRecenta(Injury a, Injury b) {
        if (a.getReportedAt() == null) {
            return b;
        }
        if (b.getReportedAt() == null) {
            return a;
        }
        return a.getReportedAt().isBefore(b.getReportedAt()) ? b : a;
    }

    /** API-Football: type "Questionable" = incert; reason cu suspendare/cartonase = suspendat. */
    static String motiv(String type, String reason) {
        String r = reason != null ? reason.toLowerCase(Locale.ROOT) : "";
        if (r.contains("suspen") || r.contains("card")) {
            return "SUSPENDAT";
        }
        if ("Questionable".equalsIgnoreCase(type)) {
            return "INCERT";
        }
        return "ACCIDENTAT";
    }

    static StatisticiCheieDto.StatisticiEchipaDto statisticiEchipa(List<FixtureTeamStats> randuri) {
        List<FixtureTeamStats> fereastra = randuri.stream().limit(FEREASTRA_STATISTICI).toList();
        return new StatisticiCheieDto.StatisticiEchipaDto(
                medie(fereastra, s -> s.getBallPossession() != null ? s.getBallPossession().doubleValue() : null),
                medie(fereastra, s -> caDouble(s.getShotsTotal())),
                medie(fereastra, s -> caDouble(s.getShotsOnGoal())),
                medie(fereastra, s -> caDouble(s.getCornerKicks())),
                medie(fereastra, s -> caDouble(StatsHistoryService.totalCartonase(s))));
    }

    /** Media campului peste randurile cu valoare; {@code null} = fara date (nu 0). */
    private static Double medie(List<FixtureTeamStats> randuri, Function<FixtureTeamStats, Double> valoare) {
        List<Double> valori = randuri.stream().map(valoare).filter(Objects::nonNull).toList();
        if (valori.isEmpty()) {
            return null;
        }
        return rotunjit(valori.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
    }

    private static Double caDouble(Integer v) {
        return v != null ? v.doubleValue() : null;
    }

    private List<IntalnireDirectaDto> intalniriDirecte(Fixture meci) {
        List<Fixture> directe = fixtures.findHeadToHead(meci.getHomeTeamId(), meci.getAwayTeamId(),
                TERMINAL, meci.getKickoff(), PageRequest.of(0, H2H_LIMIT));
        Map<Long, Team> echipe = teams.findAllById(List.of(meci.getHomeTeamId(), meci.getAwayTeamId()))
                .stream()
                .collect(Collectors.toMap(Team::getId, Function.identity()));
        return directe.stream()
                .map(f -> new IntalnireDirectaDto(
                        f.getId(), f.getKickoff(),
                        echipa(echipe, f.getHomeTeamId()), echipa(echipe, f.getAwayTeamId()),
                        scorFinal(f.getScoreFtHome(), f.getGoalsHome()),
                        scorFinal(f.getScoreFtAway(), f.getGoalsAway())))
                .toList();
    }

    private static EchipaDto echipa(Map<Long, Team> echipe, long id) {
        Team t = echipe.get(id);
        return t != null ? new EchipaDto(t.getId(), t.getName(), t.getLogo()) : new EchipaDto(id, null, null);
    }

    /** Scorul la 90 min: prefera {@code scoreFt} (exclude prelungirile), altfel {@code goals}. */
    private static Integer scorFinal(Integer scoreFt, Integer goals) {
        return scoreFt != null ? scoreFt : goals;
    }

    private static double rotunjit(double valoare) {
        return Math.round(valoare * 100.0) / 100.0;
    }
}
