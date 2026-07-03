package ro.golstat.api.ingest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ro.golstat.api.web.LiveBroadcaster;
import ro.golstat.common.GolstatConstants;
import ro.golstat.common.dto.FixtureDto;
import ro.golstat.common.dto.FixtureEventDto;
import ro.golstat.common.dto.FixtureLineupDto;
import ro.golstat.common.dto.FixtureTeamStatsDto;
import ro.golstat.common.dto.InjuryDto;
import ro.golstat.common.dto.LeagueDto;
import ro.golstat.common.dto.SeasonDto;
import ro.golstat.common.dto.StandingDto;
import ro.golstat.common.dto.TeamDto;
import ro.golstat.common.dto.VenueDto;

import java.util.List;

/**
 * Consuma topicurile Kafka (JSON ca String) si deleaga persistarea la {@link IngestService}.
 * Deserializam manual cu ObjectMapper ca sa evitam ambiguitatile de tip ale JsonDeserializer
 * pe topicuri cu tipuri diferite.
 */
@Component
public class DataIngestListeners {

    private final IngestService ingest;
    private final ObjectMapper mapper;
    private final LiveBroadcaster liveBroadcaster;

    public DataIngestListeners(IngestService ingest, ObjectMapper mapper, LiveBroadcaster liveBroadcaster) {
        this.ingest = ingest;
        this.mapper = mapper;
        this.liveBroadcaster = liveBroadcaster;
    }

    @KafkaListener(topics = GolstatConstants.KafkaTopics.VENUES)
    void onVenue(String json) {
        ingest.ingestVenue(read(json, VenueDto.class));
    }

    @KafkaListener(topics = GolstatConstants.KafkaTopics.LEAGUES)
    void onLeague(String json) {
        ingest.ingestLeague(read(json, LeagueDto.class));
    }

    @KafkaListener(topics = GolstatConstants.KafkaTopics.SEASONS)
    void onSeason(String json) {
        ingest.ingestSeason(read(json, SeasonDto.class));
    }

    @KafkaListener(topics = GolstatConstants.KafkaTopics.TEAMS)
    void onTeam(String json) {
        ingest.ingestTeam(read(json, TeamDto.class));
    }

    @KafkaListener(topics = GolstatConstants.KafkaTopics.FIXTURES)
    void onFixture(String json) {
        FixtureDto fixture = read(json, FixtureDto.class);
        ingest.ingestFixture(fixture);
        liveBroadcaster.broadcast(fixture);   // push LIVE daca meciul e in desfasurare
    }

    @KafkaListener(topics = GolstatConstants.KafkaTopics.STANDINGS)
    void onStanding(String json) {
        ingest.ingestStanding(read(json, StandingDto.class));
    }

    @KafkaListener(topics = GolstatConstants.KafkaTopics.FIXTURE_EVENTS)
    void onEvents(String json) {
        try {
            ingest.ingestEvents(mapper.readValue(json, new TypeReference<List<FixtureEventDto>>() {
            }));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON invalid pe " + GolstatConstants.KafkaTopics.FIXTURE_EVENTS, e);
        }
    }

    @KafkaListener(topics = GolstatConstants.KafkaTopics.FIXTURE_TEAM_STATS)
    void onFixtureTeamStats(String json) {
        try {
            ingest.ingestFixtureTeamStats(mapper.readValue(json, new TypeReference<List<FixtureTeamStatsDto>>() {
            }));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON invalid pe " + GolstatConstants.KafkaTopics.FIXTURE_TEAM_STATS, e);
        }
    }

    @KafkaListener(topics = GolstatConstants.KafkaTopics.FIXTURE_LINEUPS)
    void onFixtureLineups(String json) {
        try {
            ingest.ingestFixtureLineups(mapper.readValue(json, new TypeReference<List<FixtureLineupDto>>() {
            }));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON invalid pe " + GolstatConstants.KafkaTopics.FIXTURE_LINEUPS, e);
        }
    }

    @KafkaListener(topics = GolstatConstants.KafkaTopics.INJURIES)
    void onInjuries(String json) {
        try {
            ingest.ingestInjuries(mapper.readValue(json, new TypeReference<List<InjuryDto>>() {
            }));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON invalid pe " + GolstatConstants.KafkaTopics.INJURIES, e);
        }
    }

    private <T> T read(String json, Class<T> type) {
        try {
            return mapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON invalid pentru " + type.getSimpleName(), e);
        }
    }
}
