package ro.golstat.common.dto;

import java.time.OffsetDateTime;

public record FixtureDto(
        Long id,
        String referee,
        String timezone,
        OffsetDateTime kickoff,
        Long leagueId,
        Integer seasonYear,
        String round,
        Long venueId,
        String statusLong,
        String statusShort,
        Integer statusElapsed,
        Long homeTeamId,
        Long awayTeamId,
        Integer goalsHome,
        Integer goalsAway,
        Integer scoreHtHome,
        Integer scoreHtAway,
        Integer scoreFtHome,
        Integer scoreFtAway,
        Integer scoreEtHome,
        Integer scoreEtAway,
        Integer scorePenHome,
        Integer scorePenAway
) {
}
