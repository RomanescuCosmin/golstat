package ro.golstat.api.auth;

import java.util.Locale;

public final class NormalizatorEmail {

    /**
     * Doar ASCII tiparibil: colatia implicita MySQL ({@code utf8mb4_0900_ai_ci}) ignora diacriticele,
     * deci {@code josé@x} si {@code jose@x} ar fi acelasi cont. Exclude si spatiile si CR/LF.
     */
    public static final String MODEL_ASCII = "[\\x21-\\x7E]+";

    private NormalizatorEmail() {
    }

    public static String normalizeaza(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
