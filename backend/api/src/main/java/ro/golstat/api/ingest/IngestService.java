package ro.golstat.api.ingest;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.golstat.api.entity.Country;
import ro.golstat.api.entity.League;
import ro.golstat.api.entity.Season;
import ro.golstat.api.entity.Team;
import ro.golstat.api.entity.Venue;
import ro.golstat.api.repository.CountryRepository;
import ro.golstat.api.repository.FixtureEventRepository;
import ro.golstat.api.repository.FixtureRepository;
import ro.golstat.api.repository.LeagueRepository;
import ro.golstat.api.repository.SeasonRepository;
import ro.golstat.api.repository.StandingRepository;
import ro.golstat.api.repository.TeamRepository;
import ro.golstat.api.repository.VenueRepository;
import ro.golstat.common.dto.FixtureDto;
import ro.golstat.common.dto.FixtureEventDto;
import ro.golstat.common.dto.LeagueDto;
import ro.golstat.common.dto.SeasonDto;
import ro.golstat.common.dto.StandingDto;
import ro.golstat.common.dto.TeamDto;
import ro.golstat.common.dto.VenueDto;

import java.util.List;

/**
 * Persista datele venite din Kafka in Postgres, IDEMPOTENT (upsert pe id natural — at-least-once
 * ⇒ reprocesarea da acelasi rezultat).
 *
 * <p>Fixtures/standings au FK spre {@code venue}, {@code league}, {@code season}, {@code team}.
 * Ca sa NU depindem de ordinea mesajelor intre topicuri, metodele {@code ensureX} insereaza un
 * rand minimal daca parintele lipseste; mesajul real de catalog il suprascrie ulterior.
 */
@Service
public class IngestService {

    private final TeamRepository teams;
    private final FixtureRepository fixtures;
    private final FixtureEventRepository events;
    private final StandingRepository standings;
    private final LeagueRepository leagues;
    private final SeasonRepository seasons;
    private final VenueRepository venues;
    private final CountryRepository countries;

    public IngestService(TeamRepository teams, FixtureRepository fixtures,
                         FixtureEventRepository events, StandingRepository standings,
                         LeagueRepository leagues, SeasonRepository seasons, VenueRepository venues,
                         CountryRepository countries) {
        this.teams = teams;
        this.fixtures = fixtures;
        this.events = events;
        this.standings = standings;
        this.leagues = leagues;
        this.seasons = seasons;
        this.venues = venues;
        this.countries = countries;
    }

    // --- catalog ---

    @Transactional
    public void ingestLeague(LeagueDto dto) {
        ensureCountry(dto.countryName());
        leagues.save(EntityMapper.toLeague(dto));
    }

    @Transactional
    public void ingestSeason(SeasonDto dto) {
        ensureLeague(dto.leagueId());
        seasons.save(EntityMapper.toSeason(dto));
    }

    @Transactional
    public void ingestVenue(VenueDto dto) {
        ensureCountry(dto.countryName());
        venues.save(EntityMapper.toVenue(dto));
    }

    @Transactional
    public void ingestTeam(TeamDto dto) {
        ensureCountry(dto.countryName());
        ensureVenue(dto.venueId());   // echipa are FK spre stadion
        teams.save(EntityMapper.toTeam(dto));
    }

    // --- meciuri ---

    @Transactional
    public void ingestFixture(FixtureDto dto) {
        ensureVenue(dto.venueId());
        ensureSeason(dto.leagueId(), dto.seasonYear());
        ensureTeam(dto.homeTeamId());
        ensureTeam(dto.awayTeamId());
        fixtures.save(EntityMapper.toFixture(dto));
    }

    @Transactional
    public void ingestStanding(StandingDto dto) {
        ensureSeason(dto.leagueId(), dto.seasonYear());
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
        // acelasi teamId apare de mai multe ori intr-un meci (ex. 2 goluri) → ensureTeam O SINGURA data,
        // altfel doua INSERT-uri placeholder cu acelasi id in aceeasi tranzactie = duplicate key.
        batch.stream().map(FixtureEventDto::teamId).filter(java.util.Objects::nonNull).distinct().forEach(this::ensureTeam);
        for (FixtureEventDto dto : batch) {
            events.save(EntityMapper.toFixtureEvent(dto));
        }
    }

    // --- ensure-exists pentru FK-uri (randuri minimale, suprascrise de mesajele reale) ---

    private void ensureTeam(Long teamId) {
        if (teamId == null || teams.existsById(teamId)) {
            return;
        }
        Team placeholder = new Team();
        placeholder.setId(teamId);
        placeholder.setName("?");        // NOT NULL in schema
        placeholder.setIsNational(false); // NOT NULL in schema
        teams.save(placeholder);
    }

    /** league/team/venue au FK spre country(name); cream un rand minimal daca lipseste (imbogatit ulterior). */
    private void ensureCountry(String name) {
        if (name == null || countries.existsById(name)) {
            return;
        }
        Country placeholder = new Country();
        placeholder.setName(name);
        countries.save(placeholder);
    }

    private void ensureVenue(Long venueId) {
        if (venueId == null || venues.existsById(venueId)) {
            return;
        }
        Venue placeholder = new Venue();
        placeholder.setId(venueId);
        placeholder.setName("?");   // NOT NULL in schema
        venues.save(placeholder);
    }

    private void ensureLeague(Long leagueId) {
        if (leagueId == null || leagues.existsById(leagueId)) {
            return;
        }
        League placeholder = new League();
        placeholder.setId(leagueId);
        placeholder.setName("?");   // NOT NULL in schema
        leagues.save(placeholder);
    }

    private void ensureSeason(Long leagueId, Integer year) {
        if (leagueId == null || year == null || seasons.existsById(new Season.Pk(leagueId, year))) {
            return;
        }
        ensureLeague(leagueId);     // season are FK spre league
        Season placeholder = new Season();
        placeholder.setLeagueId(leagueId);
        placeholder.setYear(year);
        seasons.save(placeholder);
    }
}
