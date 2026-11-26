package ro.golstat.api.auth.email;

public interface ExpeditorEmail {

    void trimite(String catre, String subiect, String corp);
}
