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

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Bucla LIVE: la fiecare {@code golstat.live.poll-ms}, cere meciurile in desfasurare
 * ({@code fixtures?live=all}, fara cache) si republica pe {@code golstat.fixtures} (upsert idempotent
 * la ingest → scor/status se actualizeaza singure). Doua economii de cota:
 * <ul>
 *   <li><b>gating</b>: nu poleaza decat daca {@link LiveSchedule} arata un meci in fereastra de joc;</li>
 *   <li><b>filtrare</b>: publica doar ligile urmarite (restul lumii, ignorat) — filtrul e client-side,
 *       apelul {@code live=all} e unul singur oricum.</li>
 * </ul>
 * Bean-ul exista doar cand {@code golstat.live.enabled=true}.
 */
@Component
@ConditionalOnProperty(name = "golstat.live.enabled", havingValue = "true")
public class LivePoller {

    private static final Logger log = LoggerFactory.getLogger(LivePoller.class);

    private final DataProvider provider;
    private final EventPublisher publisher;
    private final LiveSchedule schedule;
    private final LiveProperties props;
    private final Clock clock;
    private final Set<Long> trackedLeagues;

    public LivePoller(DataProvider provider, EventPublisher publisher, LiveSchedule schedule,
                      LiveProperties props, CollectionProperties collection, Clock clock) {
        this.provider = provider;
        this.publisher = publisher;
        this.schedule = schedule;
        this.props = props;
        this.clock = clock;
        this.trackedLeagues = collection.leagues().stream()
                .map(LeagueTarget::leagueId)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Scheduled(fixedDelayString = "${golstat.live.poll-ms:15000}")
    public void poll() {
        boolean inMatchWindow = schedule.anyKickoffWithin(clock.instant(),
                Duration.ofMinutes(props.windowBeforeMinutes()),
                Duration.ofMinutes(props.windowAfterMinutes()));
        if (!inMatchWindow) {
            return;   // niciun meci urmarit in desfasurare → nu ardem un request
        }

        try {
            List<FixtureDto> live = provider.liveFixtures();
            int published = 0;
            for (FixtureDto f : live) {
                if (f.leagueId() != null && trackedLeagues.contains(f.leagueId())) {
                    publisher.publish(GolstatConstants.KafkaTopics.FIXTURES, String.valueOf(f.id()), f);
                    published++;
                }
            }
            log.debug("Live: {} meciuri in desfasurare, {} urmarite publicate", live.size(), published);
        } catch (ApiFootballQuotaExceededException e) {
            log.warn("Poll live oprit (cota atinsa): {}", e.getMessage());
        }
    }
}
