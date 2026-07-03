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
import ro.golstat.common.GolstatConstants;
import ro.golstat.stats.cards.CardMarket;
import ro.golstat.stats.counts.EventLineBlend;
import ro.golstat.stats.market.OverUnder;
import ro.golstat.stats.model.EventCountSample;
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
 * ({@link PredictionService}) + forma ultimelor meciuri per echipa (fara filtru de locatie,
 * ca badge-urile V/E/I) + intalnirile directe + pietele numarabile (cornere/faulturi/cartonase,
 * pe totalul meciului, fereastra combinata a ambelor echipe) + statisticile cheie.
 */
@Service
public class MatchPreviewService {

    /** Fereastra din care calculam mediile de goluri pe meci. */
    static final int FEREASTRA_MEDII = 10;
    /** Cate meciuri afisam ca badge-uri V/E/I. */
    static final int FEREASTRA_FORMA = 5;
    /** Cate intalniri directe afisam. */
    static final int H2H_LIMIT = 5;
    /** Dispersiile Negative Binomial calibrate in stats-engine (vezi EventLineBlendTest / CardMarketTest). */
    static final double DISPERSIE_FAULTURI = 30;
    static final double DISPERSIE_CARTONASE = 8;
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
    private final RefereeService referees;
    private final TeamRepository teams;
    private final FixtureLineupRepository lineups;
    private final FixtureLineupPlayerRepository lineupPlayers;
    private final InjuryRepository injuries;
    private final PlayerRepository players;

    public MatchPreviewService(PredictionService predictions, FixtureRepository fixtures,
                               MatchHistoryService history, StatsHistoryService statsHistory,
                               CountLeagueAverageService countAverages, RefereeService referees,
                               TeamRepository teams,
                               FixtureLineupRepository lineups, FixtureLineupPlayerRepository lineupPlayers,
                               InjuryRepository injuries, PlayerRepository players) {
        this.predictions = predictions;
        this.fixtures = fixtures;
        this.history = history;
        this.statsHistory = statsHistory;
        this.countAverages = countAverages;
        this.referees = referees;
        this.teams = teams;
        this.lineups = lineups;
        this.lineupPlayers = lineupPlayers;
        this.injuries = injuries;
        this.players = players;
    }

    /** Previzualizarea unui meci dupa id; {@link PredictionNotFoundException} daca nu are predictie. */
    public PrevizualizareMeciDto previzualizare(long fixtureId) {
        PredictieMeciDto predictie = predictions.predict(fixtureId)
                .orElseThrow(() -> new PredictionNotFoundException(fixtureId));
        Fixture meci = fixtures.findById(fixtureId)
                .orElseThrow(() -> new PredictionNotFoundException(fixtureId));

        FormaEchipaDto formaGazde =
                forma(history.lastMatches(meci.getHomeTeamId(), meci.getKickoff(), FEREASTRA_MEDII));
        FormaEchipaDto formaOaspeti =
                forma(history.lastMatches(meci.getAwayTeamId(), meci.getKickoff(), FEREASTRA_MEDII));

        IstoricCounturi counturiGazde =
                statsHistory.istoric(meci.getHomeTeamId(), meci.getKickoff(), FEREASTRA_MEDII);
        IstoricCounturi counturiOaspeti =
                statsHistory.istoric(meci.getAwayTeamId(), meci.getKickoff(), FEREASTRA_MEDII);
        CountLeagueAverages mediiLiga = countAverages.averages(meci.getLeagueId(), meci.getSeasonYear());
        double factorArbitru = referees.factor(meci.getReferee(), mediiLiga.cartonasePeMeci());

        return new PrevizualizareMeciDto(predictie, formaGazde, formaOaspeti, intalniriDirecte(meci),
                cornere(counturiGazde, counturiOaspeti, mediiLiga),
                faulturi(counturiGazde, counturiOaspeti, mediiLiga),
                cartonase(counturiGazde, counturiOaspeti, mediiLiga, factorArbitru),
                new StatisticiCheieDto(statisticiEchipa(counturiGazde.statisticiEchipa()),
                        statisticiEchipa(counturiOaspeti.statisticiEchipa())),
                echipeDeStart(meci));
    }

    /** {@code null} cat timp nu avem lineup pentru AMBELE echipe (sursa le anunta impreuna). */
    private EchipaDeStartDto echipeDeStart(Fixture meci) {
        Map<Long, FixtureLineup> perEchipa = lineups.findByFixtureId(meci.getId()).stream()
                .filter(l -> l.getTeamId() != null)
                .collect(Collectors.toMap(FixtureLineup::getTeamId, Function.identity()));
        FixtureLineup gazde = perEchipa.get(meci.getHomeTeamId());
        FixtureLineup oaspeti = perEchipa.get(meci.getAwayTeamId());
        if (gazde == null || oaspeti == null) {
            return null;
        }
        Map<Long, List<FixtureLineupPlayer>> jucatori = lineupPlayers.findByFixtureId(meci.getId()).stream()
                .filter(p -> p.getTeamId() != null)
                .collect(Collectors.groupingBy(FixtureLineupPlayer::getTeamId));
        Map<Long, List<EchipaDeStartDto.IndisponibilDto>> indisponibili = indisponibili(meci);
        return new EchipaDeStartDto(
                echipaLineup(gazde, jucatori.getOrDefault(meci.getHomeTeamId(), List.of()),
                        indisponibili.getOrDefault(meci.getHomeTeamId(), List.of())),
                echipaLineup(oaspeti, jucatori.getOrDefault(meci.getAwayTeamId(), List.of()),
                        indisponibili.getOrDefault(meci.getAwayTeamId(), List.of())),
                meci.getReferee());
    }

    private static EchipaDeStartDto.EchipaLineupDto echipaLineup(FixtureLineup lineup,
                                                                 List<FixtureLineupPlayer> jucatori,
                                                                 List<EchipaDeStartDto.IndisponibilDto> indisponibili) {
        return new EchipaDeStartDto.EchipaLineupDto(
                lineup.getFormation(),
                jucatoriDto(jucatori, false),
                jucatoriDto(jucatori, true),
                indisponibili);
    }

    private static List<EchipaDeStartDto.JucatorDto> jucatoriDto(List<FixtureLineupPlayer> jucatori,
                                                                 boolean rezerve) {
        return jucatori.stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsSubstitute()) == rezerve)
                .map(p -> new EchipaDeStartDto.JucatorDto(p.getPlayerId(), p.getPlayerName(),
                        p.getNumber(), p.getPosition(), p.getGrid()))
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

    private static List<OverUnder> cornere(IstoricCounturi gazde, IstoricCounturi oaspeti,
                                           CountLeagueAverages liga) {
        return EventLineBlend.poisson(concat(gazde.cornere(), oaspeti.cornere()),
                liga.cornerePeMeci(), EventLineBlend.STANDARD_CORNER_LINES).lines();
    }

    private static List<OverUnder> faulturi(IstoricCounturi gazde, IstoricCounturi oaspeti,
                                            CountLeagueAverages liga) {
        return EventLineBlend.overDispersed(concat(gazde.faulturi(), oaspeti.faulturi()),
                liga.faulturiPeMeci(), DISPERSIE_FAULTURI, EventLineBlend.STANDARD_FOUL_LINES).lines();
    }

    private static List<OverUnder> cartonase(IstoricCounturi gazde, IstoricCounturi oaspeti,
                                             CountLeagueAverages liga, double factorArbitru) {
        return CardMarket.of(concat(gazde.cartonase(), oaspeti.cartonase()),
                liga.cartonasePeMeci(), factorArbitru, DISPERSIE_CARTONASE,
                CardMarket.STANDARD_LINES).lines();
    }

    private static List<EventCountSample> concat(List<EventCountSample> a, List<EventCountSample> b) {
        return Stream.concat(a.stream(), b.stream()).toList();
    }

    static StatisticiCheieDto.StatisticiEchipaDto statisticiEchipa(List<FixtureTeamStats> randuri) {
        return new StatisticiCheieDto.StatisticiEchipaDto(
                medie(randuri, s -> s.getBallPossession() != null ? s.getBallPossession().doubleValue() : null),
                medie(randuri, s -> caDouble(s.getShotsTotal())),
                medie(randuri, s -> caDouble(s.getShotsOnGoal())),
                medie(randuri, s -> caDouble(s.getCornerKicks())),
                medie(randuri, s -> caDouble(StatsHistoryService.totalCartonase(s))));
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

    static FormaEchipaDto forma(List<MatchSample> istoric) {
        List<FormaMeciDto> meciuri = istoric.stream()
                .limit(FEREASTRA_FORMA)
                .map(MatchPreviewService::formaMeci)
                .toList();
        double marcate = istoric.stream().mapToInt(MatchSample::goalsFor).average().orElse(0.0);
        double primite = istoric.stream().mapToInt(MatchSample::goalsAgainst).average().orElse(0.0);
        return new FormaEchipaDto(meciuri, rotunjit(marcate), rotunjit(primite));
    }

    private static FormaMeciDto formaMeci(MatchSample s) {
        String rezultat = s.goalsFor() > s.goalsAgainst() ? "V"
                : s.goalsFor() == s.goalsAgainst() ? "E" : "I";
        return new FormaMeciDto(s.date(), s.home(), s.goalsFor(), s.goalsAgainst(), rezultat);
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
