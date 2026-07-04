package ro.golstat.api.team;

/** Echipa inexistenta in DB. Mapata la 404 in {@code GlobalExceptionHandler}. */
public class EchipaNotFoundException extends RuntimeException {

    public EchipaNotFoundException(long teamId) {
        super("Echipa " + teamId + " nu exista");
    }
}
