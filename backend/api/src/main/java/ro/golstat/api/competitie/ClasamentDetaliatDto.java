package ro.golstat.api.competitie;

import java.time.LocalDate;
import java.util.List;

/** Clasamentul oficial al unei competitii, pe grupe (o singura grupa cu {@code nume} null la ligile obisnuite). */
public record ClasamentDetaliatDto(List<Grupa> grupe) {

    public record Grupa(String nume, List<Rand> randuri) {
    }

    /** {@code zona} = zona oficiala a locului (ex. „Champions League", „Relegation"). */
    public record Rand(int pozitie, long teamId, String nume, String logo,
                       Integer jucate, Integer victorii, Integer egaluri, Integer infrangeri,
                       Integer golMarcate, Integer golPrimite, Integer golaveraj, Integer puncte,
                       String zona, List<MeciForma> forma) {
    }

    /** Ultimele meciuri, cel mai recent primul; {@code rezultat} = V/E/I din perspectiva echipei. */
    public record MeciForma(long fixtureId, LocalDate data, String rezultat, boolean acasa,
                            String adversar, int golGazde, int golOaspeti) {
    }
}
