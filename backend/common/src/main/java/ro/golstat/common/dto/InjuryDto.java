package ro.golstat.common.dto;

import java.time.LocalDate;

public record InjuryDto(
        Long playerId,
        String playerName,
        Long teamId,
        Long fixtureId,
        Long leagueId,
        Integer seasonYear,
        String type,
        String reason,
        LocalDate reportedAt
) {
}
