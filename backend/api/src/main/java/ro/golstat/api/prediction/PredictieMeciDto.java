package ro.golstat.api.prediction;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Predictia unui meci pentru afisare: sansele 1X2, liniile de goluri si BTTS, fiecare cu procent
 * (0..100) si cota statistica ({@code 1/p}, stil Flashscore). {@code lambda*} = asteptarea de goluri;
 * {@code esantion*} = cate meciuri au intrat in forma (masura de incredere — mica la CM).
 */
public record PredictieMeciDto(
        long fixtureId,
        EchipaDto echipaGazde,
        EchipaDto echipaOaspeti,
        OffsetDateTime kickoff,
        double lambdaGazde,
        double lambdaOaspeti,
        ProcentCota gazde,
        ProcentCota egal,
        ProcentCota oaspeti,
        List<LinieGolDto> linii,
        ProcentCota btts,
        int esantionGazde,
        int esantionOaspeti
) {
    /** O echipa pentru afisare; nume/logo pot lipsi daca echipa nu e in DB. */
    public record EchipaDto(long id, String nume, String logo) {
    }

    /** O piata: procentul (0..100) si cota statistica derivata. */
    public record ProcentCota(double procent, double cota) {
    }

    /** O linie over/under (ex. 2.5) cu ambele parti. */
    public record LinieGolDto(double linie, ProcentCota peste, ProcentCota sub) {
    }
}
