package ro.golstat.api.auth;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class ParoleConfigTest {

    private final PasswordEncoder encoder = new ParoleConfig().passwordEncoder();

    @Test
    void encode_argon2id_cuSareUnica() {
        String h1 = encoder.encode("cal verde sare gardul");
        String h2 = encoder.encode("cal verde sare gardul");

        assertThat(h1).startsWith("{argon2}$argon2id$v=19$m=19456,t=2,p=1$");
        assertThat(h1).isNotEqualTo(h2);
    }

    @Test
    void matches_corect_siGresit() {
        String hash = encoder.encode("cal verde sare gardul");

        assertThat(encoder.matches("cal verde sare gardul", hash)).isTrue();
        assertThat(encoder.matches("cal verde sare gardu", hash)).isFalse();
    }

    @Test
    void matches_parolaPeste72Octeti_diferentaLaFinalConteaza() {
        String baza = "a".repeat(100);
        String hash = encoder.encode(baza + "1");

        assertThat(encoder.matches(baza + "2", hash)).isFalse();
    }

    @Test
    void upgradeEncoding_hashCurent_false() {
        assertThat(encoder.upgradeEncoding(encoder.encode("cal verde sare gardul"))).isFalse();
    }
}
