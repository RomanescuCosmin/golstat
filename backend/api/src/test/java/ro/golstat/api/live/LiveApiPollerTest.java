package ro.golstat.api.live;

import org.junit.jupiter.api.Test;
import ro.golstat.api.ingest.IngestService;
import ro.golstat.api.repository.FixtureRepository;
import ro.golstat.api.stats.LeagueSeason;
import ro.golstat.api.web.LiveBroadcaster;
import ro.golstat.collector.live.LiveProperties;
import ro.golstat.collector.provider.DataProvider;
import ro.golstat.collector.provider.apifootball.ApiFootballQuotaExceededException;
import ro.golstat.common.GolstatConstants;
import ro.golstat.common.dto.FixtureDto;
import ro.golstat.common.dto.FixtureEventDto;
import ro.golstat.common.dto.FixtureLineupDto;
import ro.golstat.common.dto.FixtureLiveDto;
import ro.golstat.common.dto.FixtureTeamStatsDto;
import ro.golstat.common.dto.InjuryDto;
import ro.golstat.common.dto.LeagueDto;
import ro.golstat.common.dto.SeasonDto;
import ro.golstat.common.dto.StandingDto;
import ro.golstat.common.dto.TeamDto;
import ro.golstat.common.dto.VenueDto;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Bucla LIVE din API: gating pe DB, filtrare pe ligile urmarite, ingest + broadcast direct, reziliere la cota. */
class LiveApiPollerTest {

    private static final Instant NOW = Instant.parse("2026-07-03T18:00:00Z");

    private final FakeLiveProvider provider = new FakeLiveProvider();
    private final IngestService ingest = mock(IngestService.class);
    private final LiveBroadcaster broadcaster = mock(LiveBroadcaster.class);
    private final FixtureRepository fixtures = mock(FixtureRepository.class);
    private final MutableClock clock = new MutableClock(NOW);

    private LiveApiPoller poller() {
        return poller(new LiveProperties(true, 15000, 140, 15, 120000, List.of(), 70));
    }

    private LiveApiPoller poller(LiveProperties props) {
        return new LiveApiPoller(provider, ingest, broadcaster, fixtures, props, clock);
    }

    private void matchInWindow() {
        when(fixtures.existsByKickoffBetween(any(), any())).thenReturn(true);
    }

    /** Ligile urmarite vin din meciurile terminate colectate (proiectia {@link LeagueSeason}). */
    private void tracked(long... ids) {
        List<LeagueSeason> ls = new ArrayList<>();
        for (long id : ids) {
            LeagueSeason proj = mock(LeagueSeason.class);
            when(proj.getLeagueId()).thenReturn(id);
            ls.add(proj);
        }
        when(fixtures.ligiCuMeciuriJucate(any())).thenReturn(ls);
    }

    @Test
    void inWindow_ingestsAndBroadcastsTrackedLeaguesOnly() {
        matchInWindow();
        tracked(39L, 1L);
        provider.live = List.of(live(100, 39), live(200, 1), live(300, 140));   // 300 = La Liga, NEurmarita

        poller().poll();

        verify(ingest).ingestFixture(argThat(f -> f.id() == 100L));
        verify(ingest).ingestFixture(argThat(f -> f.id() == 200L));
        verify(ingest, never()).ingestFixture(argThat(f -> f.id() == 300L));
        verify(broadcaster).broadcast(argThat(f -> f.id() == 100L));
        verify(broadcaster).broadcast(argThat(f -> f.id() == 200L));
    }

    @Test
    void outsideWindow_doesNotHitProvider() {
        when(fixtures.existsByKickoffBetween(any(), any())).thenReturn(false);

        poller().poll();

        assertEquals(0, provider.liveCalls, "fara meci in fereastra → niciun request live");
        verifyNoInteractions(ingest, broadcaster);
    }

    @Test
    void quotaExceeded_logsAndSkips_withoutThrowing() {
        matchInWindow();
        tracked(39L);
        provider.throwQuota = true;

        assertDoesNotThrow(() -> poller().poll());
        verifyNoInteractions(ingest);
    }

    @Test
    void inlineEvents_ingestedPerFixture() {
        matchInWindow();
        tracked(39L);
        provider.live = List.of(live(100, 39, List.of(goal(100, 1L, 23))));

        poller().poll();

        verify(ingest).ingestEvents(argThat(batch -> batch.size() == 1));
    }

    @Test
    void statsThrottled_fetchedOncePerCadence() {
        matchInWindow();
        tracked(39L);
        provider.live = List.of(live(100, 39));
        provider.stats = List.of(teamStats(100, 1L));

        LiveApiPoller poller = poller();       // ACELASI poller pastreaza harta de throttling intre poll-uri
        poller.poll();                         // t0: prima cerere de statistici
        clock.advanceMillis(60_000);           // +60s (< 120s)
        poller.poll();                         // nu reincarca
        clock.advanceMillis(70_000);           // +130s cumulat (>= 120s)
        poller.poll();                         // reincarca

        assertEquals(List.of(100L, 100L), provider.statsAskedFor, "statistici la t0 si dupa cadenta, nu la +60s");
    }

    @Test
    void justEnded_finalizedOnceFromProvider() {
        matchInWindow();
        tracked(39L);
        LiveApiPoller poller = poller();

        provider.live = List.of(live(100, 39));
        poller.poll();                          // 100 e live

        provider.live = List.of();              // 100 a disparut din live=all → tocmai s-a terminat
        provider.byIds = List.of(fixture(100, 39));
        poller.poll();

        assertTrue(provider.byIdsAskedFor.contains(100L), "meciul terminat e cerut o data pentru starea finala");
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
        List<FixtureDto> byIds = List.of();
        boolean throwQuota = false;
        int liveCalls = 0;
        final List<Long> statsAskedFor = new ArrayList<>();
        final List<Long> byIdsAskedFor = new ArrayList<>();

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
        public List<FixtureDto> fixturesByIds(Collection<Long> fixtureIds) {
            byIdsAskedFor.addAll(fixtureIds);
            return byIds;
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
        public List<FixtureTeamStatsDto> fixtureStatistics(long fixtureId) {
            return List.of();
        }

        @Override
        public List<FixtureLineupDto> fixtureLineups(long fixtureId) {
            return List.of();
        }

        @Override
        public List<InjuryDto> injuries(long leagueId, int season) {
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
