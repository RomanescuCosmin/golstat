package ro.golstat.common.dto;

import java.util.List;

public record FixtureLineupDto(
        Long fixtureId,
        Long teamId,
        String formation,
        Long coachId,
        List<FixtureLineupPlayerDto> players
) {
}
