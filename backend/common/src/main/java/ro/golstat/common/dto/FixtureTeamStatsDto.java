package ro.golstat.common.dto;

import java.math.BigDecimal;

public record FixtureTeamStatsDto(
        Long fixtureId,
        Long teamId,
        Integer shotsOnGoal,
        Integer shotsOffGoal,
        Integer shotsTotal,
        Integer shotsBlocked,
        Integer shotsInsidebox,
        Integer shotsOutsidebox,
        Integer fouls,
        Integer cornerKicks,
        Integer offsides,
        BigDecimal ballPossession,
        Integer yellowCards,
        Integer redCards,
        Integer goalkeeperSaves,
        Integer passesTotal,
        Integer passesAccurate,
        BigDecimal passesPercentage,
        BigDecimal expectedGoals
) {
}
