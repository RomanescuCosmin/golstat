package ro.golstat.api.competitie;

import ro.golstat.api.prediction.PredictieMeciDto.EchipaDto;
import ro.golstat.api.team.PaginaEchipaDto.RandClasament;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Pagina unei competitii: antet + clasament + golgheteri/pasatori + rezultate recente + program.
 * Fiecare bloc degradeaza independent la {@code []} — pagina raspunde 200 daca liga are date.
 */
public record PaginaCompetitieDto(
        Antet antet,
        List<RandClasament> clasament,
        List<Jucator> golgheteri,
        List<Jucator> pasatori,
        List<Meci> rezultate,
        List<Meci> urmatoare
) {
    /** Identitatea competitiei + contextul de sezon. */
    public record Antet(long leagueId, String nume, String tara, String logo, Integer sezon, List<Integer> sezoane) {
    }

    /** Un jucator dintr-un top (golgheteri/pasatori) cu echipa lui. */
    public record Jucator(Long playerId, String nume, String foto, EchipaDto echipa, int valoare) {
    }

    /** Un meci al competitiei (rezultat sau program). */
    public record Meci(
            long fixtureId,
            OffsetDateTime kickoff,
            EchipaDto gazde,
            EchipaDto oaspeti,
            Integer golGazde,
            Integer golOaspeti,
            String status,
            boolean inDesfasurare,
            boolean terminat
    ) {
    }
}
