package ro.golstat.api.auth.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Doar pentru dezvoltare: scrie emailul (inclusiv linkul cu token) in log in loc sa-l trimita. */
@Component
@ConditionalOnProperty(name = "golstat.auth.mail.mod", havingValue = "log")
class LogExpeditorEmail implements ExpeditorEmail {

    private static final Logger log = LoggerFactory.getLogger(LogExpeditorEmail.class);

    LogExpeditorEmail() {
        log.warn("Emailurile NU se trimit (golstat.auth.mail.mod=log); linkurile apar in log. Doar pentru dezvoltare.");
    }

    @Override
    public void trimite(String catre, String subiect, String corp) {
        log.info("EMAIL catre={} subiect=\"{}\"\n{}", catre, subiect, corp);
    }
}
