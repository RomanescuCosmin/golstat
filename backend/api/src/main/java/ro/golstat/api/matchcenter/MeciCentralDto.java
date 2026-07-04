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
        String ligaNume,
        String ligaLogo,
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
        String arbitru,
        String stadion,
        Statistici statistici,
        Formatii formatii,
        List<EvenimentDto> evenimente
) {
    /** Statisticile ambelor echipe; oricare camp poate lipsi. */
    public record Statistici(Echipa gazde, Echipa oaspeti) {
    }

    /** Formatiile ambelor echipe; blocul e {@code null} pana exista lineup pentru AMBELE. */
    public record Formatii(EchipaFormatie gazde, EchipaFormatie oaspeti) {
    }

    /** Formatia unei echipe: schema, antrenor si loturile (titulari + rezerve). */
    public record EchipaFormatie(
            String formatie,
            String antrenor,
            List<JucatorDto> titulari,
            List<JucatorDto> rezerve
    ) {
    }

    /** Un jucator din formatie; {@code grid} = pozitia in teren "rand:coloana" (null la rezerve). */
    public record JucatorDto(Long id, String nume, Integer numar, String pozitie, String grid) {
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
