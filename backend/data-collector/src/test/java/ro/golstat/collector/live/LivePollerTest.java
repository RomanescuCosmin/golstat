package ro.golstat.collector.live;

import org.junit.jupiter.api.Test;
import ro.golstat.collector.collection.CollectionProperties;
import ro.golstat.collector.collection.LeagueTarget;
import ro.golstat.collector.provider.DataProvider;
import ro.golstat.collector.provider.apifootball.ApiFootballQuotaExceededException;
import ro.golstat.collector.publish.EventPublisher;
import ro.golstat.common.GolstatConstants;
import ro.golstat.common.dto.FixtureDto;
import ro.golstat.common.dto.FixtureEventDto;
import ro.golstat.common.dto.FixtureLiveDto;
import ro.golstat.common.dto.FixtureTeamStatsDto;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LivePollerTest {

    private static final Instant NOW = Instant.parse("2026-07-03T18:00:00Z");

    private final FakeLiveProvider provider = new FakeLiveProvider();
    private final RecordingPublisher publisher = new RecordingPublisher();
    private final LiveSchedule schedule = new LiveSchedule();
    private final MutableClock clock = new MutableClock(NOW);

    private LivePoller poller() {
        return poller(new LiveProperties(true, 15000, 180, 15, 120000, List.of()));
    }

    private LivePoller poller(LiveProperties props) {
        CollectionProperties collection = new CollectionProperties(
                List.of(new LeagueTarget(39, 2025, false), new LeagueTarget(1, 2026, false)), 90, 10);
        return new LivePoller(provider, publisher, schedule, props, collection, clock);
    }

    private void matchInWindow() {
        schedule.replaceForLeague(39L, List.of(OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC)));
    }

    @Test
    void inWindow_publishesOnlyTrackedLeagues() {
        matchInWindow();
        provider.live = List.of(
                live(100, 39),    // urmarita
                live(200, 1),     // urmarita
                live(300, 140));  // NEurmarita (La Liga)

        poller().poll();

        List<RecordingPublisher.Msg> fixtures = publisher.on(GolstatConstants.KafkaTopics.FIXTURES);
        assertEquals(2, fixtures.size());
        assertEquals(List.of("100", "200"), fixtures.stream().map(RecordingPublisher.Msg::key).toList());
    }

    @Test
    void inlineEvents_publishedAsBatchPerFixture() {
        matchInWindow();
        provider.live = List.of(live(100, 39, List.of(goal(100, 1L, 23))));

        poller().poll();

        List<RecordingPublisher.Msg> events = publisher.on(GolstatConstants.KafkaTopics.FIXTURE_EVENTS);
        assertEquals(1, events.size());
        assertEquals("100", events.get(0).key());
        List<?> batch = assertInstanceOf(List.class, events.get(0).payload());
        assertEquals(1, batch.size());
    }

    @Test
    void statsThrottled_fetchedOncePerCadence() {
        matchInWindow();
        provider.live = List.of(live(100, 39));
        provider.stats = List.of(teamStats(100, 1L), teamStats(100, 2L));

        LivePoller poller = poller();          // ACELASI poller pastreaza harta de throttling intre poll-uri
        poller.poll();                         // t0: prima cerere de statistici
        clock.advanceMillis(60_000);           // +60s (< 120s cadenta)
        poller.poll();                         // nu reincarca statisticile
        clock.advanceMillis(70_000);           // +130s cumulat (>= 120s)
        poller.poll();                         // reincarca

        assertEquals(List.of(100L, 100L), provider.statsAskedFor, "statistici cerute la t0 si dupa cadenta, nu la +60s");
        assertEquals(2, publisher.on(GolstatConstants.KafkaTopics.FIXTURE_TEAM_STATS).size());
    }

    @Test
    void statsAllowlist_skipsLeaguesNotListed() {
        matchInWindow();
        provider.live = List.of(live(100, 39), live(200, 1));
        provider.stats = List.of(teamStats(0, 1L));

        poller(new LiveProperties(true, 15000, 180, 15, 120000, List.of(39L))).poll();

        assertEquals(List.of(100L), provider.statsAskedFor, "doar liga 39 e in allowlist-ul de statistici");
    }

    @Test
    void outsideWindow_doesNotHitProvider() {
        poller().poll();
        assertEquals(0, provider.liveCalls, "fara meci in fereastra → niciun request live");
        assertTrue(publisher.sent.isEmpty());
    }

    @Test
    void quotaExceeded_logsAndSkips_withoutThrowing() {
        matchInWindow();
        provider.throwQuota = true;
        poller().poll();   // nu arunca
        assertTrue(publisher.sent.isEmpty());
    }

    private static FixtureLiveDto live(long id, long leagueId) {
        return live(id, leagueId, List.of());
    }

    private static FixtureLiveDto live(long id, long leagueId, List<FixtureEventDto> events) {
        return new FixtureLiveDto(fixture(id, leagueId), events);
    }

    private static FixtureDto fixture(long id, long leagueId) {
        return new FixtureDto(id, null, "UTC", OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC),
                leagueId, 2026, "R", null, "long", GolstatConstants.FixtureStatus.FIRST_HALF, 30,
                1L, 2L, 1, 0, 0, 0, null, null, null, null, null, null);
    }

    private static FixtureEventDto goal(long fixtureId, long teamId, int minute) {
        return new FixtureEventDto(fixtureId, teamId, null, null, minute, null,
                GolstatConstants.EventType.GOAL, "Normal Goal", null);
    }

    private static FixtureTeamStatsDto teamStats(long fixtureId, long teamId) {
        return new FixtureTeamStatsDto(fixtureId, teamId, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null);
    }

    private static final class RecordingPublisher implements EventPublisher {
        record Msg(String topic, String key, Object payload) {
        }

        final List<Msg> sent = new ArrayList<>();

        @Override
        public void publish(String topic, String key, Object payload) {
            sent.add(new Msg(topic, key, payload));
        }

        List<Msg> on(String topic) {
            return sent.stream().filter(m -> m.topic().equals(topic)).toList();
        }
    }

    /** Clock mutabil pentru testarea cadentei de statistici. */
    private static final class MutableClock extends Clock {
        private Instant instant;

        MutableClock(Instant start) {
            this.instant = start;
        }

        void advanceMillis(long millis) {
            instant = instant.plusMillis(millis);
        }

        @Override
        public Instant instant() {
            return instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }

    /** Furnizor care intoarce meciuri live configurate (sau arunca cota) si numara apelurile. */
    private static final class FakeLiveProvider implements DataProvider {
        List<FixtureLiveDto> live = List.of();
        List<FixtureTeamStatsDto> stats = List.of();
        boolean throwQuota = false;
        int liveCalls = 0;
        final List<Long> statsAskedFor = new ArrayList<>();

        @Override
        public List<FixtureLiveDto> liveFixtures() {
            liveCalls++;
            if (throwQuota) {
                throw new ApiFootballQuotaExceededException("/fixtures?live=all");
            }
            return live;
        }

        @Override
        public List<FixtureTeamStatsDto> liveFixtureStatistics(long fixtureId) {
            statsAskedFor.add(fixtureId);
            return stats;
        }

        @Override
        public List<FixtureDto> fixtures(long leagueId, int season, java.time.LocalDate from, java.time.LocalDate to) {
            return List.of();
        }

        @Override
        public List<FixtureEventDto> fixtureEvents(long fixtureId) {
            return List.of();
        }

        @Override
        public List<FixtureTeamStatsDto> fixtureStatistics(long fixtureId) {
            return List.of();
        }

        @Override
        public List<ro.golstat.common.dto.FixtureLineupDto> fixtureLineups(long fixtureId) {
            return List.of();
        }

        @Override
        public List<ro.golstat.common.dto.InjuryDto> injuries(long leagueId, int season) {
            return List.of();
        }

        @Override
        public List<ro.golstat.common.dto.StandingDto> standings(long leagueId, int season) {
            return List.of();
        }

        @Override
        public List<ro.golstat.common.dto.TeamDto> teams(long leagueId, int season) {
            return List.of();
        }

        @Override
        public List<ro.golstat.common.dto.LeagueDto> leagues() {
            return List.of();
        }

        @Override
        public List<ro.golstat.common.dto.SeasonDto> seasons(long leagueId) {
            return List.of();
        }

        @Override
        public List<ro.golstat.common.dto.VenueDto> venues() {
            return List.of();
        }
    }
}
