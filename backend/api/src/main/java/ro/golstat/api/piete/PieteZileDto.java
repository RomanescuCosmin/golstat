package ro.golstat.api.piete;

import ro.golstat.api.prediction.PredictieMeciDto.EchipaDto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Toate meciurile dintr-o fereastra de cateva zile, cu probabilitatile lor pe piete — vederea
 * inversa fata de pagina de meci: aici alegi piata si vezi meciurile, nu invers.
 *
 * <p>Payload-ul e intentionat COMPACT (doar probabilitatea modelata, cota si marimea esantionului,
 * fara frecventele empirice per fereastra): pragul, sortarea si alegerea pietei se fac in client,
 * ca slider-ul de prag sa raspunda instant, fara drum la server.
 */
public record PieteZileDto(List<ZiDto> zile) {

    /** O zi calendaristica (UTC) cu meciurile ei, cronologic. */
    public record ZiDto(LocalDate data, List<MeciPieteDto> meciuri) {
    }

    public record MeciPieteDto(
            long fixtureId,
            OffsetDateTime kickoff,
            LigaDto liga,
            EchipaDto gazde,
            EchipaDto oaspeti,
            List<CotaPiataDto> piete
    ) {
    }

    /**
     * O piata a unui meci. {@code probabilitate} e fractie 0..1 (aceeasi conventie ca
     * {@code StatisticiAvansateDto.LinieDto}), {@code cota} = 1/p, {@code linie} e {@code null} la
     * pietele binare. {@code esantion} = cate meciuri stau in spate; pietele cu esantion 0 nu se
     * trimit deloc.
     */
    public record CotaPiataDto(
            CodPiata piata,
            Double linie,
            double probabilitate,
            double cota,
            int esantion
    ) {
    }

    public record LigaDto(long id, String nume, String logo) {
    }
}
