package ro.golstat.common.dto;

public record FixtureLineupPlayerDto(
        Long fixtureId,
        Long teamId,
        Long playerId,
        String playerName,
        Integer number,
        String position,
        String grid,
        Boolean isSubstitute
) {
}
