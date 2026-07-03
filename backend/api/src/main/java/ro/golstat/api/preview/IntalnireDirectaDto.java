package ro.golstat.api.preview;

import ro.golstat.api.prediction.PredictieMeciDto;

import java.time.OffsetDateTime;

/**
 * O intalnire directa din trecut. Gazdele/oaspetii sunt cei ai meciului ISTORIC (se pot inversa
 * fata de meciul previzualizat); scorul prefera {@code scoreFt} (90 min), altfel {@code goals}.
 */
public record IntalnireDirectaDto(
        long fixtureId,
        OffsetDateTime data,
        PredictieMeciDto.EchipaDto gazde,
        PredictieMeciDto.EchipaDto oaspeti,
        Integer golGazde,
        Integer golOaspeti
) {
}
