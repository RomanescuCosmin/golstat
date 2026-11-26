package ro.golstat.api.auth;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Politica NIST 800-63B: lungime 12–128 (code points), fara reguli de compozitie, dar respinge
 * parolele des folosite si pe cele egale cu emailul sau numele.
 */
@Component
public class PoliticaParola {

    static final int LUNGIME_MINIMA = 12;
    static final int LUNGIME_MAXIMA = 128;

    private final Set<String> comune = incarcaComune();

    public void verifica(String parola, String email, String nume) {
        int lungime = parola == null ? 0 : parola.codePointCount(0, parola.length());
        if (lungime < LUNGIME_MINIMA) {
            throw new ParolaSlabaException("Parola trebuie să aibă cel puțin " + LUNGIME_MINIMA + " caractere.");
        }
        if (lungime > LUNGIME_MAXIMA) {
            throw new ParolaSlabaException("Parola poate avea cel mult " + LUNGIME_MAXIMA + " caractere.");
        }
        String mic = parola.toLowerCase(Locale.ROOT);
        if (comune.contains(mic)) {
            throw new ParolaSlabaException("Parola e prea des folosită. Alege alta.");
        }
        if (egal(mic, email) || egal(mic, parteLocala(email)) || egal(mic, nume)) {
            throw new ParolaSlabaException("Parola nu poate fi emailul sau numele tău.");
        }
    }

    private static boolean egal(String parolaMic, String valoare) {
        return valoare != null && parolaMic.equals(valoare.trim().toLowerCase(Locale.ROOT));
    }

    private static String parteLocala(String email) {
        if (email == null) {
            return null;
        }
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : null;
    }

    private static Set<String> incarcaComune() {
        var resursa = new ClassPathResource("securitate/parole-comune.txt");
        try (var reader = new BufferedReader(new InputStreamReader(resursa.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines()
                    .map(String::trim)
                    .filter(l -> !l.isEmpty())
                    .map(l -> l.toLowerCase(Locale.ROOT))
                    .collect(Collectors.toUnmodifiableSet());
        } catch (IOException e) {
            throw new UncheckedIOException("Lista de parole comune nu poate fi citita", e);
        }
    }
}
