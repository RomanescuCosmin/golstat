package ro.golstat.api.auth;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Fereastra fixa per cheie (IP, email), in memorie: nu depinde de Redis, iar un restart reseteaza
 * contoarele (compromis acceptat). Memoria e marginita de {@code maximumSize}.
 */
public class LimitatorRata {

    private record Fereastra(Instant start, int incercari) {
    }

    private final int maxIncercari;
    private final Duration durata;
    private final Clock clock;
    private final Cache<String, Fereastra> ferestre;

    public LimitatorRata(int maxIncercari, Duration durata, Clock clock) {
        this.maxIncercari = maxIncercari;
        this.durata = durata;
        this.clock = clock;
        this.ferestre = Caffeine.newBuilder()
                .maximumSize(100_000)
                .expireAfterWrite(durata)
                .build();
    }

    /** Arunca {@link PreaMulteIncercariException} daca cheia si-a epuizat fereastra, fara sa numere. */
    public void verifica(String cheie) {
        Fereastra f = activa(cheie);
        if (f != null && f.incercari() >= maxIncercari) {
            throw new PreaMulteIncercariException(secundePanaLaReset(f, clock.instant()));
        }
    }

    /** Rezerva atomic o incercare (verificare + numarare in acelasi {@code compute}, fara curse intre cereri paralele). */
    public void consuma(String cheie) {
        Instant acum = clock.instant();
        Fereastra[] blocata = new Fereastra[1];
        ferestre.asMap().compute(cheie, (k, f) -> {
            if (f == null || expirata(f, acum)) {
                return new Fereastra(acum, 1);
            }
            if (f.incercari() >= maxIncercari) {
                blocata[0] = f;
                return f;
            }
            return new Fereastra(f.start(), f.incercari() + 1);
        });
        if (blocata[0] != null) {
            throw new PreaMulteIncercariException(secundePanaLaReset(blocata[0], acum));
        }
    }

    public boolean incearca(String cheie) {
        try {
            consuma(cheie);
            return true;
        } catch (PreaMulteIncercariException e) {
            return false;
        }
    }

    public void reseteaza(String cheie) {
        ferestre.invalidate(cheie);
    }

    private Fereastra activa(String cheie) {
        Fereastra f = ferestre.getIfPresent(cheie);
        return f == null || expirata(f, clock.instant()) ? null : f;
    }

    private boolean expirata(Fereastra f, Instant acum) {
        return !acum.isBefore(f.start().plus(durata));
    }

    private long secundePanaLaReset(Fereastra f, Instant acum) {
        long ramaseMs = Duration.between(acum, f.start().plus(durata)).toMillis();
        return Math.max(1, (ramaseMs + 999) / 1000);
    }
}
