package ro.golstat.api.live;

import ro.golstat.api.prediction.PredictieMeciDto.EchipaDto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/** Program de meciuri viitoare, grupat pe zi (UTC) apoi pe competitie. */
public record ProgramDto(List<Zi> zile) {

    /** O zi de program cu competitiile care au meciuri in ea. */
    public record Zi(LocalDate data, List<Liga> ligi) {
    }

    /** O competitie dintr-o zi cu meciurile ei; nume/tara/logo pot lipsi daca liga nu e in DB. */
    public record Liga(long leagueId, String nume, String tara, String logo, List<Meci> meciuri) {
    }

    /** Un meci viitor din program. */
    public record Meci(long fixtureId, OffsetDateTime kickoff, EchipaDto gazde, EchipaDto oaspeti) {
    }
}
