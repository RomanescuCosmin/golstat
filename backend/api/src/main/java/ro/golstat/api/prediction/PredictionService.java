package ro.golstat.api.prediction;

import org.springframework.stereotype.Service;
import ro.golstat.api.entity.Fixture;
import ro.golstat.api.entity.Team;
import ro.golstat.api.repository.FixtureRepository;
import ro.golstat.api.repository.TeamRepository;
import ro.golstat.api.stats.LeagueAverageService;
import ro.golstat.api.stats.LeagueAverages;
import ro.golstat.api.stats.MatchHistoryService;
import ro.golstat.common.GolstatConstants;
import ro.golstat.stats.form.MatchWindow;
import ro.golstat.stats.goals.GoalForm;
import ro.golstat.stats.match.MatchContext;
import ro.golstat.stats.match.MatchGoalModel;
import ro.golstat.stats.model.MatchLocation;
import ro.golstat.stats.model.MatchSample;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Produce predictia unui meci viitor ({@code NS}) sau terminat ({@code FT}/{@code AET}/{@code PEN}):
 * construieste forma pe locatie a celor doua echipe ({@link MatchHistoryService}) + media ligii
 * ({@link LeagueAverageService}), le duce in {@link MatchGoalModel} si mapeaza rezultatul in DTO. La
 * meciuri terminate mapper-ul ataseaza si scorul real, pentru validarea predictiei.
 *
 * <p>Fallback teren neutru / esantion mic (ex. Campionatul Mondial): daca oricare fereastra pe
 * locatie e sub {@link #MIN_LOCATION}, se folosesc ferestrele fara filtru de locatie si media ligii
 * impartita egal (avantaj de teren zero). Motorul ramane agnostic — primeste doar numere.
 */
@Service
public class PredictionService {

    /** Cate meciuri tragem din istoric, ca sa ramana destule dupa filtrul de locatie. */
    static final int HISTORY_FETCH = 40;
    /** Marimea ferestrei pe locatie (ultimele N acasa / deplasare). */
    static final int WINDOW = 10;
    /** Sub atatea meciuri pe locatie → cadem pe teren neutru. */
    static final int MIN_LOCATION = 3;

    private final FixtureRepository fixtures;
    private final TeamRepository teams;
    private final MatchHistoryService history;
    private final LeagueAverageService leagueAverages;

    public PredictionService(FixtureRepository fixtures, TeamRepository teams, MatchHistoryService history,
                             LeagueAverageService leagueAverages) {
        this.fixtures = fixtures;
        this.teams = teams;
        this.history = history;
        this.leagueAverages = leagueAverages;
    }

    /**
     * Predictia unui meci dupa id; goala daca meciul nu exista sau e in desfasurare. Se produce pentru
     * meciuri viitoare ({@code NS}) si pentru cele terminate ({@code FT}/{@code AET}/{@code PEN}) — la
     * cele terminate predictia se compara cu rezultatul real (validarea modelului). Istoricul foloseste
     * doar meciuri dinainte de kickoff, deci predictia retrospectiva e identica cu cea pre-meci.
     */
    public Optional<PredictieMeciDto> predict(long fixtureId) {
        return fixtures.findById(fixtureId)
                .filter(f -> arePredictie(f.getStatusShort()))
                .map(f -> predictFixture(f, teamsById(List.of(f))));
    }

    /** Meciurile viitoare ({@code NS}) si cele terminate au predictie; cele in desfasurare nu (rezultat partial). */
    private static boolean arePredictie(String statusShort) {
        if (statusShort == null) {
            return false;
        }
        return GolstatConstants.FixtureStatus.NOT_STARTED.equals(statusShort)
                || GolstatConstants.FixtureStatus.TERMINAL.contains(statusShort);
    }

    /** Predictiile meciurilor viitoare ({@code NS}) ale unei ligi intr-o zi. */
    public List<PredictieMeciDto> upcoming(long leagueId, LocalDate date) {
        OffsetDateTime from = date.atStartOfDay().atOffset(ZoneOffset.UTC);
        List<Fixture> found =
                fixtures.findUpcoming(leagueId, GolstatConstants.FixtureStatus.NOT_STARTED, from, from.plusDays(1));
        Map<Long, Team> echipe = teamsById(found);
        return found.stream()
                .map(f -> predictFixture(f, echipe))
                .toList();
    }

    /**
     * 1X2 (+ goluri) pentru o LISTA de meciuri, orice status — pentru vederea grupata pe zi.
     * Optimizat sa fie ieftin pe o zi intreaga: istoric FARA rang (evita N+1-ul pe {@code standings},
     * rangul nu intra in modelul 1X2) si media ligii cache-uita per {@code (liga, sezon)}. Meciurile
     * fara echipe/kickoff/liga/sezon se sar. {@code echipe} = harta partajata de la apelant (fara N+1 de echipe).
     */
    public Map<Long, PredictieMeciDto> predictBatch(List<Fixture> fixturi, Map<Long, Team> echipe) {
        Map<String, LeagueAverages> avgCache = new HashMap<>();
        Map<Long, PredictieMeciDto> out = new HashMap<>();
        for (Fixture f : fixturi) {
            if (f.getHomeTeamId() == null || f.getAwayTeamId() == null || f.getKickoff() == null
                    || f.getLeagueId() == null || f.getSeasonYear() == null) {
                continue;
            }
            List<MatchSample> home = history.lastMatchesNoRank(f.getHomeTeamId(), f.getKickoff(), HISTORY_FETCH);
            List<MatchSample> away = history.lastMatchesNoRank(f.getAwayTeamId(), f.getKickoff(), HISTORY_FETCH);
            LeagueAverages avg = avgCache.computeIfAbsent(
                    f.getLeagueId() + ":" + f.getSeasonYear(),
                    k -> leagueAverages.averages(f.getLeagueId(), f.getSeasonYear()));
            MatchContext ctx = buildContext(home, away, avg);
            out.put(f.getId(), PredictieMeciMapper.toDto(f, MatchGoalModel.predict(ctx),
                    echipe.get(f.getHomeTeamId()), echipe.get(f.getAwayTeamId())));
        }
        return out;
    }

    /** Un singur query pentru toate echipele meciurilor date (evita N+1). */
    private Map<Long, Team> teamsById(List<Fixture> found) {
        List<Long> ids = found.stream()
                .flatMap(f -> Stream.of(f.getHomeTeamId(), f.getAwayTeamId()))
                .distinct()
                .toList();
        return teams.findAllById(ids).stream()
                .collect(Collectors.toMap(Team::getId, Function.identity()));
    }

    private PredictieMeciDto predictFixture(Fixture f, Map<Long, Team> echipe) {
        List<MatchSample> homeHistory = history.lastMatches(f.getHomeTeamId(), f.getKickoff(), HISTORY_FETCH);
        List<MatchSample> awayHistory = history.lastMatches(f.getAwayTeamId(), f.getKickoff(), HISTORY_FETCH);
        LeagueAverages avg = leagueAverages.averages(f.getLeagueId(), f.getSeasonYear());
        MatchContext ctx = buildContext(homeHistory, awayHistory, avg);
        return PredictieMeciMapper.toDto(f, MatchGoalModel.predict(ctx),
                echipe.get(f.getHomeTeamId()), echipe.get(f.getAwayTeamId()));
    }

    /** Public: si lista pe zile ({@code PieteZileService}) il construieste, din istoricul deja incarcat. */
    public static MatchContext buildContext(List<MatchSample> homeHistory, List<MatchSample> awayHistory,
                                            LeagueAverages avg) {
        List<MatchSample> gazdaAcasa = MatchWindow.lastN(homeHistory, WINDOW, MatchLocation.HOME);
        List<MatchSample> oaspetiDeplasare = MatchWindow.lastN(awayHistory, WINDOW, MatchLocation.AWAY);

        if (gazdaAcasa.size() >= MIN_LOCATION && oaspetiDeplasare.size() >= MIN_LOCATION) {
            return new MatchContext(GoalForm.of(gazdaAcasa), GoalForm.of(oaspetiDeplasare),
                    avg.mediaLigaGazde(), avg.mediaLigaOaspeti());
        }

        // teren neutru: fara filtru de locatie, avantaj de teren zero
        List<MatchSample> gazdaOricare = MatchWindow.lastN(homeHistory, WINDOW);
        List<MatchSample> oaspetiOricare = MatchWindow.lastN(awayHistory, WINDOW);
        double neutru = (avg.mediaLigaGazde() + avg.mediaLigaOaspeti()) / 2.0;
        return new MatchContext(GoalForm.of(gazdaOricare), GoalForm.of(oaspetiOricare), neutru, neutru);
    }
}
