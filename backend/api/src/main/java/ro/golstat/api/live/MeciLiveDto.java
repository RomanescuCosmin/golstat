package ro.golstat.api.live;

import ro.golstat.api.prediction.PredictieMeciDto.EchipaDto;

/** Un meci in DESFASURARE pentru banda live: echipe (nume+logo), scor curent, status, minut. */
public record MeciLiveDto(
        long fixtureId,
        Long leagueId,
        EchipaDto gazde,
        EchipaDto oaspeti,
        Integer golGazde,
        Integer golOaspeti,
        String status,
        Integer minut
) {
}
