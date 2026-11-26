package ro.golstat.api.auth.email;

import org.springframework.lang.Nullable;

/**
 * Eveniment publicat in tranzactie; emailul pleaca doar dupa commit.
 *
 * @param tokenBrut tokenul din link (null pentru notificari fara link); nu se logheaza
 */
public record EmailDeTrimis(String catre, TipEmail tip, @Nullable String tokenBrut) {

    public enum TipEmail { CONFIRMARE, RESETARE, PAROLA_SCHIMBATA }

    @Override
    public String toString() {
        return "EmailDeTrimis[tip=" + tip + "]";
    }
}
