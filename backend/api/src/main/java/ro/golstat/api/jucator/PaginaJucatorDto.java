package ro.golstat.api.jucator;

import ro.golstat.api.prediction.PredictieMeciDto.EchipaDto;

import java.util.List;

/** Profilul unui jucator: identitate + o linie de statistici per (liga, sezon, echipa). */
public record PaginaJucatorDto(
        long playerId,
        String nume,
        String foto,
        String nationalitate,
        Integer varsta,
        String pozitie,
        EchipaDto echipaCurenta,
        List<Sezon> sezoane
) {
    /** Statisticile jucatorului intr-o liga/sezon la o echipa. */
    public record Sezon(
            long leagueId,
            String liga,
            String ligaLogo,
            Integer sezon,
            EchipaDto echipa,
            Integer aparitii,
            Integer minute,
            Integer goluri,
            Integer pase,
            Integer galbene,
            Integer rosii,
            Double rating
    ) {
    }
}
