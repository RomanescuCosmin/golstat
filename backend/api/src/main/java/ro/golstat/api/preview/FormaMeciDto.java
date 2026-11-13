package ro.golstat.api.preview;

import ro.golstat.api.prediction.PredictieMeciDto.EchipaDto;

import java.time.LocalDate;

/**
 * Un meci din forma recenta a unei echipe; {@code rezultat}: "V" / "E" / "I". {@code adversar} = cu cine
 * s-a jucat (nume + emblema); {@code null} cand nu-l stim.
 */
public record FormaMeciDto(
        LocalDate data,
        boolean acasa,
        int golMarcate,
        int golPrimite,
        String rezultat,
        EchipaDto adversar
) {

    public FormaMeciDto(LocalDate data, boolean acasa, int golMarcate, int golPrimite, String rezultat) {
        this(data, acasa, golMarcate, golPrimite, rezultat, null);
    }
}
