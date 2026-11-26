package ro.golstat.api.web;

import jakarta.servlet.http.Cookie;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Double-submit real (cookie {@code XSRF-TOKEN} + header {@code X-XSRF-TOKEN}). Nu folosim
 * {@code SecurityMockMvcRequestPostProcessors.csrf()}: acela inlocuieste permanent repository-ul din
 * CsrfFilter cu unul pe sesiune si strica testele care ruleaza dupa el pe acelasi context.
 */
public final class XsrfTestSuport {

    private static final String TOKEN = "token-test-csrf";

    private XsrfTestSuport() {
    }

    public static RequestPostProcessor xsrf() {
        return request -> {
            request.setCookies(new Cookie("XSRF-TOKEN", TOKEN));
            request.addHeader("X-XSRF-TOKEN", TOKEN);
            return request;
        };
    }
}
