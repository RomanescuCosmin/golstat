package ro.golstat.common.dto;

public record FixtureEventDto(
        Long fixtureId,
        Long teamId,
        Long playerId,
        Long assistId,
        Integer timeElapsed,
        Integer timeExtra,
        String type,
        String detail,
        String comments
) {
}
