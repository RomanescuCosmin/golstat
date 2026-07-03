package ro.golstat.api.ingest;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.golstat.api.entity.Team;
import ro.golstat.api.repository.FixtureEventRepository;
import ro.golstat.api.repository.FixtureRepository;
import ro.golstat.api.repository.StandingRepository;
import ro.golstat.api.repository.TeamRepository;
import ro.golstat.common.dto.FixtureDto;
import ro.golstat.common.dto.FixtureEventDto;
import ro.golstat.common.dto.StandingDto;
import ro.golstat.common.dto.TeamDto;

import java.util.List;

/**
 * Persista datele venite din Kafka in Postgres, IDEMPOTENT (upsert pe id natural — at-least-once
 * ⇒ reprocesarea da acelasi rezultat).
 *
 * <p>Fixtures/standings au FK spre {@code team(id)}. Ca sa nu depindem de ordinea mesajelor intre
 * topicuri, {@link #ensureTeam} insereaza un rand minimal de echipa daca lipseste; mesajul real
 * de echipa il suprascrie ulterior.
 */
@Service
public class IngestService {

    private final TeamRepository teams;
    private final FixtureRepository fixtures;
    private final FixtureEventRepository events;
    private final StandingRepository standings;

    public IngestService(TeamRepository teams, FixtureRepository fixtures,
                         FixtureEventRepository events, StandingRepository standings) {
        this.teams = teams;
        this.fixtures = fixtures;
        this.events = events;
        this.standings = standings;
    }

    @Transactional
    public void ingestTeam(TeamDto dto) {
        teams.save(EntityMapper.toTeam(dto));
    }

    @Transactional
    public void ingestFixture(FixtureDto dto) {
        ensureTeam(dto.homeTeamId());
        ensureTeam(dto.awayTeamId());
        fixtures.save(EntityMapper.toFixture(dto));
    }

    @Transactional
    public void ingestStanding(StandingDto dto) {
        ensureTeam(dto.teamId());
        standings.save(EntityMapper.toStanding(dto));
    }

    /** Un lot de evenimente per meci: sterge vechile evenimente si le rescrie (replace idempotent). */
    @Transactional
    public void ingestEvents(List<FixtureEventDto> batch) {
        if (batch.isEmpty()) {
            return;
        }
        events.deleteByFixtureId(batch.get(0).fixtureId());
        for (FixtureEventDto dto : batch) {
            ensureTeam(dto.teamId());
            events.save(EntityMapper.toFixtureEvent(dto));
        }
    }

    /** Insereaza o echipa minimala (id + nume placeholder) daca nu exista deja. */
    private void ensureTeam(Long teamId) {
        if (teamId == null || teams.existsById(teamId)) {
            return;
        }
        Team placeholder = new Team();
        placeholder.setId(teamId);
        placeholder.setName("?");   // NOT NULL in schema; suprascris de mesajul real de echipa
        teams.save(placeholder);
    }
}
