package ro.golstat.stats.scorers;

/**
 * Predictia de marcator pentru un jucator: rata proprie {@code lambdaPlayer}, probabilitatea de
 * a marca oricand in meci si probabilitatea de a marca cel putin doua goluri (brace).
 * Cota statistica se deriva la afisare din {@code Odds.fromProbability(anytimeProbability)}.
 */
public record ScorerPrediction(
        long playerId,
        double lambdaPlayer,
        double anytimeProbability,
        double braceProbability
) {
}
