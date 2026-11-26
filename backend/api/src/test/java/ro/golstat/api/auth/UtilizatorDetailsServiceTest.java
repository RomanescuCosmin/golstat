package ro.golstat.api.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import ro.golstat.api.entity.Utilizator;
import ro.golstat.api.repository.UtilizatorRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UtilizatorDetailsServiceTest {

    @Mock UtilizatorRepository utilizatori;

    @Test
    void loadUserByUsername_normalizeazaEmailul_siEMereuActiv() {
        when(utilizatori.findByEmail("ana@ex.ro")).thenReturn(Optional.of(utilizator(false)));

        var detalii = (DetaliiUtilizator) new UtilizatorDetailsService(utilizatori).loadUserByUsername(" Ana@Ex.ro");

        assertThat(detalii.id()).isEqualTo(7L);
        assertThat(detalii.emailConfirmat()).isFalse();
        assertThat(detalii.isEnabled()).isTrue();
        assertThat(detalii.isAccountNonLocked()).isTrue();
        assertThat(detalii.toString()).doesNotContain("{argon2}").doesNotContain("ana@ex.ro");
    }

    @Test
    void loadUserByUsername_inexistent_UsernameNotFound() {
        when(utilizatori.findByEmail("x@ex.ro")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new UtilizatorDetailsService(utilizatori).loadUserByUsername("x@ex.ro"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void updatePassword_salveazaHashulNou() {
        Utilizator u = utilizator(true);
        when(utilizatori.findById(7L)).thenReturn(Optional.of(u));

        new UtilizatorDetailsService(utilizatori).updatePassword(DetaliiUtilizator.din(u), "{argon2}nou");

        assertThat(u.getParolaHash()).isEqualTo("{argon2}nou");
    }

    private static Utilizator utilizator(boolean confirmat) {
        var u = new Utilizator();
        u.setId(7L);
        u.setEmail("ana@ex.ro");
        u.setNume("Ana");
        u.setParolaHash("{argon2}vechi");
        u.setEmailConfirmatLa(confirmat ? java.time.Instant.EPOCH : null);
        return u;
    }
}
