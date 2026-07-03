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
import ro.golstat.api.prediction.PredictionNotFoundException;

import java.time.Instant;

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

    /** Parametru care incalca o constrangere (ex. {@code leagueId} sub 1) → 400. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraint(ConstraintViolationException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Parametri invalizi", ex.getMessage());
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

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }
}
