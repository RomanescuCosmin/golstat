package ro.golstat.common.dto;

import java.math.BigDecimal;

public record TeamSeasonStatsDto(
        Long teamId,
        Long leagueId,
        Integer seasonYear,
        String form,
        Integer playedHome,
        Integer playedAway,
        Integer playedTotal,
        Integer winsHome,
        Integer winsAway,
        Integer winsTotal,
        Integer drawsHome,
        Integer drawsAway,
        Integer drawsTotal,
        Integer losesHome,
        Integer losesAway,
        Integer losesTotal,
        Integer goalsForHome,
        Integer goalsForAway,
        Integer goalsForTotal,
        BigDecimal goalsForAvgHome,
        BigDecimal goalsForAvgAway,
        BigDecimal goalsForAvgTotal,
        Integer goalsAgainstHome,
        Integer goalsAgainstAway,
        Integer goalsAgainstTotal,
        BigDecimal goalsAgainstAvgHome,
        BigDecimal goalsAgainstAvgAway,
        BigDecimal goalsAgainstAvgTotal,
        Integer cleanSheetHome,
        Integer cleanSheetAway,
        Integer cleanSheetTotal,
        Integer failedToScoreHome,
        Integer failedToScoreAway,
        Integer failedToScoreTotal,
        Integer yellowCardsTotal,
        Integer redCardsTotal
) {
}
