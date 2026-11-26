package ro.golstat.api.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

/**
 * Argon2id cu parametrii minimi OWASP (19 MiB, 2 iteratii, paralelism 1). Prefixul {@code {argon2}}
 * din hash permite schimbarea algoritmului mai tarziu, cu re-hash automat la autentificare.
 */
@Configuration
public class ParoleConfig {

    static final String ALGORITM = "argon2";

    @Bean
    PasswordEncoder passwordEncoder() {
        var argon2 = new Argon2PasswordEncoder(16, 32, 1, 19_456, 2);
        return new DelegatingPasswordEncoder(ALGORITM, Map.of(ALGORITM, argon2));
    }
}
