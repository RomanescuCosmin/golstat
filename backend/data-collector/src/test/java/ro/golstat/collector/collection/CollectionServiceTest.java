package ro.golstat.collector.collection;

import org.junit.jupiter.api.Test;
import ro.golstat.collector.live.LiveSchedule;
import ro.golstat.collector.provider.DataProvider;
import ro.golstat.collector.provider.StubDataProvider;
import ro.golstat.collector.publish.EventPublisher;
import ro.golstat.common.GolstatConstants;
import ro.golstat.common.dto.FixtureDto;
import ro.golstat.common.dto.FixtureEventDto;
import ro.golstat.common.dto.FixtureLineupDto;
import ro.golstat.common.dto.FixtureLineupPlayerDto;
import ro.golstat.common.dto.FixtureTeamStatsDto;
import ro.golstat.common.dto.InjuryDto;
import ro.golstat.common.dto.LeagueDto;
import ro.golstat.common.dto.SeasonDto;
import ro.golstat.common.dto.StandingDto;
import ro.golstat.common.dto.TeamDto;
import ro.golstat.common.dto.VenueDto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CollectionServiceTest {

    /** Publisher care doar retine ce s-a trimis, ca sa verificam topic/cheie/payload. */
    private static final class RecordingPublisher implements EventPublisher {
        record Message(String topic, String key, Object payload) {
        }

        final List<Message> sent = new ArrayList<>();

        @Override
        public void publish(String topic, String key, Object payload) {
            sent.add(new Message(topic, key, payload));
        }

        long countOn(String topic) {
            return sent.stream().filter(m -> m.topic().equals(topic)).count();
        }

        Message first(String topic) {
            return sent.stream().filter(m -> m.topic().equals(topic)).findFirst().orElseThrow();
        }
    }

    private static int firstIndexOn(List<RecordingPublisher.Message> sent, String topic) {
        for (int i = 0; i < sent.size(); i++) {
            if (sent.get(i).topic().equals(topic)) {
                return i;
            }
        }
        return -1;
    }

    private static int lastIndexOn(List<RecordingPublisher.Message> sent, String topic) {
        for (int i = sent.size() - 1; i >= 0; i--) {
            if (sent.get(i).topic().equals(topic)) {
                return i;
            }
        }
        return -1;
    }

    private RecordingPublisher collectStub() {
        RecordingPublisher pub = new RecordingPublisher();
        new CollectionService(new StubDataProvider(), pub, new LiveSchedule())
                .collectGoalsData(1, 2024, LocalDate.of(2024, 8, 1), LocalDate.of(2024, 9, 30));
        return pub;
    }

    @Test
    void publishesCatalogTeamsStandingsFixturesAndEvents() {
        RecordingPublisher pub = collectStub();
        assertEquals(1, pub.countOn(GolstatConstants.KafkaTopics.VENUES));
        assertEquals(1, pub.countOn(GolstatConstants.KafkaTopics.LEAGUES));
        assertEquals(1, pub.countOn(GolstatConstants.KafkaTopics.SEASONS));
        assertEquals(4, pub.countOn(GolstatConstants.KafkaTopics.TEAMS));
        assertEquals(4, pub.countOn(GolstatConstants.KafkaTopics.STANDINGS));
        assertEquals(6, pub.countOn(GolstatConstants.KafkaTopics.FIXTURES));
        assertEquals(6, pub.countOn(GolstatConstants.KafkaTopics.FIXTURE_EVENTS)); // un lot per meci
    }

    @Test
    void catalogPublishedBeforeFixtures() {
        // FK: fixtures referentiaza venue/league/season/team → catalogul precede meciurile
        List<RecordingPublisher.Message> sent = collectStub().sent;
        int firstFixture = firstIndexOn(sent, GolstatConstants.KafkaTopics.FIXTURES);
        assertTrue(lastIndexOn(sent, GolstatConstants.KafkaTopics.TEAMS) < firstFixture, "echipele preced meciurile");
        assertTrue(lastIndexOn(sent, GolstatConstants.KafkaTopics.SEASONS) < firstFixture, "sezoanele preced meciurile");
        assertTrue(lastIndexOn(sent, GolstatConstants.KafkaTopics.VENUES) < firstFixture, "stadioanele preced meciurile");
    }

    @Test
    void fixtureKeyIsFixtureId() {
        RecordingPublisher.Message msg = collectStub().first(GolstatConstants.KafkaTopics.FIXTURES);
        FixtureDto fixture = assertInstanceOf(FixtureDto.class, msg.payload());
        assertEquals(String.valueOf(fixture.id()), msg.key());
    }

    @Test
    void eventsArePublishedAsBatchKeyedByFixture() {
        RecordingPublisher.Message msg = collectStub().first(GolstatConstants.KafkaTopics.FIXTURE_EVENTS);
        assertInstanceOf(List.class, msg.payload());          // lot, nu eveniment singular
        assertTrue(msg.key().matches("\\d+"));                 // cheia = fixtureId
    }

    @Test
    void standingKeyIsLeagueSeasonTeam() {
        RecordingPublisher.Message msg = collectStub().first(GolstatConstants.KafkaTopics.STANDINGS);
        assertTrue(msg.key().matches("1:2024:\\d+"), "cheia clasamentului: " + msg.key());
    }

    @Test
    void emptyWindow_publishesTeamsAndStandingsButNoFixtures() {
        RecordingPublisher pub = new RecordingPublisher();
        new CollectionService(new StubDataProvider(), pub, new LiveSchedule())
                .collectGoalsData(1, 2024, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));
        assertEquals(4, pub.countOn(GolstatConstants.KafkaTopics.TEAMS));
        assertEquals(4, pub.countOn(GolstatConstants.KafkaTopics.STANDINGS));
        assertEquals(0, pub.countOn(GolstatConstants.KafkaTopics.FIXTURES));
        assertEquals(0, pub.countOn(GolstatConstants.KafkaTopics.FIXTURE_EVENTS));
    }

    @Test
    void eventsRequestedOnlyForTerminalFixtures() {
        StatusProvider provider = new StatusProvider();
        RecordingPublisher pub = new RecordingPublisher();
        new CollectionService(provider, pub, new LiveSchedule())
                .collectGoalsData(1, 2026, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 31));

        assertEquals(2, pub.countOn(GolstatConstants.KafkaTopics.FIXTURES), "ambele meciuri publicate");
        assertEquals(1, pub.countOn(GolstatConstants.KafkaTopics.FIXTURE_EVENTS), "doar meciul FT are evenimente");
        assertEquals(List.of(200L), provider.eventsAskedFor, "NS nu declanseaza cerere de evenimente");
        assertEquals("200", pub.first(GolstatConstants.KafkaTopics.FIXTURE_EVENTS).key());
    }

    @Test
    void statisticsRequestedOnlyForTerminalFixtures_publishedAsBatch() {
        StatusProvider provider = new StatusProvider();
        RecordingPublisher pub = new RecordingPublisher();
        new CollectionService(provider, pub, new LiveSchedule())
                .collectGoalsData(1, 2026, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 31));

        assertEquals(List.of(200L), provider.statsAskedFor, "NS nu declanseaza cerere de statistici");
        assertEquals(1, pub.countOn(GolstatConstants.KafkaTopics.FIXTURE_TEAM_STATS));
        RecordingPublisher.Message msg = pub.first(GolstatConstants.KafkaTopics.FIXTURE_TEAM_STATS);
        assertEquals("200", msg.key());
        List<?> batch = assertInstanceOf(List.class, msg.payload());   // lot: ambele echipe intr-un mesaj
        assertEquals(2, batch.size());
    }

    @Test
    void emptyStatistics_notPublished() {
        StatusProvider provider = new StatusProvider();
        provider.statsAvailable = false;
        RecordingPublisher pub = new RecordingPublisher();
        new CollectionService(provider, pub, new LiveSchedule())
                .collectGoalsData(1, 2026, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 31));

        assertEquals(0, pub.countOn(GolstatConstants.KafkaTopics.FIXTURE_TEAM_STATS));
    }

    @Test
    void lineupsRequestedForTerminalAndNotStarted_publishedAsBatchPerFixture() {
        StatusProvider provider = new StatusProvider();
        RecordingPublisher pub = new RecordingPublisher();
        new CollectionService(provider, pub, new LiveSchedule())
                .collectGoalsData(1, 2026, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 31));

        // lineup-urile se cer si pentru NS (apar aproape de kickoff), nu doar pentru FT
        assertEquals(List.of(200L, 201L), provider.lineupsAskedFor);
        assertEquals(2, pub.countOn(GolstatConstants.KafkaTopics.FIXTURE_LINEUPS));
        RecordingPublisher.Message msg = pub.first(GolstatConstants.KafkaTopics.FIXTURE_LINEUPS);
        assertEquals("200", msg.key());
        List<?> batch = assertInstanceOf(List.class, msg.payload());   // lot: ambele formatii intr-un mesaj
        assertEquals(2, batch.size());
    }

    @Test
    void emptyLineups_notPublished() {
        StatusProvider provider = new StatusProvider();
        provider.lineupsAvailable = false;
        RecordingPublisher pub = new RecordingPublisher();
        new CollectionService(provider, pub, new LiveSchedule())
                .collectGoalsData(1, 2026, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 31));

        assertEquals(0, pub.countOn(GolstatConstants.KafkaTopics.FIXTURE_LINEUPS));
    }

    @Test
    void injuriesPublishedAsBatchKeyedByLeagueSeason() {
        StatusProvider provider = new StatusProvider();
        RecordingPublisher pub = new RecordingPublisher();
        new CollectionService(provider, pub, new LiveSchedule())
                .collectGoalsData(1, 2026, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 31));

        assertEquals(1, pub.countOn(GolstatConstants.KafkaTopics.INJURIES));
        RecordingPublisher.Message msg = pub.first(GolstatConstants.KafkaTopics.INJURIES);
        assertEquals("1:2026", msg.key());
        List<?> batch = assertInstanceOf(List.class, msg.payload());
        assertEquals(1, batch.size());
    }

    /** Furnizor cu un meci terminat (200, FT) si unul viitor (201, NS); retine pentru cine s-au cerut evenimente/statistici. */
    private static final class StatusProvider implements DataProvider {
        final List<Long> eventsAskedFor = new ArrayList<>();
        final List<Long> statsAskedFor = new ArrayList<>();
        final List<Long> lineupsAskedFor = new ArrayList<>();
        boolean statsAvailable = true;
        boolean lineupsAvailable = true;

        @Override
        public List<FixtureDto> fixtures(long leagueId, int season, LocalDate from, LocalDate to) {
            return List.of(fixture(200, GolstatConstants.FixtureStatus.FINISHED),
                    fixture(201, GolstatConstants.FixtureStatus.NOT_STARTED));
        }

        @Override
        public List<FixtureEventDto> fixtureEvents(long fixtureId) {
            eventsAskedFor.add(fixtureId);
            return List.of(new FixtureEventDto(fixtureId, 1L, null, null, 10, null,
                    GolstatConstants.EventType.GOAL, "Normal Goal", null));
        }

        @Override
        public List<FixtureTeamStatsDto> fixtureStatistics(long fixtureId) {
            statsAskedFor.add(fixtureId);
            if (!statsAvailable) {
                return List.of();
            }
            return List.of(teamStats(fixtureId, 1L), teamStats(fixtureId, 2L));
        }

        private static FixtureTeamStatsDto teamStats(long fixtureId, long teamId) {
            return new FixtureTeamStatsDto(fixtureId, teamId, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null);
        }

        @Override
        public List<FixtureLineupDto> fixtureLineups(long fixtureId) {
            lineupsAskedFor.add(fixtureId);
            if (!lineupsAvailable) {
                return List.of();
            }
            return List.of(lineup(fixtureId, 1L), lineup(fixtureId, 2L));
        }

        private static FixtureLineupDto lineup(long fixtureId, long teamId) {
            return new FixtureLineupDto(fixtureId, teamId, "4-4-2", 5L, List.of(
                    new FixtureLineupPlayerDto(fixtureId, teamId, 100 + teamId, "Portar", 1, "G", "1:1", false)));
        }

        @Override
        public List<InjuryDto> injuries(long leagueId, int season) {
            return List.of(new InjuryDto(900L, "Jucator Accidentat", 1L, 200L, leagueId, season,
                    "Missing Fixture", "Knee Injury", LocalDate.of(2026, 6, 10)));
        }

        @Override
        public List<FixtureDto> liveFixtures() {
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

        private static FixtureDto fixture(long id, String statusShort) {
            return new FixtureDto(
                    id, null, "UTC", OffsetDateTime.of(2026, 6, 15, 18, 0, 0, 0, ZoneOffset.UTC),
                    1L, 2026, "Regular Season - 1", 1L, "long", statusShort, 90,
                    1L, 2L, 1, 0, 1, 0, 1, 0, null, null, null, null);
        }
    }
}
