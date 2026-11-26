package ro.golstat.api.auth.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import ro.golstat.api.auth.AuthProperties;

/**
 * Modul implicit. Fara credentiale aplicatia porneste (colectarea nu trebuie sa cada din cauza
 * emailului), dar emailurile nu pleaca si nici nu se logheaza: un token nu ajunge niciodata in log.
 */
@Component
@ConditionalOnProperty(name = "golstat.auth.mail.mod", havingValue = "smtp", matchIfMissing = true)
class SmtpExpeditorEmail implements ExpeditorEmail {

    private static final Logger log = LoggerFactory.getLogger(SmtpExpeditorEmail.class);

    private final JavaMailSender mailSender;
    private final String deLa;
    private final boolean configurat;

    SmtpExpeditorEmail(JavaMailSender mailSender, AuthProperties props,
                       @Value("${spring.mail.username:}") String utilizator,
                       @Value("${spring.mail.password:}") String parola) {
        this.mailSender = mailSender;
        this.deLa = props.mail().deLa();
        this.configurat = StringUtils.hasText(deLa) && StringUtils.hasText(utilizator) && StringUtils.hasText(parola);
        if (!configurat) {
            log.error("SMTP neconfigurat (GOLSTAT_MAIL_USERNAME / GOLSTAT_MAIL_PASSWORD lipsesc): emailurile de cont NU se trimit");
        }
    }

    @Override
    public void trimite(String catre, String subiect, String corp) {
        if (!configurat) {
            log.error("Email netrimis: SMTP neconfigurat");
            return;
        }
        var mesaj = new SimpleMailMessage();
        mesaj.setFrom(deLa);
        mesaj.setTo(catre);
        mesaj.setSubject(subiect);
        mesaj.setText(corp);
        mailSender.send(mesaj);
    }
}
