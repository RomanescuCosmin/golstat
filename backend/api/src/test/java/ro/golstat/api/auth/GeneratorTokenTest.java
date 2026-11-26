package ro.golstat.api.auth;

import org.junit.jupiter.api.Test;
import ro.golstat.api.auth.GeneratorToken.TokenNou;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GeneratorTokenTest {

    private final GeneratorToken generator = new GeneratorToken();

    @Test
    void genereaza_256BitiBase64Url() {
        TokenNou token = generator.genereaza();

        assertThat(token.brut()).hasSize(43).matches("[A-Za-z0-9_-]+");
    }

    @Test
    void genereaza_hashSha256Hex_diferitDeBrut() {
        TokenNou token = generator.genereaza();

        assertThat(token.hash()).hasSize(64).matches("[0-9a-f]+").isNotEqualTo(token.brut());
        assertThat(GeneratorToken.hash(token.brut())).isEqualTo(token.hash());
    }

    @Test
    void hash_vectorCunoscut() {
        assertThat(GeneratorToken.hash("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void genereaza_unic() {
        Set<String> tokenuri = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            tokenuri.add(generator.genereaza().brut());
        }
        assertThat(tokenuri).hasSize(1000);
    }
}
