package ro.golstat.api.auth;

/** Acelasi mesaj pentru email inexistent si parola gresita. */
public class AutentificareEsuataException extends RuntimeException {

    public AutentificareEsuataException() {
        super("Email sau parolă incorecte.");
    }
}
