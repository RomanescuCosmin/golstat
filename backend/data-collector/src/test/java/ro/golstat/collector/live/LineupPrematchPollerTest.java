package ro.golstat.collector.live;

import org.junit.jupiter.api.Test;
import ro.golstat.collector.collection.CollectionProperties;
import ro.golstat.collector.collection.LeagueTarget;
import ro.golstat.collector.provider.DataProvider;
import ro.golstat.collector.provider.apifootball.ApiFootballQuotaExceededException;
import ro.golstat.collector.publish.EventPublisher;
import ro.golstat.common.GolstatConstants;
import ro.golstat.common.dto.FixtureDto;
import ro.golstat.common.dto.FixtureLineupDto;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LineupPrematchPollerTest {

    private static final Instant NOW = Instant.parse("2026-07-07T18:00:00Z");

    private final FakeProvider provider = new FakeProvider();
    private final RecordingPublisher publisher = new RecordingPublisher();

    private LineupPrematchPoller poller() {
        CollectionProperties collection = CollectionProperties.scheduled(List.of(
                LeagueTarget.zilnica(39, 2025, false, true, false, false),
                LeagueTarget.zilnica(667, 2026, true, true, false, false)),   // amicale: doar fixtures → fara lineups
                90, 10);
        LiveProperties props = new LiveProperties(true, 15000, 180, 15, 120000, List.of(), 70);
        return new LineupPrematchPoller(provider, publisher, collection, props,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static FixtureDto fixture(long id, String status, Instant kickoff) {
        return new FixtureDto(id, null, "UTC", OffsetDateTime.ofInstant(kickoff, ZoneOffset.UTC),
                39L, 2025, "R", null, "long", status, null,
                1L, 2L, null, null, null, null, null, null, null, null, null, null);
    }

    private static FixtureLineupDto lineup(long fixtureId, long teamId) {
        return new FixtureLineupDto(fixtureId, teamId, "4-3-3", null, List.of());
    }

    @Test
    void meciInFereastra_lineupsPublicateCaLot_apoiNuMaiSuntCerute() {
        provider.fixtures = List.of(fixture(100, GolstatConstants.FixtureStatus.NOT_STARTED,
                NOW.plusSeconds(30 * 60)));
        provider.lineups = List.of(lineup(100, 1), lineup(100, 2));

        LineupPrematchPoller poller = poller();
        poller.poll();
        poller.poll();   // al doilea tick: meciul e deja publicat → nu se mai cere

        assertEquals(List.of(100L), provider.lineupsAskedFor);
        List<RecordingPublisher.Msg> publicate = publisher.on(GolstatConstants.KafkaTopics.FIXTURE_LINEUPS);
        assertEquals(1, publicate.size());
        assertEquals("100", publicate.get(0).key());
        List<?> lot = assertInstanceOf(List.class, publicate.get(0).payload());
        assertEquals(2, lot.size());
    }

    @Test
    void raspunsGol_seReincearcaLaTickulUrmator() {
        provider.fixtures = List.of(fixture(100, GolstatConstants.FixtureStatus.NOT_STARTED,
                NOW.plusSeconds(30 * 60)));
        provider.lineups = List.of();   // inca neanuntate

        LineupPrematchPoller poller = poller();
        poller.poll();
        poller.poll();

        assertEquals(List.of(100L, 100L), provider.lineupsAskedFor, "gol → reincercat la fiecare tick");
        assertTrue(publisher.sent.isEmpty());
    }

    @Test
    void inAfaraFerestrei_sauStatusGresit_nuSeCer() {
        provider.fixtures = List.of(
                fixture(100, GolstatConstants.FixtureStatus.NOT_STARTED, NOW.plusSeconds(3 * 3600)), // prea devreme
                fixture(101, GolstatConstants.FixtureStatus.FIRST_HALF, NOW),                        // deja live
                fixture(102, GolstatConstants.FixtureStatus.NOT_STARTED, NOW.minusSeconds(3600)));   // demult trecut

        poller().poll();

        assertEquals(List.of(), provider.lineupsAskedFor);
    }

    @Test
    void ligaDoarFixtures_esteSarita() {
        provider.fixtures = List.of(fixture(100, GolstatConstants.FixtureStatus.NOT_STARTED,
                NOW.plusSeconds(30 * 60)));

        poller().poll();

        // fixtures cerute doar pentru liga 39 (nu si pentru amicale 667)
        assertEquals(List.of(39L), provider.fixturesAskedForLeague);
    }

    @Test
    void cotaAtinsa_seOpresteFaraSaArunce() {
        provider.fixtures = List.of(fixture(100, GolstatConstants.FixtureStatus.NOT_STARTED,
                NOW.plusSeconds(30 * 60)));
        provider.throwQuota = true;

        poller().poll();   // nu arunca

        assertTrue(publisher.sent.isEmpty());
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

    private static final class FakeProvider implements DataProvider {
        List<FixtureDto> fixtures = List.of();
        List<FixtureLineupDto> lineups = List.of();
        boolean throwQuota = false;
        final List<Long> lineupsAskedFor = new ArrayList<>();
        final List<Long> fixturesAskedForLeague = new ArrayList<>();

        @Override
        public List<FixtureDto> fixtures(long leagueId, int season, LocalDate from, LocalDate to) {
            fixturesAskedForLeague.add(leagueId);
            return fixtures;
        }

        @Override
        public List<FixtureLineupDto> upcomingFixtureLineups(long fixtureId) {
            if (throwQuota) {
                throw new ApiFootballQuotaExceededException("/fixtures/lineups");
            }
            lineupsAskedFor.add(fixtureId);
            return lineups;
        }

        @Override
        public List<ro.golstat.common.dto.FixtureEventDto> fixtureEvents(long fixtureId) {
            return List.of();
        }

        @Override
        public List<ro.golstat.common.dto.FixtureTeamStatsDto> fixtureStatistics(long fixtureId) {
            return List.of();
        }

        @Override
        public List<FixtureLineupDto> fixtureLineups(long fixtureId) {
            return List.of();
        }

        @Override
        public List<ro.golstat.common.dto.InjuryDto> injuries(long leagueId, int season) {
            return List.of();
        }

        @Override
        public List<ro.golstat.common.dto.FixtureLiveDto> liveFixtures() {
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
