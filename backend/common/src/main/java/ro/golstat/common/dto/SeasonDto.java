package ro.golstat.common.dto;

import java.time.LocalDate;

public record SeasonDto(
        Long leagueId,
        Integer year,
        LocalDate startDate,
        LocalDate endDate,
        Boolean isCurrent,
        Boolean hasEvents,
        Boolean hasLineups,
        Boolean hasStatisticsFixtures,
        Boolean hasStatisticsPlayers,
        Boolean hasStandings
) {
}
