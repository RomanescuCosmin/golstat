package ro.golstat.api.stats;

import org.springframework.stereotype.Service;
import ro.golstat.api.repository.FixtureRepository;
import ro.golstat.common.GolstatConstants;

import java.util.List;

/**
 * Media golurilor unei ligi/sezon ("MediaLiga" din model), din meciurile TERMINALE deja ingerate.
 * Cand liga n-are inca meciuri terminale (ex. Campionatul Mondial la primul meci) cade pe un
 * fallback global. Se misca lent — apelantul poate cache-ui.
 */
@Service
public class LeagueAverageService {

    private static final List<String> TERMINAL = List.of(
            GolstatConstants.FixtureStatus.FINISHED,
            GolstatConstants.FixtureStatus.FINISHED_AET,
            GolstatConstants.FixtureStatus.FINISHED_PEN
    );

    /** Medii tipice de fotbal (usor peste 1 gol/echipa), gazdele marcheaza mai mult ca oaspetii. */
    static final double DEFAULT_GAZDE = 1.45;
    static final double DEFAULT_OASPETI = 1.15;

    private final FixtureRepository fixtures;

    public LeagueAverageService(FixtureRepository fixtures) {
        this.fixtures = fixtures;
    }

    public LeagueAverages averages(long leagueId, int season) {
        GoalAverage agg = fixtures.avgGoals(leagueId, season, TERMINAL);
        double gazde = value(agg != null ? agg.getAvgGazde() : null, DEFAULT_GAZDE);
        double oaspeti = value(agg != null ? agg.getAvgOaspeti() : null, DEFAULT_OASPETI);
        return new LeagueAverages(gazde, oaspeti);
    }

    private static double value(Double v, double fallback) {
        return v != null ? v : fallback;
    }
}
