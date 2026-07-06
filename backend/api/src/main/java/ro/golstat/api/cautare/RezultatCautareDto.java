package ro.golstat.api.cautare;

/**
 * Un rezultat din cautarea globala (echipa / campionat / jucator): identitate minima pentru dropdown.
 * {@code id} = teamId / leagueId / playerId dupa {@code tip}; {@code imagine} = logo (echipa/liga) sau
 * poza (jucator); {@code subtitlu} = tara (echipa/liga) sau nationalitate (jucator).
 */
public record RezultatCautareDto(
        TipRezultat tip,
        long id,
        String nume,
        String imagine,
        String subtitlu,
        boolean nationala
) {
}
