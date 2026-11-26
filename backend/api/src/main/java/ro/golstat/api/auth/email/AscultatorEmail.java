package ro.golstat.api.auth.email;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import ro.golstat.api.auth.AuthProperties;
import ro.golstat.api.auth.email.SabloaneEmail.Mesaj;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Trimite emailurile dupa commit, pe un executor propriu si marginit: durata raspunsului HTTP nu
 * depinde de SMTP (nu tradeaza daca un cont exista), iar o tranzactie anulata nu trimite nimic.
 * Executorul nu e bean, ca sa nu dezactiveze {@code applicationTaskExecutor} din Boot.
 */
@Component
class AscultatorEmail {

    private static final Logger log = LoggerFactory.getLogger(AscultatorEmail.class);

    private final ExpeditorEmail expeditor;
    private final String frontendUrl;
    private final ExecutorService executor;

    @Autowired
    AscultatorEmail(ExpeditorEmail expeditor, AuthProperties props) {
        this(expeditor, props, new ThreadPoolExecutor(1, 2, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100),
                new CustomizableThreadFactory("email-"),
                (r, e) -> log.warn("Coada de emailuri e plina; email aruncat")));
    }

    AscultatorEmail(ExpeditorEmail expeditor, AuthProperties props, ExecutorService executor) {
        this.expeditor = expeditor;
        this.frontendUrl = props.frontendUrl().replaceAll("/+$", "");
        this.executor = executor;
    }

    @TransactionalEventListener
    void la(EmailDeTrimis email) {
        executor.execute(() -> trimite(email));
    }

    private void trimite(EmailDeTrimis email) {
        Mesaj mesaj = SabloaneEmail.pentru(email.tip(), link(email));
        try {
            expeditor.trimite(email.catre(), mesaj.subiect(), mesaj.corp());
        } catch (RuntimeException e) {
            log.error("Trimiterea emailului {} a esuat: {}", email.tip(), e.getMessage());
        }
    }

    /** Tokenul sta in fragment ({@code #}): nu ajunge la server, in loguri de acces sau in {@code Referer}. */
    private String link(EmailDeTrimis email) {
        return switch (email.tip()) {
            case CONFIRMARE -> frontendUrl + "/#confirmare=" + email.tokenBrut();
            case RESETARE -> frontendUrl + "/#resetare=" + email.tokenBrut();
            case PAROLA_SCHIMBATA -> frontendUrl + "/";
        };
    }

    @PreDestroy
    void opreste() throws InterruptedException {
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
    }
}
