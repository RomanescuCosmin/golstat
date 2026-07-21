package ro.golstat.api.preview;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
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
import ro.golstat.api.preview.StatisticiAvansateBuilder.FerestreEchipa;
import ro.golstat.api.repository.FixtureEventRepository;
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
import ro.golstat.stats.form.MatchWindow;
import ro.golstat.stats.model.MatchLocation;
import ro.golstat.stats.model.MatchSample;

import java.time.LocalDate;
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

    public MatchPreviewService(PredictionService predictions, FixtureRepository fixtures,
                               MatchHistoryService history, StatsHistoryService statsHistory,
                               CountLeagueAverageService countAverages, LeagueAverageService leagueAverages,
                               RefereeService referees, TeamRepository teams,
                               FixtureLineupRepository lineups, FixtureLineupPlayerRepository lineupPlayers,
                               InjuryRepository injuries, PlayerRepository players,
                               FixtureTeamStatsRepository teamStats, FixtureEventRepository events) {
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
        PredictieMeciDto predictie = predictions.predict(fixtureId)
                .orElseThrow(() -> new PredictionNotFoundException(fixtureId));
        Fixture meci = fixtures.findById(fixtureId)
                .orElseThrow(() -> new PredictionNotFoundException(fixtureId));

        List<MatchSample> istoricGazde =
                history.lastMatches(meci.getHomeTeamId(), meci.getKickoff(), HISTORY_FETCH);
        List<MatchSample> istoricOaspeti =
                history.lastMatches(meci.getAwayTeamId(), meci.getKickoff(), HISTORY_FETCH);
        IstoricCounturi counturiGazde =
                statsHistory.istoric(meci.getHomeTeamId(), meci.getKickoff(), HISTORY_FETCH);
        IstoricCounturi counturiOaspeti =
                statsHistory.istoric(meci.getAwayTeamId(), meci.getKickoff(), HISTORY_FETCH);
        CountLeagueAverages mediiCounturi = countAverages.averages(meci.getLeagueId(), meci.getSeasonYear());
        LeagueAverages mediiGoluri = leagueAverages.averages(meci.getLeagueId(), meci.getSeasonYear());
        double factorArbitru = referees.factor(meci.getReferee(), mediiCounturi.cartonasePeMeci());

        StatisticiAvansateDto statistici = StatisticiAvansateBuilder.build(
                ferestre(istoricGazde, counturiGazde, MatchLocation.HOME),
                ferestre(istoricOaspeti, counturiOaspeti, MatchLocation.AWAY),
                mediiCounturi, mediiGoluri.mediaLigaGazde() + mediiGoluri.mediaLigaOaspeti(),
                factorArbitru, egalModel(predictie), rezultatStatistici(meci));

        return new PrevizualizareMeciDto(predictie,
                forma(istoricGazde, MatchLocation.HOME),
                forma(istoricOaspeti, MatchLocation.AWAY),
                intalniriDirecte(meci),
                statistici,
                new StatisticiCheieDto(statisticiEchipa(counturiGazde.statisticiEchipa()),
                        statisticiEchipa(counturiOaspeti.statisticiEchipa())),
                echipeDeStart(meci));
    }

    /** Ferestrele unei echipe pentru analiza pe piete: ultimele {@value #FEREASTRA}, locatie + general. */
    private static FerestreEchipa ferestre(List<MatchSample> istoric, IstoricCounturi counturi,
                                           MatchLocation locatie) {
        return StatisticiAvansateBuilder.ferestre(istoric, counturi, locatie, FEREASTRA);
    }

    /** P(egal la final) 0..1 din modelul de goluri al meciului; null cand predictia nu o are. */
    private static Double egalModel(PredictieMeciDto predictie) {
        return predictie.egal() != null ? predictie.egal().procent() / 100.0 : null;
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
                totalCount(gazdeStats, oaspetiStats, FixtureTeamStats::getShotsOnGoal));
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
        return new FormaEchipaDto(
                fereastraForma(MatchWindow.lastN(istoric, FEREASTRA, locatie)),
                fereastraForma(MatchWindow.lastN(istoric, FEREASTRA)));
    }

    private static FormaEchipaDto.FereastraFormaDto fereastraForma(List<MatchSample> fereastra) {
        List<FormaMeciDto> meciuri = fereastra.stream()
                .map(MatchPreviewService::formaMeci)
                .toList();
        double marcate = fereastra.stream().mapToInt(MatchSample::goalsFor).average().orElse(0.0);
        double primite = fereastra.stream().mapToInt(MatchSample::goalsAgainst).average().orElse(0.0);
        return new FormaEchipaDto.FereastraFormaDto(meciuri, rotunjit(marcate), rotunjit(primite));
    }

    private static FormaMeciDto formaMeci(MatchSample s) {
        String rezultat = s.goalsFor() > s.goalsAgainst() ? "V"
                : s.goalsFor() == s.goalsAgainst() ? "E" : "I";
        return new FormaMeciDto(s.date(), s.home(), s.goalsFor(), s.goalsAgainst(), rezultat);
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

    private static EchipaDeStartDto.EchipaLineupDto echipaLineup(LineupEchipa echipa,
                                                                 Map<Long, String> fotografii,
                                                                 List<EchipaDeStartDto.IndisponibilDto> indisponibili) {
        return new EchipaDeStartDto.EchipaLineupDto(
                echipa.lineup().getFormation(),
                jucatoriDto(echipa.jucatori(), false, fotografii),
                jucatoriDto(echipa.jucatori(), true, fotografii),
                indisponibili);
    }

    private static List<EchipaDeStartDto.JucatorDto> jucatoriDto(List<FixtureLineupPlayer> jucatori,
                                                                 boolean rezerve,
                                                                 Map<Long, String> fotografii) {
        return jucatori.stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsSubstitute()) == rezerve)
                .map(p -> new EchipaDeStartDto.JucatorDto(p.getPlayerId(), p.getPlayerName(),
                        p.getNumber(), p.getPosition(), p.getGrid(),
                        p.getPlayerId() != null ? fotografii.get(p.getPlayerId()) : null))
                .toList();
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
