package ro.golstat.collector.collection;

import org.junit.jupiter.api.Test;
import ro.golstat.collector.provider.StubDataProvider;
import ro.golstat.collector.publish.EventPublisher;
import ro.golstat.common.GolstatConstants;
import ro.golstat.common.dto.FixtureDto;

import java.time.LocalDate;
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
        new CollectionService(new StubDataProvider(), pub)
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
        new CollectionService(new StubDataProvider(), pub)
                .collectGoalsData(1, 2024, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));
        assertEquals(4, pub.countOn(GolstatConstants.KafkaTopics.TEAMS));
        assertEquals(4, pub.countOn(GolstatConstants.KafkaTopics.STANDINGS));
        assertEquals(0, pub.countOn(GolstatConstants.KafkaTopics.FIXTURES));
        assertEquals(0, pub.countOn(GolstatConstants.KafkaTopics.FIXTURE_EVENTS));
    }
}
