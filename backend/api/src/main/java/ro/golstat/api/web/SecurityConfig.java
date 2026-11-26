package ro.golstat.api.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * Lantul de securitate. Toate GET-urile raman publice; login-ul doar stabileste identitatea.
 * Nu depinde de repository-uri, ca testele {@code @WebMvcTest} sa-l poata importa direct.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    public static final String CALE_DECONECTARE = "/api/v1/auth/deconectare";

    private final boolean cookieSecure;
    private final String cookieSesiune;

    public SecurityConfig(@Value("${golstat.securitate.cookie-secure:true}") boolean cookieSecure,
                          @Value("${server.servlet.session.cookie.name:JSESSIONID}") String cookieSesiune) {
        this.cookieSecure = cookieSecure;
        this.cookieSesiune = cookieSesiune;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper,
                                            CookieCsrfTokenRepository csrfTokenRepository,
                                            SecurityContextRepository securityContextRepository) throws Exception {
        var erori = new ProblemDetailSecurityHandlers(objectMapper);
        http
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/schimba-parola").authenticated()
                        .anyRequest().permitAll())
                .csrf(c -> c
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                        // SockJS face POST-uri pe transportul xhr (protejat prin verificarea de origine)
                        .ignoringRequestMatchers("/ws/**")
                        // endpoint-uri dev, active doar cu golstat.dev.enabled
                        .ignoringRequestMatchers("/api/v1/dev/**"))
                .securityContext(s -> s.securityContextRepository(securityContextRepository))
                .logout(l -> l
                        .logoutRequestMatcher(PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, CALE_DECONECTARE))
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT))
                        .deleteCookies(cookieSesiune))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                // altfel fiecare vizitator anonim ar crea o sesiune
                .requestCache(RequestCacheConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(erori)
                        .accessDeniedHandler(erori))
                .headers(h -> h
                        .referrerPolicy(r -> r.policy(ReferrerPolicy.NO_REFERRER))
                        .contentSecurityPolicy(c -> c.policyDirectives("default-src 'none'; frame-ancestors 'none'")));
        return http.build();
    }

    /** Cookie {@code XSRF-TOKEN} citibil din JS (asa functioneaza double-submit), restrictionat la acelasi site. */
    @Bean
    CookieCsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repo = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repo.setCookieCustomizer(c -> c.sameSite("Strict").secure(cookieSecure).path("/"));
        return repo;
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }
}
