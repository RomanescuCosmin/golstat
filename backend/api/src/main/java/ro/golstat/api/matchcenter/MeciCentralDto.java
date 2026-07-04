package ro.golstat.api.matchcenter;

import ro.golstat.api.prediction.PredictieMeciDto.EchipaDto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Detaliul unui meci pentru Match Center: scor + status/minut, statisticile finale (sau live)
 * pe echipa si cronologia evenimentelor. {@code statistici} e {@code null} cat timp nu exista
 * randuri colectate. Functioneaza pe meciuri terminale (2022–2024) si se upgradeaza la live.
 */
public record MeciCentralDto(
        long fixtureId,
        Long leagueId,
        EchipaDto gazde,
        EchipaDto oaspeti,
        Integer golGazde,
        Integer golOaspeti,
        String status,
        String statusLung,
        Integer minut,
        boolean inDesfasurare,
        boolean terminat,
        OffsetDateTime kickoff,
        Statistici statistici,
        List<EvenimentDto> evenimente
) {
    /** Statisticile ambelor echipe; oricare camp poate lipsi. */
    public record Statistici(Echipa gazde, Echipa oaspeti) {
    }

    /** Statisticile unei echipe pentru meciul curent; toate nullable (date partiale/lipsa). */
    public record Echipa(
            Integer posesie,
            Integer suturiPePoarta,
            Integer suturiTotal,
            Integer cornere,
            Integer faulturi,
            Integer galbene,
            Integer rosii,
            Integer pase,
            Integer paseReusite,
            Integer preciziePase,
            Double xg
    ) {
    }
}
