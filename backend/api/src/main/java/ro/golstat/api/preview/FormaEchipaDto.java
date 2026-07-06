package ro.golstat.api.preview;

import java.util.List;

/**
 * Forma recenta a unei echipe, pe doua ferestre de cate 7 meciuri: {@code locatie} = doar pe
 * locatia din meciul previzualizat (gazdele ACASA, oaspetii in DEPLASARE) si {@code general} =
 * indiferent de locatie. Meciurile sunt cele mai recente primele; mediile de goluri se calculeaza
 * pe fereastra respectiva.
 */
public record FormaEchipaDto(FereastraFormaDto locatie, FereastraFormaDto general) {

    public record FereastraFormaDto(
            List<FormaMeciDto> meciuri,
            double goluriMarcatePeMeci,
            double goluriPrimitePeMeci
    ) {
    }
}
