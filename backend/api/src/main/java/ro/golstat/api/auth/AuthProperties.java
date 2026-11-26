package ro.golstat.api.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param frontendUrl baza linkurilor din email; NU se deriva din header-ul {@code Host} (link poisoning)
 */
@ConfigurationProperties(prefix = "golstat.auth")
public record AuthProperties(String frontendUrl, Mail mail) {

    public enum ModMail { LOG, SMTP }

    public record Mail(ModMail mod, String deLa) {
    }
}
