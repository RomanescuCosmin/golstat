package ro.golstat.common.dto;

/** Statisticile unei echipe intr-o REPRIZA a unui meci ({@code half}: 1 sau 2). */
public record FixtureTeamHalfStatsDto(
        Long fixtureId,
        Long teamId,
        Integer half,
        Integer cornerKicks,
        Integer fouls,
        Integer yellowCards,
        Integer redCards,
        Integer shotsTotal,
        Integer shotsOnGoal,
        Integer offsides
) {
}
