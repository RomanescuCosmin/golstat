package ro.golstat.common.dto;

public record StandingDto(
        Long leagueId,
        Integer seasonYear,
        Long teamId,
        Integer rank,
        String groupName,
        Integer points,
        Integer goalsDiff,
        String form,
        String status,
        String description,
        Integer playedAll,
        Integer winAll,
        Integer drawAll,
        Integer loseAll,
        Integer goalsForAll,
        Integer goalsAgainstAll,
        Integer playedHome,
        Integer winHome,
        Integer drawHome,
        Integer loseHome,
        Integer goalsForHome,
        Integer goalsAgainstHome,
        Integer playedAway,
        Integer winAway,
        Integer drawAway,
        Integer loseAway,
        Integer goalsForAway,
        Integer goalsAgainstAway
) {
}
