package ro.golstat.api.preview;

import java.util.List;

/**
 * Forma recenta a unei echipe: ultimele 5 meciuri (cele mai recente primele) plus mediile de
 * goluri pe meci calculate pe intreaga fereastra (pana la 10 meciuri).
 */
public record FormaEchipaDto(
        List<FormaMeciDto> meciuri,
        double goluriMarcatePeMeci,
        double goluriPrimitePeMeci
) {
}
