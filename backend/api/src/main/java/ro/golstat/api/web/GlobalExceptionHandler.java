package ro.golstat.api.web;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import ro.golstat.api.auth.AutentificareEsuataException;
import ro.golstat.api.auth.EmailExistentException;
import ro.golstat.api.auth.EmailNeconfirmatException;
import ro.golstat.api.auth.ParolaCurentaGresitaException;
import ro.golstat.api.auth.ParolaSlabaException;
import ro.golstat.api.auth.PreaMulteIncercariException;
import ro.golstat.api.auth.TokenInvalidException;
import ro.golstat.api.competitie.CompetitieNotFoundException;
import ro.golstat.api.jucator.JucatorNotFoundException;
import ro.golstat.api.matchcenter.MeciNotFoundException;
import ro.golstat.api.prediction.PredictionNotFoundException;
import ro.golstat.api.team.EchipaNotFoundException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Handler global de erori pentru API. Toate raspunsurile de eroare sunt {@link ProblemDetail}
 * (RFC 7807, {@code application/problem+json}) cu forma stabila — usor de consumat din frontend:
 * {@code status}, {@code title}, {@code detail}, {@code instance} (calea) si un {@code timestamp}.
 *
 * <p>Extinde {@link ResponseEntityExceptionHandler} ca sa uniformizeze si erorile standard Spring MVC
 * (parametru lipsa, tip gresit, metoda nepermisa, body invalid, validare) — pe langa exceptiile de
 * domeniu de mai jos. Erorile neprevazute devin 500 GENERIC (fara sa scurgem detalii interne), dar
 * cauza reala se logheaza.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Meci inexistent sau care nu e viitor → 404. */
    @ExceptionHandler(PredictionNotFoundException.class)
    public ProblemDetail handleNotFound(PredictionNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Predictie indisponibila", ex.getMessage());
    }

    /** Meci inexistent → 404. */
    @ExceptionHandler(MeciNotFoundException.class)
    public ProblemDetail handleMeciNotFound(MeciNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Meci inexistent", ex.getMessage());
    }

    /** Echipa inexistenta → 404. */
    @ExceptionHandler(EchipaNotFoundException.class)
    public ProblemDetail handleEchipaNotFound(EchipaNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Echipa inexistenta", ex.getMessage());
    }

    /** Competitie inexistenta → 404. */
    @ExceptionHandler(CompetitieNotFoundException.class)
    public ProblemDetail handleCompetitieNotFound(CompetitieNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Competitie inexistenta", ex.getMessage());
    }

    /** Jucator inexistent → 404. */
    @ExceptionHandler(JucatorNotFoundException.class)
    public ProblemDetail handleJucatorNotFound(JucatorNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Jucator inexistent", ex.getMessage());
    }

    /** Parametru care incalca o constrangere (ex. {@code leagueId} sub 1) → 400. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraint(ConstraintViolationException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Parametri invalizi", ex.getMessage());
    }

    @ExceptionHandler(AutentificareEsuataException.class)
    public ProblemDetail handleAutentificareEsuata(AutentificareEsuataException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "Autentificare eșuată", ex.getMessage());
    }

    @ExceptionHandler(EmailExistentException.class)
    public ProblemDetail handleEmailExistent(EmailExistentException ex) {
        return cuCod(problem(HttpStatus.CONFLICT, "Email folosit", ex.getMessage()), "email-existent");
    }

    @ExceptionHandler(EmailNeconfirmatException.class)
    public ProblemDetail handleEmailNeconfirmat(EmailNeconfirmatException ex) {
        return cuCod(problem(HttpStatus.FORBIDDEN, "Email neconfirmat", ex.getMessage()), "email-neconfirmat");
    }

    @ExceptionHandler(TokenInvalidException.class)
    public ProblemDetail handleTokenInvalid(TokenInvalidException ex) {
        return cuCod(problem(HttpStatus.BAD_REQUEST, "Link invalid", ex.getMessage()), "token-invalid");
    }

    @ExceptionHandler(ParolaSlabaException.class)
    public ProblemDetail handleParolaSlaba(ParolaSlabaException ex) {
        return cuCod(problem(HttpStatus.BAD_REQUEST, "Parolă prea slabă", ex.getMessage()), "parola-slaba");
    }

    @ExceptionHandler(ParolaCurentaGresitaException.class)
    public ProblemDetail handleParolaCurentaGresita(ParolaCurentaGresitaException ex) {
        return cuCod(problem(HttpStatus.BAD_REQUEST, "Parolă greșită", ex.getMessage()), "parola-gresita");
    }

    /** Doua modificari simultane ale aceluiasi cont (ex. schimbare + resetare parola). */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleConflict(ObjectOptimisticLockingFailureException ex) {
        return problem(HttpStatus.CONFLICT, "Conflict", "Contul a fost modificat între timp. Reîncearcă.");
    }

    @ExceptionHandler(PreaMulteIncercariException.class)
    public ResponseEntity<ProblemDetail> handlePreaMulteIncercari(PreaMulteIncercariException ex) {
        ProblemDetail pd = problem(HttpStatus.TOO_MANY_REQUESTS, "Prea multe încercări", ex.getMessage());
        pd.setProperty("retryAfter", ex.getRetryAfterSecunde());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSecunde()))
                .body(pd);
    }

    /** Validare {@code @Valid}: {@code erori: {camp: mesaj}}; valorile respinse (ex. parole) nu se intorc. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        Map<String, String> erori = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> erori.putIfAbsent(e.getField(), e.getDefaultMessage()));
        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "Date invalide", "Verifică câmpurile marcate.");
        pd.setProperty("erori", erori);
        return handleExceptionInternal(ex, pd, headers, HttpStatus.BAD_REQUEST, request);
    }

    /** Plasa de siguranta: orice altceva neprevazut → 500 generic, dar logam cauza reala. */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Eroare neasteptata in API", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Eroare interna",
                "A aparut o eroare interna. Incearca din nou mai tarziu.");
    }

    /** Adauga {@code timestamp} pe erorile standard Spring MVC (tratate de clasa de baza). */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, @Nullable Object body,
                                                             HttpHeaders headers, HttpStatusCode statusCode,
                                                             WebRequest request) {
        if (body instanceof ProblemDetail pd) {
            pd.setProperty("timestamp", Instant.now());
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }

    private static ProblemDetail cuCod(ProblemDetail pd, String cod) {
        pd.setProperty("cod", cod);
        return pd;
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }
}
