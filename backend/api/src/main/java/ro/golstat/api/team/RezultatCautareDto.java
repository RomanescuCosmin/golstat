package ro.golstat.api.team;

/** Un rezultat din cautarea de echipe: identitate minima pentru dropdown. */
public record RezultatCautareDto(
        long teamId,
        String nume,
        String logo,
        String tara,
        boolean nationala
) {
}
