package ro.golstat.api.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cererile si raspunsurile de autentificare. Pe parole validam doar prezenta si lungimea maxima
 * (anti-DoS pe Argon2); restul politicii e in {@link PoliticaParola}.
 */
public final class AuthDto {

    private static final String EMAIL_INVALID = "Adresă de email invalidă.";

    private AuthDto() {
    }

    public record InregistrareRequest(
            @NotBlank @Email(message = EMAIL_INVALID) @Pattern(regexp = NormalizatorEmail.MODEL_ASCII, message = EMAIL_INVALID)
            @Size(max = 254) String email,
            @NotNull @Size(max = 128) String parola,
            @NotBlank @Size(min = 2, max = 50)
            @Pattern(regexp = "[\\p{L}\\p{M}\\p{N} .'-]+",
                    message = "Numele poate conține doar litere, cifre, spații, punct, apostrof și cratimă.")
            String nume) {
    }

    public record AutentificareRequest(
            @NotBlank @Size(max = 254) String email,
            @NotNull @Size(max = 128) String parola) {
    }

    public record EmailRequest(
            @NotBlank @Email(message = EMAIL_INVALID) @Pattern(regexp = NormalizatorEmail.MODEL_ASCII, message = EMAIL_INVALID)
            @Size(max = 254) String email) {
    }

    public record ConfirmareRequest(
            @NotBlank @Size(max = 64) String token,
            @NotNull @Size(max = 128) String parola) {
    }

    public record ResetareRequest(
            @NotBlank @Size(max = 64) String token,
            @NotNull @Size(max = 128) String parolaNoua) {
    }

    public record SchimbareParolaRequest(
            @NotNull @Size(max = 128) String parolaCurenta,
            @NotNull @Size(max = 128) String parolaNoua) {
    }

    public record UtilizatorDto(long id, String email, String nume) {
    }
}
