package ro.golstat.api.ingest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ro.golstat.collector.publish.EventPublisher;
import ro.golstat.common.GolstatConstants.KafkaTopics;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Publicare FARA Kafka: payload-ul trece prin acelasi JSON si aceiasi listeneri ca pe drumul normal,
 * doar ca in-proces. Folosit de importul one-shot cand nu exista broker (ex. laptop fara Docker).
 */
public class DirectEventPublisher implements EventPublisher {

    private final ObjectMapper mapper;
    private final Map<String, Consumer<String>> rute;

    public DirectEventPublisher(DataIngestListeners l, ReprizeIngestListener reprize, ObjectMapper mapper) {
        this.mapper = mapper;
        this.rute = Map.ofEntries(
                Map.entry(KafkaTopics.VENUES, l::onVenue),
                Map.entry(KafkaTopics.LEAGUES, l::onLeague),
                Map.entry(KafkaTopics.SEASONS, l::onSeason),
                Map.entry(KafkaTopics.TEAMS, l::onTeam),
                Map.entry(KafkaTopics.FIXTURES, l::onFixture),
                Map.entry(KafkaTopics.STANDINGS, l::onStanding),
                Map.entry(KafkaTopics.COACHES, l::onCoach),
                Map.entry(KafkaTopics.TEAM_SEASON_STATS, l::onTeamSeasonStats),
                Map.entry(KafkaTopics.PLAYERS, l::onPlayers),
                Map.entry(KafkaTopics.PLAYER_SEASON_STATS, l::onPlayerSeasonStats),
                Map.entry(KafkaTopics.FIXTURE_EVENTS, l::onEvents),
                Map.entry(KafkaTopics.FIXTURE_TEAM_STATS, l::onFixtureTeamStats),
                Map.entry(KafkaTopics.FIXTURE_TEAM_HALF_STATS, reprize::onReprize),
                Map.entry(KafkaTopics.FIXTURE_PLAYER_STATS, l::onFixturePlayerStats),
                Map.entry(KafkaTopics.FIXTURE_LINEUPS, l::onFixtureLineups),
                Map.entry(KafkaTopics.INJURIES, l::onInjuries));
    }

    @Override
    public void publish(String topic, String key, Object payload) {
        Consumer<String> ruta = rute.get(topic);
        if (ruta == null) {
            throw new IllegalArgumentException("Topic necunoscut: " + topic);
        }
        try {
            ruta.accept(mapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Nu pot serializa payload-ul pentru " + topic, e);
        }
    }
}
