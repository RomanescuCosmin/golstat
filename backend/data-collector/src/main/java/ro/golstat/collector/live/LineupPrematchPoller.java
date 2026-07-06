package ro.golstat.collector.live;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ro.golstat.collector.collection.CollectionProperties;
import ro.golstat.collector.collection.LeagueTarget;
import ro.golstat.collector.provider.DataProvider;
import ro.golstat.collector.provider.apifootball.ApiFootballQuotaExceededException;
import ro.golstat.collector.publish.EventPublisher;
import ro.golstat.common.GolstatConstants;
import ro.golstat.common.dto.FixtureDto;
import ro.golstat.common.dto.FixtureLineupDto;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Aduce formatiile PROBABILE/anuntate ale meciurilor care incep curand: pentru fiecare meci NS cu
 * kickoff in urmatoarele {@code lineups-before-minutes}, cere lineup-urile fara cache
 * ({@link DataProvider#upcomingFixtureLineups}) la fiecare tick, pana apar; odata publicate, meciul
 * nu mai e cerut. Lista de meciuri vine din {@code provider.fixtures(...)}, care e cache-uita
 * (TTL upcoming) → tick-urile fara meciuri in fereastra nu ard cota.
 *
 * <p>Downstream nu se schimba nimic: publicam pe topicul existent {@code fixture-lineups}
 * (ingest idempotent), iar pagina de preview arata automat formatia anuntata in locul celei
 * probabile din ultimul meci.
 */
@Component
@ConditionalOnProperty(name = "golstat.live.enabled", havingValue = "true")
public class LineupPrematchPoller {

    private static final Logger log = LoggerFactory.getLogger(LineupPrematchPoller.class);

    /** Un meci ramane eligibil putin si dupa kickoff (fixtures cache-uite pot ramane NS cateva minute). */
    private static final Duration GRATIE_DUPA_KICKOFF = Duration.ofMinutes(15);
    /** Dupa atat timp de la kickoff, intrarea "publicat" nu mai e necesara (meciul nu mai e eligibil). */
    private static final Duration RETENTIE_PUBLICATE = Duration.ofHours(3);

    private final DataProvider provider;
    private final EventPublisher publisher;
    private final CollectionProperties collection;
    private final LiveProperties props;
    private final Clock clock;
    /** Meciurile cu formatii deja publicate pre-meci (id → kickoff), ca sa nu le mai cerem. */
    private final Map<Long, Instant> publicate = new HashMap<>();

    public LineupPrematchPoller(DataProvider provider, EventPublisher publisher,
                                CollectionProperties collection, LiveProperties props, Clock clock) {
        this.provider = provider;
        this.publisher = publisher;
        this.collection = collection;
        this.props = props;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${golstat.live.lineups-poll-ms:300000}")
    public void poll() {
        Instant now = clock.instant();
        publicate.values().removeIf(kickoff -> kickoff.isBefore(now.minus(RETENTIE_PUBLICATE)));
        try {
            for (LeagueTarget liga : collection.leagues()) {
                if (liga.doarFixtures()) {
                    continue;   // competitie "doar meciuri" (amicale) → fara detalii per meci
                }
                colecteazaLiga(liga, now);
            }
        } catch (ApiFootballQuotaExceededException e) {
            log.warn("Poll lineups pre-meci oprit (cota atinsa): {}", e.getMessage());
        }
    }

    private void colecteazaLiga(LeagueTarget liga, Instant now) {
        LocalDate azi = LocalDate.ofInstant(now, ZoneOffset.UTC);
        List<FixtureDto> fixtures = provider.fixtures(liga.leagueId(), liga.season(), azi, azi.plusDays(1));
        for (FixtureDto f : fixtures) {
            if (!eligibil(f, now)) {
                continue;
            }
            try {
                List<FixtureLineupDto> lineups = provider.upcomingFixtureLineups(f.id());
                if (!lineups.isEmpty()) {
                    publisher.publish(GolstatConstants.KafkaTopics.FIXTURE_LINEUPS,
                            String.valueOf(f.id()), lineups);
                    publicate.put(f.id(), f.kickoff().toInstant());
                    log.info("Formatii pre-meci publicate pentru meciul {} (liga {})", f.id(), liga.leagueId());
                }
            } catch (ApiFootballQuotaExceededException e) {
                throw e;
            } catch (RuntimeException e) {
                // eroare izolata pe un meci → il sarim, se reia la tick-ul urmator
                log.warn("Lineups pre-meci esuate pentru meciul {}: {}", f.id(), e.toString());
            }
        }
    }

    /** NS, ne-publicat deja, cu kickoff intre [now - gratie, now + lineupsBeforeMinutes]. */
    private boolean eligibil(FixtureDto f, Instant now) {
        if (f == null || f.id() == null || f.kickoff() == null
                || !GolstatConstants.FixtureStatus.NOT_STARTED.equals(f.statusShort())
                || publicate.containsKey(f.id())) {
            return false;
        }
        Instant kickoff = f.kickoff().toInstant();
        return !kickoff.isBefore(now.minus(GRATIE_DUPA_KICKOFF))
                && !kickoff.isAfter(now.plus(Duration.ofMinutes(props.lineupsBeforeMinutes())));
    }
}
