package ro.golstat.api.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.golstat.api.auth.AuthDto.AutentificareRequest;
import ro.golstat.api.auth.AuthDto.ConfirmareRequest;
import ro.golstat.api.auth.AuthDto.EmailRequest;
import ro.golstat.api.auth.AuthDto.InregistrareRequest;
import ro.golstat.api.auth.AuthDto.ResetareRequest;
import ro.golstat.api.auth.AuthDto.SchimbareParolaRequest;
import ro.golstat.api.auth.AuthDto.UtilizatorDto;
import ro.golstat.api.repository.UtilizatorRepository;

/**
 * Inregistrarea raspunde 409 pe un email deja folosit; parola uitata si retrimiterea raspund mereu 202,
 * indiferent daca emailul are cont. Deconectarea e tratata de filtrul de logout din SecurityConfig.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final ContService cont;
    private final AutentificareService autentificare;
    private final UtilizatorRepository utilizatori;
    private final LimitariAuth limitari;

    public AuthController(ContService cont, AutentificareService autentificare, UtilizatorRepository utilizatori,
                          LimitariAuth limitari) {
        this.cont = cont;
        this.autentificare = autentificare;
        this.utilizatori = utilizatori;
        this.limitari = limitari;
    }

    /** 204 pentru vizitatori; raspunsul seteaza si cookie-ul XSRF-TOKEN. */
    @GetMapping("/eu")
    public ResponseEntity<UtilizatorDto> eu(@AuthenticationPrincipal UtilizatorAutentificat principal,
                                            HttpServletRequest request) {
        if (principal == null) {
            return ResponseEntity.noContent().build();
        }
        return utilizatori.findById(principal.id())
                .map(u -> ResponseEntity.ok(new UtilizatorDto(u.getId(), u.getEmail(), u.getNume())))
                .orElseGet(() -> {
                    // contul a fost sters (ex. curatarea conturilor neconfirmate): sesiunea nu mai are sens
                    HttpSession sesiune = request.getSession(false);
                    if (sesiune != null) {
                        sesiune.invalidate();
                    }
                    return ResponseEntity.noContent().build();
                });
    }

    @PostMapping("/inregistrare")
    public ResponseEntity<Void> inregistrare(@Valid @RequestBody InregistrareRequest cerere, HttpServletRequest request) {
        limitari.inregistrarePeIp().consuma(request.getRemoteAddr());
        try {
            cont.inregistreaza(cerere.email(), cerere.parola(), cerere.nume());
        } catch (DataIntegrityViolationException e) {
            // doua inregistrari simultane pe acelasi email: a doua pierde pe constrangerea unica
            log.warn("Inregistrare respinsa de constrangerile bazei: {}", e.getMostSpecificCause().getMessage());
            throw new EmailExistentException();
        }
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/autentificare")
    public UtilizatorDto autentificare(@Valid @RequestBody AutentificareRequest cerere,
                                       HttpServletRequest request, HttpServletResponse response) {
        return autentificare.autentifica(cerere.email(), cerere.parola(), request, response);
    }

    @PostMapping("/confirmare-email")
    public ResponseEntity<Void> confirmareEmail(@Valid @RequestBody ConfirmareRequest cerere, HttpServletRequest request) {
        limitari.tokenuriPeIp().consuma(request.getRemoteAddr());
        cont.confirmaEmail(cerere.token(), cerere.parola());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/retrimite-confirmare")
    public ResponseEntity<Void> retrimiteConfirmare(@Valid @RequestBody EmailRequest cerere, HttpServletRequest request) {
        limitari.emailuriPeIp().consuma(request.getRemoteAddr());
        cont.retrimiteConfirmare(cerere.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/parola-uitata")
    public ResponseEntity<Void> parolaUitata(@Valid @RequestBody EmailRequest cerere, HttpServletRequest request) {
        limitari.emailuriPeIp().consuma(request.getRemoteAddr());
        cont.parolaUitata(cerere.email());
        return ResponseEntity.accepted().build();
    }

    /** Nu autentifica automat: utilizatorul se conecteaza cu parola noua. */
    @PostMapping("/resetare-parola")
    public ResponseEntity<Void> resetareParola(@Valid @RequestBody ResetareRequest cerere, HttpServletRequest request) {
        limitari.tokenuriPeIp().consuma(request.getRemoteAddr());
        cont.reseteazaParola(cerere.token(), cerere.parolaNoua());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/schimba-parola")
    public ResponseEntity<Void> schimbaParola(@AuthenticationPrincipal UtilizatorAutentificat principal,
                                              @Valid @RequestBody SchimbareParolaRequest cerere,
                                              HttpServletRequest request) {
        HttpSession sesiune = request.getSession(false);
        cont.schimbaParola(principal.id(), cerere.parolaCurenta(), cerere.parolaNoua(),
                sesiune == null ? null : sesiune.getId());
        request.changeSessionId();
        return ResponseEntity.noContent().build();
    }
}
