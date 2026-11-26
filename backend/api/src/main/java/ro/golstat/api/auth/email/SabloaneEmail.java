package ro.golstat.api.auth.email;

import ro.golstat.api.auth.email.EmailDeTrimis.TipEmail;

/**
 * Texte simple (text/plain). Nu includem nimic introdus de utilizator (nici numele): emailul de
 * confirmare poate ajunge la o adresa straina, iar numele ar deveni un vector de phishing.
 */
final class SabloaneEmail {

    record Mesaj(String subiect, String corp) {
    }

    private static final String SEMNATURA = "\n\nEchipa golstat";
    private static final String NU_TU = "\n\nDacă nu tu ai cerut asta, ignoră emailul; contul tău rămâne neschimbat.";

    private SabloaneEmail() {
    }

    static Mesaj pentru(TipEmail tip, String link) {
        String salut = "Salut,\n\n";
        return switch (tip) {
            case CONFIRMARE -> new Mesaj("Confirmă-ți adresa de email",
                    salut + "Confirmă adresa de email ca să-ți activezi contul golstat:\n\n" + link
                            + "\n\nLinkul e valabil 24 de ore." + NU_TU + SEMNATURA);
            case RESETARE -> new Mesaj("Resetează parola",
                    salut + "Am primit o cerere de resetare a parolei. Alege o parolă nouă aici:\n\n" + link
                            + "\n\nLinkul e valabil 30 de minute și poate fi folosit o singură dată." + NU_TU + SEMNATURA);
            case CONT_EXISTENT -> new Mesaj("Ai deja un cont golstat",
                    salut + "Cineva a încercat să creeze un cont nou cu această adresă, dar ea are deja un cont.\n"
                            + "Dacă ai fost tu și nu-ți mai amintești parola, setează una nouă aici:\n\n" + link
                            + "\n\nLinkul e valabil 30 de minute." + NU_TU + SEMNATURA);
            case PAROLA_SCHIMBATA -> new Mesaj("Parola ta a fost schimbată",
                    salut + "Parola contului tău golstat tocmai a fost schimbată și celelalte sesiuni au fost deconectate.\n\n"
                            + "Dacă nu tu ai făcut asta, resetează imediat parola din fereastra de autentificare."
                            + SEMNATURA);
        };
    }
}
