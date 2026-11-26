package ro.golstat.api.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;

/**
 * Erorile din lantul de filtre Spring Security (inainte de controller, deci in afara
 * {@link GlobalExceptionHandler}) scrise in aceeasi forma {@link ProblemDetail}.
 * Respingerea CSRF are {@code cod: "csrf"} ca frontend-ul sa poata reincerca o data cu token nou.
 */
public class ProblemDetailSecurityHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public ProblemDetailSecurityHandlers(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException ex) throws IOException {
        scrie(request, response, HttpStatus.UNAUTHORIZED, "Neautentificat",
                "Trebuie să fii autentificat pentru această acțiune.", null);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {
        if (ex instanceof CsrfException) {
            scrie(request, response, HttpStatus.FORBIDDEN, "Sesiune expirată",
                    "Sesiunea a expirat. Reîncearcă.", "csrf");
        } else {
            scrie(request, response, HttpStatus.FORBIDDEN, "Acces interzis",
                    "Nu ai acces la această resursă.", null);
        }
    }

    private void scrie(HttpServletRequest request, HttpServletResponse response, HttpStatus status,
                       String title, String detail, String cod) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        pd.setInstance(URI.create(request.getRequestURI()));
        pd.setProperty("timestamp", Instant.now());
        if (cod != null) {
            pd.setProperty("cod", cod);
        }
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), pd);
    }
}
