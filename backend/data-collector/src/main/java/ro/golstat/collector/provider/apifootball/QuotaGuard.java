package ro.golstat.collector.provider.apifootball;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Garda de cota pentru planul free (100 requests/zi). Contorul e per ZI UTC (cheie cu data), cu TTL
 * pana la miezul noptii UTC — aliniat cu reset-ul furnizorului. Doar apelurile HTTP reale rezerva un
 * slot; cache-hit-urile nu trec pe-aici. Colectorul ruleaza pe un singur fir (scheduler), dar
 * pastram {@code synchronized} ca rezervarea "citeste-apoi-incrementeaza" sa ramana atomica.
 */
@Service
@Profile("!stub")
public class QuotaGuard {

    private static final String QUOTA_PREFIX = "golstat:af:quota:";

    private final CounterStore store;
    private final Clock clock;
    private final int dailyLimit;
    private final int rezervaZilnica;

    public QuotaGuard(CounterStore store, ApiFootballProperties props, Clock clock) {
        this.store = store;
        this.clock = clock;
        this.dailyLimit = props.dailyRequestLimit();
        this.rezervaZilnica = props.rezervaZilnica();
    }

    /** Rezerva un slot pe ziua curenta. {@code false} daca s-a atins limita (fara sa incrementeze). */
    public synchronized boolean tryAcquire() {
        if (used() >= dailyLimit) {
            return false;
        }
        String key = quotaKey();
        long updated = store.increment(key);
        if (updated == 1L) {
            store.expire(key, untilNextMidnightUtc());
        }
        return true;
    }

    public long used() {
        return store.get(quotaKey()).map(Long::parseLong).orElse(0L);
    }

    /** Cate requesturi mai incap azi. Permite deciderea INAINTE de apel, nu prin exceptie. */
    public long ramase() {
        return Math.max(0, dailyLimit - used());
    }

    /**
     * Are voie backfill-ul istoric sa mai consume? Doar peste {@code rezervaZilnica}, ca sa ramana
     * mereu cota pentru meciurile recente si program — acelea sunt vizibile imediat in aplicatie.
     */
    public boolean bugetPesteRezerva() {
        return ramase() > rezervaZilnica;
    }

    private String quotaKey() {
        return QUOTA_PREFIX + LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    private Duration untilNextMidnightUtc() {
        Instant now = clock.instant();
        Instant nextMidnight = LocalDate.ofInstant(now, ZoneOffset.UTC)
                .plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        return Duration.between(now, nextMidnight);
    }
}
