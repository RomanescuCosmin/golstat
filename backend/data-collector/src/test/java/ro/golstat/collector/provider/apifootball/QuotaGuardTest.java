package ro.golstat.collector.provider.apifootball;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuotaGuardTest {

    private static final Instant NOON = Instant.parse("2026-07-03T10:00:00Z");
    private final Clock clock = Clock.fixed(NOON, ZoneOffset.UTC);

    private static ApiFootballProperties props(int limit) {
        return new ApiFootballProperties("http://api.test", "k", limit, 0,
                Duration.ofHours(6), Duration.ofHours(1), Duration.ofHours(24),
                Duration.ofHours(20), Duration.ofDays(7), Duration.ofDays(7));
    }

    @Test
    void acquiresUpToLimitThenBlocks() {
        InMemoryCounterStore store = new InMemoryCounterStore();
        QuotaGuard guard = new QuotaGuard(store, props(3), clock);

        assertTrue(guard.tryAcquire());
        assertTrue(guard.tryAcquire());
        assertTrue(guard.tryAcquire());
        assertFalse(guard.tryAcquire(), "a 4-a cerere peste limita");
        assertFalse(guard.tryAcquire());
        assertEquals(3, guard.used(), "cererile blocate nu incrementeaza contorul");
    }

    @Test
    void counterKeyedByUtcDay() {
        InMemoryCounterStore store = new InMemoryCounterStore();
        new QuotaGuard(store, props(100), clock).tryAcquire();
        assertEquals("1", store.values.get("golstat:af:quota:2026-07-03"));
    }

    @Test
    void firstAcquireSetsTtlUntilMidnightUtc() {
        InMemoryCounterStore store = new InMemoryCounterStore();
        new QuotaGuard(store, props(100), clock).tryAcquire();

        Duration untilMidnight = Duration.between(NOON, Instant.parse("2026-07-04T00:00:00Z"));
        assertEquals(untilMidnight, store.ttls.get("golstat:af:quota:2026-07-03"));
    }

    @Test
    void limitZeroBlocksEverything() {
        QuotaGuard guard = new QuotaGuard(new InMemoryCounterStore(), props(0), clock);
        assertFalse(guard.tryAcquire());
        assertEquals(0, guard.used());
    }
}
