package ro.golstat.api.preview;

import java.time.LocalDate;

/** Un meci din forma recenta a unei echipe; {@code rezultat}: "V" / "E" / "I". */
public record FormaMeciDto(
        LocalDate data,
        boolean acasa,
        int golMarcate,
        int golPrimite,
        String rezultat
) {
}
