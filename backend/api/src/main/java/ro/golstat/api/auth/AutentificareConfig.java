package ro.golstat.api.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import ro.golstat.api.web.SpaCsrfTokenRequestHandler;

import java.util.List;

@Configuration
class AutentificareConfig {

    /** {@code DaoAuthenticationProvider} face un hash fals pentru emailuri inexistente: timp egal cu parola gresita. */
    @Bean
    AuthenticationManager authenticationManager(UtilizatorDetailsService utilizatori, PasswordEncoder encoder) {
        var provider = new DaoAuthenticationProvider(utilizatori);
        provider.setPasswordEncoder(encoder);
        provider.setUserDetailsPasswordService(utilizatori);
        return new ProviderManager(provider);
    }

    /** Id de sesiune nou (anti session fixation) si token CSRF nou la fiecare autentificare. */
    @Bean
    SessionAuthenticationStrategy sessionAuthenticationStrategy(CookieCsrfTokenRepository csrfTokenRepository) {
        var csrf = new CsrfAuthenticationStrategy(csrfTokenRepository);
        // ca raspunsul de login sa contina deja noul cookie XSRF-TOKEN
        csrf.setRequestHandler(new SpaCsrfTokenRequestHandler());
        return new CompositeSessionAuthenticationStrategy(List.of(new ChangeSessionIdAuthenticationStrategy(), csrf));
    }
}
