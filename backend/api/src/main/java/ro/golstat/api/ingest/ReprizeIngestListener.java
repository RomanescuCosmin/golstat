package ro.golstat.api.ingest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ro.golstat.api.entity.FixtureTeamHalfStats;
import ro.golstat.api.repository.FixtureTeamHalfStatsRepository;
import ro.golstat.common.GolstatConstants;
import ro.golstat.common.dto.FixtureTeamHalfStatsDto;

import java.util.List;

/** Persista statisticile pe reprize (upsert pe meci + echipa + repriza). */
@Component
public class ReprizeIngestListener {

    private final FixtureTeamHalfStatsRepository reprize;
    private final ObjectMapper mapper;

    public ReprizeIngestListener(FixtureTeamHalfStatsRepository reprize, ObjectMapper mapper) {
        this.reprize = reprize;
        this.mapper = mapper;
    }

    @KafkaListener(topics = GolstatConstants.KafkaTopics.FIXTURE_TEAM_HALF_STATS)
    @Transactional
    public void onReprize(String json) {
        List<FixtureTeamHalfStatsDto> batch;
        try {
            batch = mapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "JSON invalid pe " + GolstatConstants.KafkaTopics.FIXTURE_TEAM_HALF_STATS, e);
        }
        salveaza(batch);
    }

    /** Upsert pe (meci, echipa, repriza); folosit si de bucla live la finalul meciului. */
    @Transactional
    public void salveaza(List<FixtureTeamHalfStatsDto> batch) {
        for (FixtureTeamHalfStatsDto d : batch) {
            if (d.fixtureId() == null || d.teamId() == null || d.half() == null) {
                continue;
            }
            FixtureTeamHalfStats s = new FixtureTeamHalfStats();
            s.setFixtureId(d.fixtureId());
            s.setTeamId(d.teamId());
            s.setHalf(d.half());
            s.setCornerKicks(d.cornerKicks());
            s.setFouls(d.fouls());
            s.setYellowCards(d.yellowCards());
            s.setRedCards(d.redCards());
            s.setShotsTotal(d.shotsTotal());
            s.setShotsOnGoal(d.shotsOnGoal());
            s.setOffsides(d.offsides());
            reprize.save(s);
        }
    }
}
