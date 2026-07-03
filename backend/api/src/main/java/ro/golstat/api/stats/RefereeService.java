package ro.golstat.api.stats;

import org.springframework.stereotype.Service;
import ro.golstat.api.repository.FixtureTeamStatsRepository;
import ro.golstat.common.GolstatConstants;
import ro.golstat.stats.cards.RefereeFactor;

import java.util.List;

/**
 * Factorul de arbitru pentru piata de cartonase, calculat din meciurile TERMINALE deja colectate
 * ({@code fixture.referee} + cartonasele din {@code fixture_team_stats}). Arbitru necunoscut sau
 * fara istoric → {@link RefereeFactor#NEUTRAL}.
 */
@Service
public class RefereeService {

    private static final List<String> TERMINAL = List.of(
            GolstatConstants.FixtureStatus.FINISHED,
            GolstatConstants.FixtureStatus.FINISHED_AET,
            GolstatConstants.FixtureStatus.FINISHED_PEN
    );

    private final FixtureTeamStatsRepository teamStats;

    public RefereeService(FixtureTeamStatsRepository teamStats) {
        this.teamStats = teamStats;
    }

    /** @param leagueAvgCards media TOTALULUI de cartonase pe meci a ligii */
    public double factor(String referee, double leagueAvgCards) {
        if (referee == null || referee.isBlank()) {
            return RefereeFactor.NEUTRAL;
        }
        RefereeCardAverage agg = teamStats.refereeCardAverage(referee, TERMINAL);
        if (agg == null || agg.getAvgCards() == null
                || agg.getMatches() == null || agg.getMatches() == 0) {
            return RefereeFactor.NEUTRAL;
        }
        return RefereeFactor.of(agg.getAvgCards(), agg.getMatches(), leagueAvgCards);
    }
}
