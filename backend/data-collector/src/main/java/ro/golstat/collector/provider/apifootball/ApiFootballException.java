package ro.golstat.collector.provider.apifootball;

/** Eroare de la API-Football (envelope cu {@code errors} ne-gol, sau raspuns invalid). */
public class ApiFootballException extends RuntimeException {

    public ApiFootballException(String message) {
        super(message);
    }
}
