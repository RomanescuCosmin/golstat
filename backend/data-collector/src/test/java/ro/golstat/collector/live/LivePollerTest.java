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
import ro.golstat.common.dto.LeagueDto;
import ro.golstat.common.dto.SeasonDto;
import ro.golstat.common.dto.StandingDto;
import ro.golstat.common.dto.TeamDto;
import ro.golstat.common.dto.VenueDto;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LivePollerTest {

    private static final Instant NOW = Instant.parse("2026-07-03T18:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final FakeLiveProvider provider = new FakeLiveProvider();
    private final RecordingPublisher publisher = new RecordingPublisher();
    private final LiveSchedule schedule = new LiveSchedule();

    private LivePoller poller() {
        CollectionProperties collection = new CollectionProperties(
                List.of(new LeagueTarget(39, 2025), new LeagueTarget(1, 2026)), 90, 10);
        LiveProperties props = new LiveProperties(true, 15000, 180, 15);
        return new LivePoller(provider, publisher, schedule, props, collection, CLOCK);
    }

    private void matchInWindow() {
        schedule.replaceForLeague(39L, List.of(OffsetDateTime.parse("2026-07-03T17:40:00Z")));
    }

    @Test
    void inWindow_publishesOnlyTrackedLeagues() {
        matchInWindow();
        provider.live = List.of(
                fixture(100, 39),    // urmarita
                fixture(200, 1),     // urmarita
                fixture(300, 140));  // NEurmarita (La Liga)

        poller().poll();

        assertEquals(2, publisher.sent.size());
        assertTrue(publisher.sent.stream().allMatch(m -> m.topic().equals(GolstatConstants.KafkaTopics.FIXTURES)));
        assertEquals(List.of("100", "200"), publisher.sent.stream().map(RecordingPublisher.Msg::key).toList());
    }

    @Test
    void outsideWindow_doesNotHitProvider() {
        // schedule gol → nicio fereastra de meci
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

    private static FixtureDto fixture(long id, long leagueId) {
        return new FixtureDto(id, null, "UTC", OffsetDateTime.parse("2026-07-03T17:40:00Z"),
                leagueId, 2026, "R", null, "long", GolstatConstants.FixtureStatus.FIRST_HALF, 30,
                1L, 2L, 1, 0, 0, 0, null, null, null, null, null, null);
    }

    private static final class RecordingPublisher implements EventPublisher {
        record Msg(String topic, String key, Object payload) {
        }

        final List<Msg> sent = new ArrayList<>();

        @Override
        public void publish(String topic, String key, Object payload) {
            sent.add(new Msg(topic, key, payload));
        }
    }

    /** Furnizor care intoarce meciuri live configurate (sau arunca cota) si numara apelurile. */
    private static final class FakeLiveProvider implements DataProvider {
        List<FixtureDto> live = List.of();
        boolean throwQuota = false;
        int liveCalls = 0;

        @Override
        public List<FixtureDto> liveFixtures() {
            liveCalls++;
            if (throwQuota) {
                throw new ApiFootballQuotaExceededException("/fixtures?live=all");
            }
            return live;
        }

        @Override
        public List<FixtureDto> fixtures(long leagueId, int season, LocalDate from, LocalDate to) {
            return List.of();
        }

        @Override
        public List<FixtureEventDto> fixtureEvents(long fixtureId) {
            return List.of();
        }

        @Override
        public List<StandingDto> standings(long leagueId, int season) {
            return List.of();
        }

        @Override
        public List<TeamDto> teams(long leagueId, int season) {
            return List.of();
        }

        @Override
        public List<LeagueDto> leagues() {
            return List.of();
        }

        @Override
        public List<SeasonDto> seasons(long leagueId) {
            return List.of();
        }

        @Override
        public List<VenueDto> venues() {
            return List.of();
        }
    }
}
