package ro.golstat.api.competitie;

/** Competitie inexistenta in DB. Mapata la 404 in {@code GlobalExceptionHandler}. */
public class CompetitieNotFoundException extends RuntimeException {

    public CompetitieNotFoundException(long leagueId) {
        super("Competitia " + leagueId + " nu exista");
    }
}
