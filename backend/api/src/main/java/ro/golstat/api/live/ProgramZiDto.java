package ro.golstat.api.live;

import ro.golstat.api.prediction.PredictieMeciDto.EchipaDto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Meciurile unei zile (orice status), grupate pe competitie — sursa pentru prima pagina stil flashscore.
 * Fata de {@link ProgramDto}, fiecare meci poarta scor + status (live/terminat/programat), ca sa poata fi
 * randat direct: ora la {@code NS}, minut+scor la live, scor final la terminat.
 */
public record ProgramZiDto(LocalDate data, List<Liga> ligi) {

    /** O competitie cu meciurile ei din ziua respectiva; nume/tara/logo pot lipsi daca liga nu e in DB. */
    public record Liga(long leagueId, String nume, String tara, String logo, List<Meci> meciuri) {
    }

    /** Un meci din zi: echipe (nume+logo), scor, status, minut si daca e live/terminat. */
    public record Meci(
            long fixtureId,
            OffsetDateTime kickoff,
            EchipaDto gazde,
            EchipaDto oaspeti,
            Integer golGazde,
            Integer golOaspeti,
            String status,
            boolean inDesfasurare,
            boolean terminat,
            Integer minut
    ) {
    }
}
