package ro.golstat.api.jucator;

/** Jucator inexistent in DB. Mapata la 404 in {@code GlobalExceptionHandler}. */
public class JucatorNotFoundException extends RuntimeException {

    public JucatorNotFoundException(long playerId) {
        super("Jucatorul " + playerId + " nu exista");
    }
}
