package ro.golstat.api.auth;

import org.springframework.security.core.AuthenticatedPrincipal;

import java.io.Serializable;

/**
 * Principalul pastrat in sesiune: doar id + email, fara entitate si fara hash-ul parolei.
 * Numele (indexat de Spring Session) e id-ul, ca sesiunile sa poata fi gasite si dupa schimbarea emailului.
 */
public record UtilizatorAutentificat(long id, String email) implements AuthenticatedPrincipal, Serializable {

    @Override
    public String getName() {
        return String.valueOf(id);
    }
}
