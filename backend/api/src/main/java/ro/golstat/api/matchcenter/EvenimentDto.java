package ro.golstat.api.matchcenter;

/**
 * Un eveniment din cronologia meciului (gol, cartonas, schimbare, VAR). {@code gazde} = evenimentul
 * apartine echipei gazda. {@code jucator}/{@code asist} sunt rezolvate din DB (pot lipsi).
 */
public record EvenimentDto(
        Long id,
        Long teamId,
        boolean gazde,
        Integer minut,
        Integer minutExtra,
        String tip,
        String detaliu,
        String jucator,
        String asist
) {
}
