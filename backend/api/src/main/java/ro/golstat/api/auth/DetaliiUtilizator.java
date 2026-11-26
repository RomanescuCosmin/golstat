package ro.golstat.api.auth;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import ro.golstat.api.entity.Utilizator;

import java.util.Collection;
import java.util.List;

/**
 * Folosit doar in timpul autentificarii. Mereu activ si neblocat: {@code DaoAuthenticationProvider}
 * verifica aceste stari INAINTE de parola, deci ar trada existenta contului. Confirmarea emailului
 * se verifica dupa ce parola e corecta.
 */
public record DetaliiUtilizator(long id, String email, String parolaHash, boolean emailConfirmat)
        implements UserDetails {

    static DetaliiUtilizator din(Utilizator u) {
        return new DetaliiUtilizator(u.getId(), u.getEmail(), u.getParolaHash(), u.isEmailConfirmat());
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return parolaHash;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String toString() {
        return "DetaliiUtilizator[id=" + id + "]";
    }
}
