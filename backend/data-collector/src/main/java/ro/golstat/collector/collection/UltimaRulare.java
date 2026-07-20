package ro.golstat.collector.collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import ro.golstat.collector.provider.apifootball.CounterStore;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Retine cand s-a incheiat ultimul ciclu, ca fereastra sa se largeasca singura dupa o pauza.
 *
 * <p>De ce: {@code zile-in-urma} e mic (1) fiindca fiecare meci ramas in fereastra se re-cere zilnic
 * (detaliile au TTL 24h), deci o fereastra lata costa cota in fiecare zi. Dar cu fereastra de o zi,
 * un laptop inchis doua zile ar pierde definitiv meciurile din gap — tacut, fara eroare.
 *
 * <p>Solutia: platim fereastra lata DOAR cand chiar a fost o pauza. Dupa 10 zile de nerulare, primul
 * ciclu cauta 11 zile in urma, apoi revine la 1.
 */
@Component
public class UltimaRulare {

    private static final Logger log = LoggerFactory.getLogger(UltimaRulare.class);
    private static final String CHEIE = "golstat:collection:ultima-rulare";

    /**
     * Plafon pentru recuperare. Fara el, un laptop repornit dupa trei luni ar cere ~90 de zile de
     * meciuri pe toate ligile si ar arde cota zilnica fara sa termine nimic.
     */
    static final int ZILE_MAX_RECUPERARE = 45;

    /** Absent pe profilul {@code stub} (Redis nu exista acolo) → cadem pe memorie de proces. */
    private final CounterStore store;
    private final Clock clock;
    private final AtomicReference<String> inMemory = new AtomicReference<>();

    public UltimaRulare(@Nullable CounterStore store, Clock clock) {
        this.store = store;
        this.clock = clock;
    }

    /**
     * Cate zile in urma trebuie sa caute ciclul curent: {@code zileConfigurate} in regim normal,
     * sau cat a durat pauza (plafonat) daca ultimul ciclu a fost demult.
     */
    public int zileDeAcoperit(int zileConfigurate) {
        Optional<Instant> ultima = citeste();
        if (ultima.isEmpty()) {
            return zileConfigurate;   // prima rulare: n-avem de recuperat nimic
        }
        long zilePauza = Duration.between(ultima.get(), clock.instant()).toDays();
        if (zilePauza <= zileConfigurate) {
            return zileConfigurate;
        }
        int acoperire = (int) Math.min(zilePauza + 1, ZILE_MAX_RECUPERARE);
        log.info("Pauza de {} zile de la ultimul ciclu - largesc fereastra la {} zile pentru recuperare",
                zilePauza, acoperire);
        return acoperire;
    }

    public void marcheaza() {
        String valoare = clock.instant().toString();
        if (store != null) {
            store.set(CHEIE, valoare);
        } else {
            inMemory.set(valoare);
        }
    }

    private Optional<Instant> citeste() {
        String valoare = store != null ? store.get(CHEIE).orElse(null) : inMemory.get();
        if (valoare == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Instant.parse(valoare));
        } catch (RuntimeException e) {
            log.warn("Marcaj de ultima rulare invalid ({}) - il ignor", valoare);
            return Optional.empty();
        }
    }
}
