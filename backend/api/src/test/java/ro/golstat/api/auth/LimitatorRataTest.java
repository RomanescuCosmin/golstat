package ro.golstat.api.auth;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LimitatorRataTest {

    private static final Instant T0 = Instant.parse("2026-11-26T10:00:00Z");

    private final CeasMutabil ceas = new CeasMutabil(T0);
    private final LimitatorRata limitator = new LimitatorRata(3, Duration.ofMinutes(5), ceas);

    @Test
    void consuma_subLimita_trece() {
        assertThatCode(() -> {
            limitator.consuma("ip:1");
            limitator.consuma("ip:1");
            limitator.consuma("ip:1");
        }).doesNotThrowAnyException();
    }

    @Test
    void consuma_pesteLimita_exceptieCuRetryAfter() {
        repeta(3, "ip:1");
        ceas.avanseaza(Duration.ofSeconds(60));

        assertThatThrownBy(() -> limitator.consuma("ip:1"))
                .isInstanceOfSatisfying(PreaMulteIncercariException.class,
                        e -> assertThat(e.getRetryAfterSecunde()).isEqualTo(240));
    }

    @Test
    void retryAfter_rotunjitInSus_minim1() {
        repeta(3, "ip:1");
        ceas.avanseaza(Duration.ofMinutes(5).minusMillis(1));

        assertThatThrownBy(() -> limitator.verifica("ip:1"))
                .isInstanceOfSatisfying(PreaMulteIncercariException.class,
                        e -> assertThat(e.getRetryAfterSecunde()).isEqualTo(1));
    }

    @Test
    void fereastraExpirata_permiteDinNou() {
        repeta(3, "ip:1");
        ceas.avanseaza(Duration.ofMinutes(5));

        assertThatCode(() -> limitator.consuma("ip:1")).doesNotThrowAnyException();
    }

    @Test
    void cheiIndependente() {
        repeta(3, "ip:1");

        assertThatCode(() -> limitator.consuma("ip:2")).doesNotThrowAnyException();
    }

    @Test
    void verifica_nuNumara() {
        for (int i = 0; i < 10; i++) {
            limitator.verifica("email:a");
        }
        assertThatCode(() -> repeta(3, "email:a")).doesNotThrowAnyException();
    }

    @Test
    void reseteaza_golesteContorul() {
        repeta(3, "email:a");
        limitator.reseteaza("email:a");

        assertThatCode(() -> limitator.verifica("email:a")).doesNotThrowAnyException();
    }

    @Test
    void incearca_falsePesteLimita_faraExceptie() {
        assertThat(limitator.incearca("email:a")).isTrue();
        assertThat(limitator.incearca("email:a")).isTrue();
        assertThat(limitator.incearca("email:a")).isTrue();
        assertThat(limitator.incearca("email:a")).isFalse();
    }

    @Test
    void consuma_cereriParalele_niciodataPesteLimita() throws Exception {
        var reusite = new java.util.concurrent.atomic.AtomicInteger();
        var start = new java.util.concurrent.CountDownLatch(1);
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(16)) {
            for (int i = 0; i < 200; i++) {
                executor.submit(() -> {
                    start.await();
                    if (limitator.incearca("email:a")) {
                        reusite.incrementAndGet();
                    }
                    return null;
                });
            }
            start.countDown();
        }

        assertThat(reusite.get()).isEqualTo(3);
    }

    private void repeta(int ori, String cheie) {
        for (int i = 0; i < ori; i++) {
            limitator.consuma(cheie);
        }
    }

    private static final class CeasMutabil extends Clock {
        private Instant acum;

        CeasMutabil(Instant acum) {
            this.acum = acum;
        }

        void avanseaza(Duration d) {
            acum = acum.plus(d);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return acum;
        }
    }
}
