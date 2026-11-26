package ro.golstat.api.auth;

import org.springframework.lang.Nullable;

public interface SesiuniUtilizator {

    /** Sterge toate sesiunile utilizatorului, cu exceptia celei cu id-ul {@code pastreaza} (daca e dat). */
    void invalideazaToate(long utilizatorId, @Nullable String pastreaza);
}
