package ro.golstat.api.auth.email;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import ro.golstat.api.auth.AuthProperties;
import ro.golstat.api.auth.AuthProperties.Mail;
import ro.golstat.api.auth.AuthProperties.ModMail;
import ro.golstat.api.auth.email.EmailDeTrimis.TipEmail;

import java.util.concurrent.AbstractExecutorService;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AscultatorEmailTest {

    @Mock ExpeditorEmail expeditor;

    private final AscultatorEmail ascultator = new AscultatorEmail(expeditor(),
            new AuthProperties("https://golstat.ro/", new Mail(ModMail.LOG, "")), new ExecutorSincron());

    private ExpeditorEmail expeditor() {
        return (catre, subiect, corp) -> expeditor.trimite(catre, subiect, corp);
    }

    @Test
    void confirmare_linkDinFrontendUrl_cuTokenInFragment() {
        ascultator.la(new EmailDeTrimis("ana@ex.ro", TipEmail.CONFIRMARE, "TOK"));

        assertThat(corpTrimis("ana@ex.ro")).contains("https://golstat.ro/#confirmare=TOK").contains("24 de ore");
    }

    @Test
    void resetareSiContExistent_linkDeResetare() {
        ascultator.la(new EmailDeTrimis("ana@ex.ro", TipEmail.CONT_EXISTENT, "TOK"));

        assertThat(corpTrimis("ana@ex.ro")).contains("https://golstat.ro/#resetare=TOK").contains("are deja un cont");
    }

    @Test
    void parolaSchimbata_faraToken() {
        ascultator.la(new EmailDeTrimis("ana@ex.ro", TipEmail.PAROLA_SCHIMBATA, null));

        assertThat(corpTrimis("ana@ex.ro")).doesNotContain("null").contains("tocmai a fost schimbată");
    }

    @Test
    void eroareSmtp_nuSePropaga() {
        doThrow(new MailSendException("jos")).when(expeditor).trimite(anyString(), anyString(), anyString());

        assertThatCode(() -> ascultator.la(new EmailDeTrimis("ana@ex.ro", TipEmail.RESETARE, "TOK")))
                .doesNotThrowAnyException();
    }

    @Test
    void toString_nuContineTokenul() {
        assertThat(new EmailDeTrimis("ana@ex.ro", TipEmail.RESETARE, "SECRET").toString())
                .doesNotContain("SECRET").doesNotContain("ana@ex.ro");
    }

    private String corpTrimis(String catre) {
        var corp = ArgumentCaptor.forClass(String.class);
        verify(expeditor).trimite(org.mockito.ArgumentMatchers.eq(catre), anyString(), corp.capture());
        return corp.getValue();
    }

    private static final class ExecutorSincron extends AbstractExecutorService {
        @Override public void execute(Runnable command) { command.run(); }
        @Override public void shutdown() { }
        @Override public List<Runnable> shutdownNow() { return List.of(); }
        @Override public boolean isShutdown() { return false; }
        @Override public boolean isTerminated() { return false; }
        @Override public boolean awaitTermination(long timeout, TimeUnit unit) { return true; }
    }
}
