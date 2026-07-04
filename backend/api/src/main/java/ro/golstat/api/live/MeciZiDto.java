package ro.golstat.api.live;

import ro.golstat.api.prediction.PredictieMeciDto.EchipaDto;

import java.time.OffsetDateTime;

/** Un meci dintr-o zi (orice status): echipe, scor curent/final, status si minut — pentru lista zilei. */
public record MeciZiDto(
        long fixtureId,
        Long leagueId,
        EchipaDto gazde,
        EchipaDto oaspeti,
        Integer golGazde,
        Integer golOaspeti,
        String status,
        Integer minut,
        boolean inDesfasurare,
        boolean terminat,
        OffsetDateTime kickoff
) {
}
