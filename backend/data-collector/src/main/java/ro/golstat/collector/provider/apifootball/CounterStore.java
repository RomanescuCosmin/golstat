package ro.golstat.collector.provider.apifootball;

import java.time.Duration;
import java.util.Optional;

/**
 * Depozitul minimal (Redis) de care au nevoie cache-ul de raspunsuri si garda de cota: cheie→valoare
 * cu TTL, plus un contor atomic. Abstractizat ca sa testam logica fara Redis real (fake in-memory).
 */
public interface CounterStore {

    Optional<String> get(String key);

    void set(String key, String value, Duration ttl);

    /**
     * Scrie fara TTL. Folosit de cursorul de backfill: progresul unei tinte terminate trebuie sa
     * supravietuiasca oricat, altfel dupa expirare am relua colectarea unui sezon deja adus.
     */
    void set(String key, String value);

    /** Incrementeaza atomic (INCR) si intoarce noua valoare; cheia inexistenta porneste de la 0. */
    long increment(String key);

    void expire(String key, Duration ttl);
}
