package ro.golstat.api.prediction;

import org.springframework.stereotype.Service;
import ro.golstat.api.entity.Fixture;
import ro.golstat.api.repository.FixtureRepository;
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
import java.util.List;
import java.util.Optional;

/**
 * Produce predictia unui meci VIITOR (status {@code NS}): construieste forma pe locatie a celor
 * doua echipe ({@link MatchHistoryService}) + media ligii ({@link LeagueAverageService}), le duce in
 * {@link MatchGoalModel} si mapeaza rezultatul in DTO.
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
    private final MatchHistoryService history;
    private final LeagueAverageService leagueAverages;

    public PredictionService(FixtureRepository fixtures, MatchHistoryService history,
                             LeagueAverageService leagueAverages) {
        this.fixtures = fixtures;
        this.history = history;
        this.leagueAverages = leagueAverages;
    }

    /** Predictia unui meci dupa id; goala daca meciul nu exista sau nu e viitor ({@code NS}). */
    public Optional<PredictieMeciDto> predict(long fixtureId) {
        return fixtures.findById(fixtureId)
                .filter(f -> GolstatConstants.FixtureStatus.NOT_STARTED.equals(f.getStatusShort()))
                .map(this::predictFixture);
    }

    /** Predictiile meciurilor viitoare ({@code NS}) ale unei ligi intr-o zi. */
    public List<PredictieMeciDto> upcoming(long leagueId, LocalDate date) {
        OffsetDateTime from = date.atStartOfDay().atOffset(ZoneOffset.UTC);
        return fixtures.findUpcoming(leagueId, GolstatConstants.FixtureStatus.NOT_STARTED, from, from.plusDays(1))
                .stream()
                .map(this::predictFixture)
                .toList();
    }

    private PredictieMeciDto predictFixture(Fixture f) {
        List<MatchSample> homeHistory = history.lastMatches(f.getHomeTeamId(), f.getKickoff(), HISTORY_FETCH);
        List<MatchSample> awayHistory = history.lastMatches(f.getAwayTeamId(), f.getKickoff(), HISTORY_FETCH);
        LeagueAverages avg = leagueAverages.averages(f.getLeagueId(), f.getSeasonYear());
        MatchContext ctx = buildContext(homeHistory, awayHistory, avg);
        return PredictieMeciMapper.toDto(f, MatchGoalModel.predict(ctx));
    }

    static MatchContext buildContext(List<MatchSample> homeHistory, List<MatchSample> awayHistory, LeagueAverages avg) {
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
