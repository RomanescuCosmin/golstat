package ro.golstat.api.live;

import ro.golstat.api.prediction.PredictieMeciDto.EchipaDto;

/** Un meci in DESFASURARE pentru banda live: competitie (nume+logo), echipe (nume+logo), scor, status, minut. */
public record MeciLiveDto(
        long fixtureId,
        Long leagueId,
        String ligaNume,
        String ligaLogo,
        EchipaDto gazde,
        EchipaDto oaspeti,
        Integer golGazde,
        Integer golOaspeti,
        String status,
        Integer minut
) {
}
