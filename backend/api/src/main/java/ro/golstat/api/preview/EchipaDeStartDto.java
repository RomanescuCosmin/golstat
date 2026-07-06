package ro.golstat.api.preview;

import java.util.List;

/**
 * Echipa de start a unui meci: formatie + titulari + rezerve per echipa, indisponibilii
 * fiecarei echipe si arbitrul. Cand formatiile nu sunt inca anuntate (meci viitor), se
 * intoarce echipa PROBABILA — ultimul unsprezece de start al fiecarei echipe —
 * cu {@code probabila = true}. Null doar cand nici macar atat nu se poate construi.
 */
public record EchipaDeStartDto(
        EchipaLineupDto gazde,
        EchipaLineupDto oaspeti,
        String arbitru,
        boolean probabila
) {

    public record EchipaLineupDto(
            String formatie,
            List<JucatorDto> titulari,
            List<JucatorDto> rezerve,
            List<IndisponibilDto> indisponibili
    ) {
    }

    /** {@code grid} = pozitia in teren "rand:coloana" (ex. "4:2"); null la rezerve. */
    public record JucatorDto(Long id, String nume, Integer numar, String pozitie, String grid,
                             String foto) {
    }

    /** {@code motiv}: ACCIDENTAT / SUSPENDAT / INCERT; {@code detaliu} = motivul brut din sursa. */
    public record IndisponibilDto(Long id, String nume, String motiv, String detaliu) {
    }
}
