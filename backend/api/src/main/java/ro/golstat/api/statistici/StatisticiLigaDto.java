package ro.golstat.api.statistici;

/**
 * Mediile pe meci ale unei ligi/sezon, pentru clasamentul de tendinte al paginii Statistici.
 * Valorile vin din mediile deja calculate de motor (goluri, cornere, faulturi, cartonase).
 */
public record StatisticiLigaDto(
        long leagueId,
        String nume,
        String tara,
        String logo,
        Integer sezon,
        Double medieGoluri,
        Double medieCornere,
        Double medieFaulturi,
        Double medieCartonase
) {
}
