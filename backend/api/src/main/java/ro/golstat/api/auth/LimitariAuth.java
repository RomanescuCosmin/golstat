package ro.golstat.api.auth;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;

@Component
public class LimitariAuth {

    private final LimitatorRata autentificarePeIp;
    private final LimitatorRata autentificarePeEmailSiIp;
    private final LimitatorRata autentificarePeEmail;
    private final LimitatorRata inregistrarePeIp;
    private final LimitatorRata emailuriPeIp;
    private final LimitatorRata emailuriPeAdresa;
    private final LimitatorRata tokenuriPeIp;
    private final LimitatorRata schimbarePeUtilizator;

    public LimitariAuth(Clock clock) {
        autentificarePeIp = new LimitatorRata(20, Duration.ofMinutes(5), clock);
        autentificarePeEmailSiIp = new LimitatorRata(5, Duration.ofMinutes(15), clock);
        autentificarePeEmail = new LimitatorRata(50, Duration.ofHours(1), clock);
        inregistrarePeIp = new LimitatorRata(5, Duration.ofHours(1), clock);
        emailuriPeIp = new LimitatorRata(5, Duration.ofHours(1), clock);
        emailuriPeAdresa = new LimitatorRata(3, Duration.ofHours(1), clock);
        tokenuriPeIp = new LimitatorRata(10, Duration.ofMinutes(15), clock);
        schimbarePeUtilizator = new LimitatorRata(10, Duration.ofMinutes(15), clock);
    }

    public LimitatorRata autentificarePeIp() {
        return autentificarePeIp;
    }

    /** Strict, dar pe pereche email+IP: un atacator nu poate bloca contul victimei din alta parte. */
    public LimitatorRata autentificarePeEmailSiIp() {
        return autentificarePeEmailSiIp;
    }

    /** Larg, pe email: opreste ghicirea distribuita pe multe IP-uri. */
    public LimitatorRata autentificarePeEmail() {
        return autentificarePeEmail;
    }

    public LimitatorRata inregistrarePeIp() {
        return inregistrarePeIp;
    }

    /** Parola uitata + retrimitere confirmare. */
    public LimitatorRata emailuriPeIp() {
        return emailuriPeIp;
    }

    /** Peste limita raspunsul ramane 202, doar ca nu mai pleaca email (anti-bombardare). */
    public LimitatorRata emailuriPeAdresa() {
        return emailuriPeAdresa;
    }

    /** Confirmare email + resetare parola. */
    public LimitatorRata tokenuriPeIp() {
        return tokenuriPeIp;
    }

    public LimitatorRata schimbarePeUtilizator() {
        return schimbarePeUtilizator;
    }
}
