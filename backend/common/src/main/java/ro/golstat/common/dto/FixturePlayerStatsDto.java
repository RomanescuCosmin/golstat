package ro.golstat.common.dto;

import java.math.BigDecimal;

public record FixturePlayerStatsDto(
        Long fixtureId,
        Long teamId,
        Long playerId,
        /** Fara coloana in tabela: alimenteaza doar placeholder-ul de jucator la ingest (ca la InjuryDto). */
        String playerName,
        Integer minutes,
        BigDecimal rating,
        Boolean captain,
        Boolean substitute,
        String position,
        Integer shotsTotal,
        Integer shotsOn,
        Integer goalsTotal,
        Integer goalsConceded,
        Integer goalsAssists,
        Integer goalsSaves,
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
        Integer cardsRed,
        Integer penaltyWon,
        Integer penaltyCommitted,
        Integer penaltyScored,
        Integer penaltyMissed,
        Integer penaltySaved
) {
}
