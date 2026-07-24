package ro.golstat.api.live;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ro.golstat.api.ingest.IngestService;
import ro.golstat.api.repository.FixtureRepository;
import ro.golstat.api.stats.LeagueSeason;
import ro.golstat.api.web.LiveBroadcaster;
import ro.golstat.collector.live.LiveProperties;
import ro.golstat.collector.provider.DataProvider;
import ro.golstat.common.GolstatConstants;
import ro.golstat.common.dto.FixtureDto;
import ro.golstat.common.dto.FixtureLiveDto;
import ro.golstat.common.dto.FixtureTeamStatsDto;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Bucla LIVE gazduita in API (proces mereu pornit), echivalentul lui {@code LivePoller} din colector.
 * La fiecare {@code golstat.live.poll-ms} cere meciurile in desfasurare ({@code fixtures?live=all}) si
 * pentru cele din ligile urmarite: le persista (upsert idempotent) si le difuzeaza pe WebSocket, DIRECT
 * — fara Kafka, fiind un singur proces care detine si ingest-ul, si broadcaster-ul.
 *
 * <p>Doua diferente fata de varianta din colector, ambele fiindca API-ul ARE baza de date:
 * <ul>
 *   <li><b>gating din DB</b>: polim doar cand exista un meci in fereastra de joc ({@code existsByKickoffBetween}),
 *       nu dintr-un orar in memorie populat de colectare;</li>
 *   <li><b>ligile urmarite</b> = ligile deja colectate ({@code league}), nu o lista de config.</li>
 * </ul>
 * Bean activ doar cu {@code golstat.live.enabled=true}.
 */
@Component
@ConditionalOnProperty(name = "golstat.live.enabled", havingValue = "true")
public class LiveApiPoller {

    private static final Logger log = LoggerFactory.getLogger(LiveApiPoller.class);

    private final DataProvider provider;
    private final IngestService ingest;
    private final LiveBroadcaster broadcaster;
    private final FixtureRepository fixtures;
    private final LiveProperties props;
    private final Clock clock;
    private final Set<Long> statsLeagues;
    /** Ultimul moment cand am cerut statistici per meci; curatat cand meciul nu mai e live. */
    private final Map<Long, Instant> ultimulFetchStats = new HashMap<>();
    /** Meciurile urmarite live la poll-ul anterior; ce iese din set = tocmai s-a terminat → il finalizam (FT). */
    private final Set<Long> liveAnterior = new HashSet<>();

    public LiveApiPoller(DataProvider provider, IngestService ingest, LiveBroadcaster broadcaster,
                         FixtureRepository fixtures, LiveProperties props, Clock clock) {
        this.provider = provider;
        this.ingest = ingest;
        this.broadcaster = broadcaster;
        this.fixtures = fixtures;
        this.props = props;
        this.clock = clock;
        this.statsLeagues = Set.copyOf(props.statsLeagues());
    }

    @Scheduled(fixedDelayString = "${golstat.live.poll-ms:15000}")
    public void poll() {
        Instant now = clock.instant();
        OffsetDateTime lower = now.minus(Duration.ofMinutes(props.windowBeforeMinutes())).atOffset(ZoneOffset.UTC);
        OffsetDateTime upper = now.plus(Duration.ofMinutes(props.windowAfterMinutes())).atOffset(ZoneOffset.UTC);
        if (!fixtures.existsByKickoffBetween(lower, upper)) {
            liveAnterior.clear();   // in afara ferestrei nu urmarim nimic → nu ardem un request
            return;
        }

        Set<Long> tracked = trackedLeagues();
        try {
            List<FixtureLiveDto> live = provider.liveFixtures();
            Set<Long> liveTracked = new HashSet<>();
            for (FixtureLiveDto fl : live) {
                FixtureDto f = fl.fixture();
                if (f == null || f.id() == null || f.leagueId() == null || !tracked.contains(f.leagueId())) {
                    continue;
                }
                liveTracked.add(f.id());
                ingest.ingestFixture(f);
                broadcaster.broadcast(f);   // push LIVE daca meciul e in desfasurare

                if (fl.evenimente() != null && !fl.evenimente().isEmpty()) {
                    ingest.ingestEvents(fl.evenimente());
                }

                if (statsEligible(f.leagueId()) && dueForStats(f.id(), now)) {
                    List<FixtureTeamStatsDto> stats = provider.liveFixtureStatistics(f.id());
                    if (!stats.isEmpty()) {
                        ingest.ingestFixtureTeamStats(stats);
                    }
                    ultimulFetchStats.put(f.id(), now);
                }
            }
            // meciurile care erau live si acum nu mai sunt = tocmai s-au terminat: le luam starea finala
            // (FT + scor final) o data, altfel ar ramane inghetate in DB la ultimul snapshot „live".
            Set<Long> tocmaiTerminate = new HashSet<>(liveAnterior);
            tocmaiTerminate.removeAll(liveTracked);
            if (!tocmaiTerminate.isEmpty()) {
                for (FixtureDto f : provider.fixturesByIds(tocmaiTerminate)) {
                    if (f != null && f.id() != null) {
                        ingest.ingestFixture(f);
                    }
                }
            }
            liveAnterior.clear();
            liveAnterior.addAll(liveTracked);
            ultimulFetchStats.keySet().removeIf(id -> !liveTracked.contains(id));
            log.debug("Live API: {} in desfasurare, {} urmarite", live.size(), liveTracked.size());
        } catch (RuntimeException e) {
            // cota atinsa, esec de retea sau Redis indisponibil: bucla programata trebuie sa supravietuiasca
            // pana la urmatorul poll, nu sa cada cu stack trace la fiecare 15s.
            log.warn("Poll live esuat: {}", e.toString());
        }
    }

    /**
     * Ligile urmarite = cele care AU meciuri terminate colectate (aceeasi definitie ca pagina de
     * statistici). NU tabelul {@code league} — acela e catalogul COMPLET API-Football (~1200 ligi), iar
     * filtrarea pe el ar lasa sa treaca orice meci live de pe glob (India, Coreea...). Meciurile live
     * dintr-o liga necolectata n-au istoric terminal, deci raman filtrate — si nu se pot auto-include:
     * neingerate, nu devin niciodata terminale.
     */
    private Set<Long> trackedLeagues() {
        Set<Long> ids = new HashSet<>();
        for (LeagueSeason ls : fixtures.ligiCuMeciuriJucate(GolstatConstants.FixtureStatus.TERMINAL)) {
            ids.add(ls.getLeagueId());
        }
        return ids;
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
