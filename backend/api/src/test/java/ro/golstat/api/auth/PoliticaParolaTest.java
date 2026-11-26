package ro.golstat.api.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PoliticaParolaTest {

    private final PoliticaParola politica = new PoliticaParola();

    @Test
    void verifica_prea_scurta_respinsa() {
        assertThatThrownBy(() -> politica.verifica("scurta12345", "ana@ex.ro", "Ana"))
                .isInstanceOf(ParolaSlabaException.class)
                .hasMessageContaining("cel puțin 12");
    }

    @Test
    void verifica_null_respinsa() {
        assertThatThrownBy(() -> politica.verifica(null, "ana@ex.ro", "Ana"))
                .isInstanceOf(ParolaSlabaException.class);
    }

    @Test
    void verifica_prea_lunga_respinsa() {
        assertThatThrownBy(() -> politica.verifica("a".repeat(129), "ana@ex.ro", "Ana"))
                .isInstanceOf(ParolaSlabaException.class)
                .hasMessageContaining("cel mult 128");
    }

    @Test
    void verifica_lungimeNumarataPeCodePoints() {
        // 12 caractere vizibile, dar 24 de unitati UTF-16 (emoji)
        String emoji = "😀".repeat(11) + "x";
        assertThatCode(() -> politica.verifica(emoji, "ana@ex.ro", "Ana")).doesNotThrowAnyException();
        assertThatThrownBy(() -> politica.verifica("😀".repeat(11), "ana@ex.ro", "Ana"))
                .isInstanceOf(ParolaSlabaException.class);
    }

    @Test
    void verifica_parolaComuna_respinsaIndiferentDeMajuscule() {
        assertThatThrownBy(() -> politica.verifica("000000000000", "ana@ex.ro", "Ana"))
                .hasMessageContaining("des folosită");
        assertThatThrownBy(() -> politica.verifica("ILoveMyFamily", "ana@ex.ro", "Ana"))
                .hasMessageContaining("des folosită");
    }

    @Test
    void verifica_egalaCuEmailul_respinsa() {
        assertThatThrownBy(() -> politica.verifica("Ana.Popescu@ex.ro", "ana.popescu@ex.ro", "Ana"))
                .hasMessageContaining("emailul sau numele");
    }

    @Test
    void verifica_egalaCuParteaLocalaAEmailului_respinsa() {
        assertThatThrownBy(() -> politica.verifica("ana.popescu.1990", "ana.popescu.1990@ex.ro", "Ana"))
                .hasMessageContaining("emailul sau numele");
    }

    @Test
    void verifica_egalaCuNumele_respinsa() {
        assertThatThrownBy(() -> politica.verifica("Cosmin Romanescu", "x@ex.ro", "cosmin romanescu"))
                .hasMessageContaining("emailul sau numele");
    }

    @Test
    void verifica_frazaLunga_acceptata() {
        assertThatCode(() -> politica.verifica("cal verde sare gardul la stadion", "ana@ex.ro", "Ana"))
                .doesNotThrowAnyException();
    }
}
