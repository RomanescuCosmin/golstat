package ro.golstat.api.stats;

import org.springframework.stereotype.Service;
import ro.golstat.api.repository.FixtureTeamStatsRepository;
import ro.golstat.common.GolstatConstants;

import java.util.List;

/**
 * Mediile de cornere/faulturi/cartonase ale unei ligi/sezon, din meciurile TERMINALE cu statistici
 * colectate. DB-ul da media PE ECHIPA; modelele cer media TOTALULUI pe meci, deci dublam.
 * Fara date (sau liga/sezon necunoscute pe fixture) → fallback global.
 */
@Service
public class CountLeagueAverageService {

    private static final List<String> TERMINAL = List.of(
            GolstatConstants.FixtureStatus.FINISHED,
            GolstatConstants.FixtureStatus.FINISHED_AET,
            GolstatConstants.FixtureStatus.FINISHED_PEN
    );

    /** Totaluri tipice de fotbal pe meci (ambele echipe). */
    static final double DEFAULT_CORNERE = 10.0;
    static final double DEFAULT_FAULTURI = 24.0;
    static final double DEFAULT_CARTONASE = 4.0;

    private final FixtureTeamStatsRepository teamStats;

    public CountLeagueAverageService(FixtureTeamStatsRepository teamStats) {
        this.teamStats = teamStats;
    }

    public CountLeagueAverages averages(Long leagueId, Integer season) {
        if (leagueId == null || season == null) {
            return new CountLeagueAverages(DEFAULT_CORNERE, DEFAULT_FAULTURI, DEFAULT_CARTONASE);
        }
        CountAverage agg = teamStats.avgCounts(leagueId, season, TERMINAL);
        return new CountLeagueAverages(
                total(agg != null ? agg.getAvgCornere() : null, DEFAULT_CORNERE),
                total(agg != null ? agg.getAvgFaulturi() : null, DEFAULT_FAULTURI),
                total(agg != null ? agg.getAvgCartonase() : null, DEFAULT_CARTONASE));
    }

    private static double total(Double perEchipa, double fallback) {
        return perEchipa != null ? 2 * perEchipa : fallback;
    }
}
