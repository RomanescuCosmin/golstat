package ro.golstat.api.live;

import ro.golstat.api.prediction.PredictieMeciDto.EchipaDto;
import ro.golstat.api.prediction.PredictieMeciDto.ProcentCota;

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

    /** Un meci din zi: echipe (nume+logo), scor, status, minut, runda si predictia 1X2 (nullable). */
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
            Integer minut,
            String runda,
            Predictie1X2 predictie
    ) {
    }

    /** Sansele 1X2 (procent 0..100 + cota) pentru bara de probabilitate; {@code null} cand nu se poate calcula. */
    public record Predictie1X2(ProcentCota gazde, ProcentCota egal, ProcentCota oaspeti) {
    }
}
