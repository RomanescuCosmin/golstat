package ro.golstat.common.dto;

import java.math.BigDecimal;

public record PlayerSeasonStatsDto(
        Long playerId,
        Long teamId,
        Long leagueId,
        Integer seasonYear,
        String position,
        Integer appearances,
        Integer lineups,
        Integer minutes,
        BigDecimal rating,
        Boolean captain,
        Integer goalsTotal,
        Integer goalsConceded,
        Integer goalsAssists,
        Integer goalsSaves,
        Integer shotsTotal,
        Integer shotsOn,
        Integer passesTotal,
        Integer passesKey,
        Integer passesAccuracy,
        Integer tacklesTotal,
        Integer tacklesBlocks,
        Integer tacklesIntercep,
        Integer duelsTotal,
        Integer duelsWon,
        Integer dribblesAttempts,
        Integer dribblesSuccess,
        Integer foulsDrawn,
        Integer foulsCommitted,
        Integer cardsYellow,
        Integer cardsYellowred,
        Integer cardsRed,
        Integer penaltyWon,
        Integer penaltyCommitted,
        Integer penaltyScored,
        Integer penaltyMissed,
        Integer penaltySaved
) {
}
