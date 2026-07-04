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
import ro.golstat.common.dto.FixtureLiveDto;
import ro.golstat.common.dto.FixtureTeamStatsDto;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Bucla LIVE: la fiecare {@code golstat.live.poll-ms}, cere meciurile in desfasurare
 * ({@code fixtures?live=all}, fara cache) si:
 * <ul>
 *   <li>republica fixture-ul pe {@code golstat.fixtures} (upsert idempotent → scor/status se actualizeaza);</li>
 *   <li>publica evenimentele INLINE (gratis din {@code live=all}) pe {@code fixture-events}, lot per meci
 *       (ingest idempotent delete+rewrite → cronologia se reimprospateaza la fiecare poll);</li>
 *   <li>reimprospateaza statisticile live THROTTLED (la {@code stats-every-ms}) pe {@code fixture-team-stats}.</li>
 * </ul>
 * Doua economii de cota: <b>gating</b> pe {@link LiveSchedule} si <b>filtrare</b> pe ligile urmarite.
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
    private final Set<Long> statsLeagues;
    /** Ultimul moment cand am cerut statistici per meci; curatat cand meciul nu mai e live. */
    private final Map<Long, Instant> ultimulFetchStats = new HashMap<>();

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
        this.statsLeagues = Set.copyOf(props.statsLeagues());
    }

    @Scheduled(fixedDelayString = "${golstat.live.poll-ms:15000}")
    public void poll() {
        boolean inMatchWindow = schedule.anyKickoffWithin(clock.instant(),
                Duration.ofMinutes(props.windowBeforeMinutes()),
                Duration.ofMinutes(props.windowAfterMinutes()));
        if (!inMatchWindow) {
            return;   // niciun meci urmarit in desfasurare → nu ardem un request
        }

        Instant now = clock.instant();
        try {
            List<FixtureLiveDto> live = provider.liveFixtures();
            Set<Long> liveTracked = new HashSet<>();
            int published = 0;
            for (FixtureLiveDto fl : live) {
                FixtureDto f = fl.fixture();
                if (f == null || f.id() == null || f.leagueId() == null
                        || !trackedLeagues.contains(f.leagueId())) {
                    continue;
                }
                liveTracked.add(f.id());
                publisher.publish(GolstatConstants.KafkaTopics.FIXTURES, String.valueOf(f.id()), f);
                published++;

                if (fl.evenimente() != null && !fl.evenimente().isEmpty()) {
                    publisher.publish(GolstatConstants.KafkaTopics.FIXTURE_EVENTS,
                            String.valueOf(f.id()), fl.evenimente());
                }

                if (statsEligible(f.leagueId()) && dueForStats(f.id(), now)) {
                    List<FixtureTeamStatsDto> stats = provider.liveFixtureStatistics(f.id());
                    if (!stats.isEmpty()) {
                        publisher.publish(GolstatConstants.KafkaTopics.FIXTURE_TEAM_STATS,
                                String.valueOf(f.id()), stats);
                    }
                    ultimulFetchStats.put(f.id(), now);
                }
            }
            // meciurile care nu mai sunt live nu mai au nevoie de intrare in harta de throttling
            ultimulFetchStats.keySet().removeIf(id -> !liveTracked.contains(id));
            log.debug("Live: {} meciuri in desfasurare, {} urmarite publicate", live.size(), published);
        } catch (ApiFootballQuotaExceededException e) {
            log.warn("Poll live oprit (cota atinsa): {}", e.getMessage());
        }
    }

    /** Allowlist de statistici: gol → toate ligile urmarite; altfel doar cele din lista. */
    private boolean statsEligible(Long leagueId) {
        return statsLeagues.isEmpty() || statsLeagues.contains(leagueId);
    }

    private boolean dueForStats(Long fixtureId, Instant now) {
        Instant last = ultimulFetchStats.get(fixtureId);
        return last == null || Duration.between(last, now).toMillis() >= props.statsEveryMs();
    }
}
