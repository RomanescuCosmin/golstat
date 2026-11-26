package ro.golstat.api.auth;

/** Aruncata doar dupa o parola corecta, deci nu tradeaza existenta contului. */
public class EmailNeconfirmatException extends RuntimeException {

    public EmailNeconfirmatException() {
        super("Confirmă adresa de email înainte să te autentifici.");
    }
}
