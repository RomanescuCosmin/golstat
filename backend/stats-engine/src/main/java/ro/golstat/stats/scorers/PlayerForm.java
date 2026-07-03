package ro.golstat.stats.scorers;

/**
 * Forma istorica a unui jucator pentru piata de marcatori: {@code goals} marcate in
 * {@code minutes} jucate (rata istorica), pozitia (pentru baseline) si {@code expectedMinutes}
 * in meciul de analizat ({@code 0} = accidentat / suspendat / nu e in lot).
 */
public record PlayerForm(
        long playerId,
        Position position,
        int goals,
        int minutes,
        int expectedMinutes
) {
}
