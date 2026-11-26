package ro.golstat.api.auth;

public class TokenInvalidException extends RuntimeException {

    public TokenInvalidException() {
        super("Link invalid sau expirat.");
    }
}
