package ro.golstat.api.auth;

public class EmailExistentException extends RuntimeException {

    public EmailExistentException() {
        super("Există deja un cont cu acest email.");
    }
}
