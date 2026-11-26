package ro.golstat.api.auth;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/** Tokenuri de 256 biti pentru linkurile din email; in baza ajunge doar {@link #hash(String)}. */
@Component
public class GeneratorToken {

    public record TokenNou(String brut, String hash) {
    }

    private static final int OCTETI = 32;

    private final SecureRandom random = new SecureRandom();

    public TokenNou genereaza() {
        byte[] octeti = new byte[OCTETI];
        random.nextBytes(octeti);
        String brut = Base64.getUrlEncoder().withoutPadding().encodeToString(octeti);
        return new TokenNou(brut, hash(brut));
    }

    public static String hash(String brut) {
        try {
            byte[] rezumat = MessageDigest.getInstance("SHA-256").digest(brut.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(rezumat);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
