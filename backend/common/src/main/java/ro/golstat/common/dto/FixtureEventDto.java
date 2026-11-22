package ro.golstat.common.dto;

/**
 * Un eveniment de meci. La schimbare {@code playerId} = cine iese, {@code assistId} = cine intra.
 * {@code playerName} / {@code assistName} vin cand furnizorul le da in acelasi raspuns (bzzoiro):
 * jucatorul poate sa nu existe inca in DB, iar fara nume cronologia ar arata doar „—".
 */
public record FixtureEventDto(
        Long fixtureId,
        Long teamId,
        Long playerId,
        Long assistId,
        Integer timeElapsed,
        Integer timeExtra,
        String type,
        String detail,
        String comments,
        String playerName,
        String assistName
) {
    public FixtureEventDto(Long fixtureId, Long teamId, Long playerId, Long assistId, Integer timeElapsed,
                           Integer timeExtra, String type, String detail, String comments) {
        this(fixtureId, teamId, playerId, assistId, timeElapsed, timeExtra, type, detail, comments, null, null);
    }
}
