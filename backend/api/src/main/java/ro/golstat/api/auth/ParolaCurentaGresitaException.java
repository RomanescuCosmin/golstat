package ro.golstat.api.auth;

public class ParolaCurentaGresitaException extends RuntimeException {

    public ParolaCurentaGresitaException() {
        super("Parola e greșită.");
    }
}
