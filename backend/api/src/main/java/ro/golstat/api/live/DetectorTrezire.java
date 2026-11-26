package ro.golstat.api.live;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Detecteaza o pauza a procesului (PC-ul in sleep / hibernare): bataia de 30s nu vine cand masina doarme,
 * deci un gol mare intre doua batai = am ratat live-ul. Atunci recuperam ce s-a jucat intre timp
 * prin {@link ActualizareProgram#recupereaza}.
 */
@Component
@Profile("bzzoiro")
@ConditionalOnProperty(name = "golstat.live.enabled", havingValue = "true")
public class DetectorTrezire {

    private static final Logger log = LoggerFactory.getLogger(DetectorTrezire.class);

    static final Duration PRAG_PAUZA = Duration.ofMinutes(5);

    private final ActualizareProgram actualizare;
    private final Clock clock;
    private Instant ultimaBataie;

    public DetectorTrezire(ActualizareProgram actualizare, Clock clock) {
        this.actualizare = actualizare;
        this.clock = clock;
    }

    @Scheduled(fixedDelay = 30_000)
    public void bataie() {
        Instant acum = clock.instant();
        Instant anterioara = ultimaBataie;
        ultimaBataie = acum;
        if (anterioara == null || Duration.between(anterioara, acum).compareTo(PRAG_PAUZA) < 0) {
            return;
        }
        log.info("Pauza de {} min detectata (sleep?): recuperez meciurile ratate",
                Duration.between(anterioara, acum).toMinutes());
        try {
            actualizare.recupereaza(anterioara);
        } catch (RuntimeException e) {
            log.warn("Recuperarea dupa pauza a esuat: {}", e.toString());
        }
        // recuperarea poate dura minute; nu o numaram drept o noua pauza
        ultimaBataie = clock.instant();
    }
}
