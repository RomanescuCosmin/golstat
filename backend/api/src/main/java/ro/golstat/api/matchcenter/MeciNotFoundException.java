package ro.golstat.api.matchcenter;

/** Meci inexistent in DB. Mapata la 404 in {@code GlobalExceptionHandler}. */
public class MeciNotFoundException extends RuntimeException {

    public MeciNotFoundException(long fixtureId) {
        super("Meciul " + fixtureId + " nu exista");
    }
}
