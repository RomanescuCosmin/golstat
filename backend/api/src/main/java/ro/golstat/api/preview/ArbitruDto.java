package ro.golstat.api.preview;

/**
 * Cardul de arbitru al meciului, din meciurile lui terminate pe care le avem (orice competitie).
 * {@code factorCartonase} = cat de sever e fata de media ligii (1.0 = neutru), deja tras spre 1.0 la
 * esantion mic — exact factorul care intra in procentele de cartonase. Mediile sunt {@code null} fara meciuri.
 */
public record ArbitruDto(
        String nume,
        int meciuri,
        Double cartonasePeMeci,
        Double faulturiPeMeci,
        int eliminari,
        double cartonaseMediaLigii,
        double factorCartonase
) {
}
