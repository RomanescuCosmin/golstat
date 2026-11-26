package ro.golstat.api.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NormalizatorEmailTest {

    @Test
    void normalizeaza_trimSiLowercase() {
        assertThat(NormalizatorEmail.normalizeaza("  Ana.Pop@Ex.RO ")).isEqualTo("ana.pop@ex.ro");
    }

    @Test
    void normalizeaza_independentDeLocale_iTurcesc() {
        assertThat(NormalizatorEmail.normalizeaza("ILIE@EX.RO")).isEqualTo("ilie@ex.ro");
    }

    @Test
    void modelAscii_respingeDiacriticeSpatiiSiControl() {
        assertThat("ana.pop+golstat@ex.ro").matches(NormalizatorEmail.MODEL_ASCII);
        assertThat("josé@ex.ro").doesNotMatch(NormalizatorEmail.MODEL_ASCII);
        assertThat("ana pop@ex.ro").doesNotMatch(NormalizatorEmail.MODEL_ASCII);
        assertThat("ana@ex.ro\r\nBcc: x@y.z").doesNotMatch(NormalizatorEmail.MODEL_ASCII);
    }

    @Test
    void normalizeaza_null_null() {
        assertThat(NormalizatorEmail.normalizeaza(null)).isNull();
    }
}
