package ro.golstat.api.preview;

import java.util.List;

/**
 * Echipa de start (probabila) a unui meci: formatie + titulari + rezerve per echipa,
 * indisponibilii fiecarei echipe si arbitrul. Lipseste (null in previzualizare) pana cand
 * lineup-urile sunt anuntate, aproape de kickoff.
 */
public record EchipaDeStartDto(
        EchipaLineupDto gazde,
        EchipaLineupDto oaspeti,
        String arbitru
) {

    public record EchipaLineupDto(
            String formatie,
            List<JucatorDto> titulari,
            List<JucatorDto> rezerve,
            List<IndisponibilDto> indisponibili
    ) {
    }

    /** {@code grid} = pozitia in teren "rand:coloana" (ex. "4:2"); null la rezerve. */
    public record JucatorDto(Long id, String nume, Integer numar, String pozitie, String grid) {
    }

    /** {@code motiv}: ACCIDENTAT / SUSPENDAT / INCERT; {@code detaliu} = motivul brut din sursa. */
    public record IndisponibilDto(Long id, String nume, String motiv, String detaliu) {
    }
}
