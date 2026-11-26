package ro.golstat.api.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.golstat.api.auth.AuthDto.UtilizatorDto;
import ro.golstat.api.repository.UtilizatorRepository;

import java.time.Clock;
import java.util.List;

@Service
public class AutentificareService {

    private final AuthenticationManager authenticationManager;
    private final SessionAuthenticationStrategy strategieSesiune;
    private final SecurityContextRepository contextRepository;
    private final UtilizatorRepository utilizatori;
    private final LimitariAuth limitari;
    private final AuditAuth audit;
    private final Clock clock;
    private final SecurityContextHolderStrategy contextHolder = SecurityContextHolder.getContextHolderStrategy();

    public AutentificareService(AuthenticationManager authenticationManager, SessionAuthenticationStrategy strategieSesiune,
                                SecurityContextRepository contextRepository, UtilizatorRepository utilizatori,
                                LimitariAuth limitari, AuditAuth audit, Clock clock) {
        this.authenticationManager = authenticationManager;
        this.strategieSesiune = strategieSesiune;
        this.contextRepository = contextRepository;
        this.utilizatori = utilizatori;
        this.limitari = limitari;
        this.audit = audit;
        this.clock = clock;
    }

    /**
     * Limitele se rezerva inainte de verificarea parolei (cererile paralele nu pot depasi limita);
     * o autentificare reusita elibereaza contorul email+IP. Principalul din sesiune e
     * {@link UtilizatorAutentificat}, nu {@code UserDetails}: Spring Session indexeaza sesiunile dupa
     * id, ca sa le putem invalida dupa resetarea parolei.
     */
    @Transactional
    public UtilizatorDto autentifica(String email, String parola, HttpServletRequest request, HttpServletResponse response) {
        String e = NormalizatorEmail.normalizeaza(email);
        String ip = request.getRemoteAddr();
        String emailSiIp = e + "|" + ip;
        limitari.autentificarePeIp().consuma(ip);
        limitari.autentificarePeEmailSiIp().consuma(emailSiIp);
        limitari.autentificarePeEmail().consuma(e);

        DetaliiUtilizator detalii;
        try {
            detalii = (DetaliiUtilizator) authenticationManager
                    .authenticate(UsernamePasswordAuthenticationToken.unauthenticated(e, parola))
                    .getPrincipal();
        } catch (AuthenticationException ex) {
            audit.eveniment("autentificare-esuata", null);
            throw new AutentificareEsuataException();
        }
        if (!detalii.emailConfirmat()) {
            audit.eveniment("autentificare-email-neconfirmat", detalii.id());
            throw new EmailNeconfirmatException();
        }
        limitari.autentificarePeEmailSiIp().reseteaza(emailSiIp);
        UtilizatorDto utilizator = utilizatori.findById(detalii.id())
                .map(u -> new UtilizatorDto(u.getId(), u.getEmail(), u.getNume()))
                .orElseThrow(AutentificareEsuataException::new);

        var autentificare = UsernamePasswordAuthenticationToken.authenticated(
                new UtilizatorAutentificat(detalii.id(), detalii.email()), null, List.of());
        strategieSesiune.onAuthentication(autentificare, request, response);
        var context = contextHolder.createEmptyContext();
        context.setAuthentication(autentificare);
        contextHolder.setContext(context);
        contextRepository.saveContext(context, request, response);

        utilizatori.marcheazaAutentificare(detalii.id(), clock.instant());
        audit.eveniment("autentificare", detalii.id());
        return utilizator;
    }
}
