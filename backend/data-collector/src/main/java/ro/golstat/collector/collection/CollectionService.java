package ro.golstat.collector.collection;

import org.springframework.stereotype.Service;
import ro.golstat.collector.provider.DataProvider;
import ro.golstat.collector.publish.EventPublisher;
import ro.golstat.common.GolstatConstants;
import ro.golstat.common.dto.FixtureDto;
import ro.golstat.common.dto.FixtureEventDto;
import ro.golstat.common.dto.StandingDto;
import ro.golstat.common.dto.TeamDto;

import java.time.LocalDate;
import java.util.List;

/**
 * Colecteaza datele pentru piata de goluri de la {@link DataProvider} si le publica in Kafka.
 *
 * <p>Chei de mesaj (pentru topicuri compacte):
 * <ul>
 *   <li>fixtures → {@code fixtureId} (ultima stare a meciului castiga)</li>
 *   <li>standings → {@code leagueId:season:teamId} (ultimul clasament per echipa/sezon)</li>
 *   <li>events → {@code fixtureId}, publicate ca LOT (o lista per meci): evenimentele n-au id
 *       natural stabil, iar un lot per meci face topicul compactabil si idempotent
 *       (re-colectarea unui meci inlocuieste tot setul de evenimente).</li>
 * </ul>
 */
@Service
public class CollectionService {

    private final DataProvider provider;
    private final EventPublisher publisher;

    public CollectionService(DataProvider provider, EventPublisher publisher) {
        this.provider = provider;
        this.publisher = publisher;
    }

    public void collectGoalsData(long leagueId, int season, LocalDate from, LocalDate to) {
        // Echipele intai: fixtures/standings au FK spre team(id).
        for (TeamDto team : provider.teams(leagueId, season)) {
            publisher.publish(GolstatConstants.KafkaTopics.TEAMS, String.valueOf(team.id()), team);
        }

        for (StandingDto standing : provider.standings(leagueId, season)) {
            publisher.publish(GolstatConstants.KafkaTopics.STANDINGS, standingKey(standing), standing);
        }

        for (FixtureDto fixture : provider.fixtures(leagueId, season, from, to)) {
            publisher.publish(GolstatConstants.KafkaTopics.FIXTURES, String.valueOf(fixture.id()), fixture);

            List<FixtureEventDto> events = provider.fixtureEvents(fixture.id());
            if (!events.isEmpty()) {
                publisher.publish(GolstatConstants.KafkaTopics.FIXTURE_EVENTS, String.valueOf(fixture.id()), events);
            }
        }
    }

    private static String standingKey(StandingDto standing) {
        return standing.leagueId() + ":" + standing.seasonYear() + ":" + standing.teamId();
    }
}
