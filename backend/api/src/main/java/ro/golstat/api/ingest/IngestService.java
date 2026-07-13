package ro.golstat.api.ingest;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.golstat.api.entity.Coach;
import ro.golstat.api.entity.Country;
import ro.golstat.api.entity.Fixture;
import ro.golstat.api.entity.Injury;
import ro.golstat.api.entity.League;
import ro.golstat.api.entity.Player;
import ro.golstat.api.entity.Season;
import ro.golstat.api.entity.Team;
import ro.golstat.api.entity.Venue;
import ro.golstat.api.repository.CoachRepository;
import ro.golstat.api.repository.CountryRepository;
import ro.golstat.api.repository.FixtureEventRepository;
import ro.golstat.api.repository.FixtureLineupPlayerRepository;
import ro.golstat.api.repository.FixtureLineupRepository;
import ro.golstat.api.repository.FixturePlayerStatsRepository;
import ro.golstat.api.repository.FixtureRepository;
import ro.golstat.api.repository.FixtureTeamStatsRepository;
import ro.golstat.api.repository.InjuryRepository;
import ro.golstat.api.repository.LeagueRepository;
import ro.golstat.api.repository.PlayerRepository;
import ro.golstat.api.repository.PlayerSeasonStatsRepository;
import ro.golstat.api.repository.SeasonRepository;
import ro.golstat.api.repository.StandingRepository;
import ro.golstat.api.repository.TeamRepository;
import ro.golstat.api.repository.TeamSeasonStatsRepository;
import ro.golstat.api.repository.VenueRepository;
import ro.golstat.common.dto.CoachDto;
import ro.golstat.common.dto.FixtureDto;
import ro.golstat.common.dto.FixtureEventDto;
import ro.golstat.common.dto.FixtureLineupDto;
import ro.golstat.common.dto.FixtureLineupPlayerDto;
import ro.golstat.common.dto.FixturePlayerStatsDto;
import ro.golstat.common.dto.FixtureTeamStatsDto;
import ro.golstat.common.dto.InjuryDto;
import ro.golstat.common.dto.LeagueDto;
import ro.golstat.common.dto.PlayerDto;
import ro.golstat.common.dto.PlayerSeasonStatsDto;
import ro.golstat.common.dto.SeasonDto;
import ro.golstat.common.dto.StandingDto;
import ro.golstat.common.dto.TeamDto;
import ro.golstat.common.dto.TeamSeasonStatsDto;
import ro.golstat.common.dto.VenueDto;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private final FixtureTeamStatsRepository teamStats;
    private final FixturePlayerStatsRepository playerStats;
    private final StandingRepository standings;
    private final LeagueRepository leagues;
    private final SeasonRepository seasons;
    private final VenueRepository venues;
    private final CountryRepository countries;
    private final FixtureLineupRepository lineups;
    private final FixtureLineupPlayerRepository lineupPlayers;
    private final InjuryRepository injuries;
    private final PlayerRepository players;
    private final CoachRepository coaches;
    private final TeamSeasonStatsRepository teamSeasonStats;
    private final PlayerSeasonStatsRepository playerSeasonStats;

    public IngestService(TeamRepository teams, FixtureRepository fixtures,
                         FixtureEventRepository events, FixtureTeamStatsRepository teamStats,
                         FixturePlayerStatsRepository playerStats,
                         StandingRepository standings,
                         LeagueRepository leagues, SeasonRepository seasons, VenueRepository venues,
                         CountryRepository countries,
                         FixtureLineupRepository lineups, FixtureLineupPlayerRepository lineupPlayers,
                         InjuryRepository injuries, PlayerRepository players, CoachRepository coaches,
                         TeamSeasonStatsRepository teamSeasonStats,
                         PlayerSeasonStatsRepository playerSeasonStats) {
        this.teams = teams;
        this.fixtures = fixtures;
        this.events = events;
        this.teamStats = teamStats;
        this.playerStats = playerStats;
        this.standings = standings;
        this.leagues = leagues;
        this.seasons = seasons;
        this.venues = venues;
        this.countries = countries;
        this.lineups = lineups;
        this.lineupPlayers = lineupPlayers;
        this.injuries = injuries;
        this.players = players;
        this.coaches = coaches;
        this.teamSeasonStats = teamSeasonStats;
        this.playerSeasonStats = playerSeasonStats;
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

    /** Un lot de jucatori (profil) — upsert pe id. */
    @Transactional
    public void ingestPlayers(List<PlayerDto> batch) {
        for (PlayerDto dto : batch) {
            if (dto.id() != null) {
                players.save(EntityMapper.toPlayer(dto));
            }
        }
    }

    @Transactional
    public void ingestCoach(CoachDto dto) {
        if (dto.id() == null) {
            return;
        }
        coaches.save(EntityMapper.toCoach(dto));
    }

    @Transactional
    public void ingestTeamSeasonStats(TeamSeasonStatsDto dto) {
        if (dto.teamId() == null || dto.leagueId() == null || dto.seasonYear() == null) {
            return;
        }
        ensureSeason(dto.leagueId(), dto.seasonYear());
        ensureTeam(dto.teamId());
        teamSeasonStats.save(EntityMapper.toTeamSeasonStats(dto));
    }

    /**
     * Un lot de statistici de sezon per jucator. FK dur pe {@code player_season_stats.player_id}
     * (+ team, season) → asiguram parintii o SINGURA data per id inainte de save (topicele sosesc asincron).
     */
    @Transactional
    public void ingestPlayerSeasonStats(List<PlayerSeasonStatsDto> batch) {
        List<PlayerSeasonStatsDto> valide = batch.stream()
                .filter(d -> d.playerId() != null && d.teamId() != null
                        && d.leagueId() != null && d.seasonYear() != null)   // PK compus
                .toList();
        if (valide.isEmpty()) {
            return;
        }
        valide.stream().map(PlayerSeasonStatsDto::playerId).distinct().forEach(id -> ensurePlayer(id, "?"));
        valide.stream().map(PlayerSeasonStatsDto::teamId).distinct().forEach(this::ensureTeam);
        valide.stream()
                .map(d -> Map.entry(d.leagueId(), d.seasonYear()))
                .distinct()
                .forEach(e -> ensureSeason(e.getKey(), e.getValue()));
        for (PlayerSeasonStatsDto dto : valide) {
            playerSeasonStats.save(EntityMapper.toPlayerSeasonStats(dto));
        }
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

    /** Un lot de statistici per meci (ambele echipe); upsert pe PK compus (fixtureId, teamId). */
    @Transactional
    public void ingestFixtureTeamStats(List<FixtureTeamStatsDto> batch) {
        if (batch.isEmpty()) {
            return;
        }
        batch.stream().map(FixtureTeamStatsDto::teamId).filter(java.util.Objects::nonNull).distinct().forEach(this::ensureTeam);
        for (FixtureTeamStatsDto dto : batch) {
            teamStats.save(EntityMapper.toFixtureTeamStats(dto));
        }
    }

    /**
     * Un lot de statistici individuale per meci (toti jucatorii, ambele echipe); upsert pe PK compus
     * (fixtureId, playerId). FK-uri dure spre player/team/fixture → asiguram parintii inainte de save.
     */
    @Transactional
    public void ingestFixturePlayerStats(List<FixturePlayerStatsDto> batch) {
        List<FixturePlayerStatsDto> valide = batch.stream()
                .filter(d -> d.fixtureId() != null && d.playerId() != null && d.teamId() != null)
                .toList();
        if (valide.isEmpty()) {
            return;
        }
        // ensure* O SINGURA data per id (vezi nota de la ingestEvents)
        Map<Long, String> jucatori = new LinkedHashMap<>();
        valide.forEach(d -> jucatori.putIfAbsent(d.playerId(), d.playerName()));
        jucatori.forEach(this::ensurePlayer);
        List<Long> teamIds = valide.stream().map(FixturePlayerStatsDto::teamId).distinct().toList();
        teamIds.forEach(this::ensureTeam);
        ensureFixture(valide.get(0).fixtureId(), teamIds.get(0), teamIds.get(teamIds.size() - 1));

        for (FixturePlayerStatsDto dto : valide) {
            playerStats.save(EntityMapper.toFixturePlayerStats(dto));
        }
    }

    /**
     * Un lot de formatii per meci (ambele echipe): sterge setul vechi si il rescrie (replace
     * idempotent). Randurile de jucatori au FK compus spre {@code fixture_lineup} → intai
     * parintii, apoi jucatorii.
     */
    @Transactional
    public void ingestFixtureLineups(List<FixtureLineupDto> batch) {
        List<FixtureLineupDto> valide = batch.stream()
                .filter(d -> d.fixtureId() != null && d.teamId() != null)   // PK compus
                .toList();
        if (valide.isEmpty()) {
            return;
        }
        long fixtureId = valide.get(0).fixtureId();
        // ensure* O SINGURA data per id (vezi nota de la ingestEvents)
        List<Long> teamIds = valide.stream().map(FixtureLineupDto::teamId).distinct().toList();
        teamIds.forEach(this::ensureTeam);
        valide.stream().map(FixtureLineupDto::coachId).filter(java.util.Objects::nonNull).distinct().forEach(this::ensureCoach);
        ensureFixture(fixtureId, teamIds.get(0), teamIds.get(teamIds.size() - 1));

        lineupPlayers.deleteByFixtureId(fixtureId);
        lineups.deleteByFixtureId(fixtureId);
        for (FixtureLineupDto dto : valide) {
            lineups.save(EntityMapper.toFixtureLineup(dto));
        }
        for (FixtureLineupDto dto : valide) {
            if (dto.players() == null) {
                continue;
            }
            for (FixtureLineupPlayerDto p : dto.players()) {
                if (p.playerId() != null) {   // PK compus (fixtureId, playerId)
                    lineupPlayers.save(EntityMapper.toFixtureLineupPlayer(p));
                }
            }
        }
    }

    /**
     * Un lot de indisponibili per liga/sezon: sterge setul vechi si il rescrie (replace idempotent,
     * cheia lotului = liga:sezon). Dedup pe (player, fixture, type) — constrangerea UNIQUE din schema.
     */
    @Transactional
    public void ingestInjuries(List<InjuryDto> batch) {
        if (batch.isEmpty()) {
            return;
        }
        InjuryDto first = batch.get(0);
        injuries.deleteByLeagueIdAndSeasonYear(first.leagueId(), first.seasonYear());

        Set<String> seen = new HashSet<>();
        List<InjuryDto> valide = batch.stream()
                .filter(d -> d.playerId() != null)   // player_id e NOT NULL
                .filter(d -> seen.add(d.playerId() + ":" + d.fixtureId() + ":" + d.type()))
                .toList();

        // ensure* O SINGURA data per id (vezi nota de la ingestEvents)
        Map<Long, String> jucatori = new LinkedHashMap<>();
        valide.forEach(d -> jucatori.putIfAbsent(d.playerId(), d.playerName()));
        jucatori.forEach(this::ensurePlayer);
        valide.stream().map(InjuryDto::teamId).filter(java.util.Objects::nonNull).distinct().forEach(this::ensureTeam);

        Set<Long> meciuriOk = new HashSet<>();
        for (InjuryDto dto : valide) {
            Injury entity = EntityMapper.toInjury(dto);
            if (dto.fixtureId() != null && !meciuriOk.contains(dto.fixtureId())) {
                if (dto.teamId() != null) {
                    ensureFixture(dto.fixtureId(), dto.teamId(), dto.teamId());
                    meciuriOk.add(dto.fixtureId());
                } else if (fixtures.existsById(dto.fixtureId())) {
                    meciuriOk.add(dto.fixtureId());
                } else {
                    entity.setFixtureId(null);   // fara echipa nu putem crea placeholder de meci (FK)
                }
            }
            injuries.save(entity);
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
        placeholder.setIsCurrent(false);   // is_current e NOT NULL; Hibernate ar insera null si ar sari default-ul din schema
        seasons.save(placeholder);
    }

    private void ensurePlayer(Long playerId, String name) {
        if (playerId == null || players.existsById(playerId)) {
            return;
        }
        Player placeholder = new Player();
        placeholder.setId(playerId);
        placeholder.setName(name);   // numele vine cu mesajul (ex. injuries) — util la afisare
        players.save(placeholder);
    }

    private void ensureCoach(Long coachId) {
        if (coachId == null || coaches.existsById(coachId)) {
            return;
        }
        Coach placeholder = new Coach();
        placeholder.setId(coachId);
        coaches.save(placeholder);
    }

    /**
     * Coloanele NOT NULL ale meciului cer valori: sezon santinela 0/0 si echipele primite
     * (apelantul le-a asigurat deja cu ensureTeam); mesajul real de pe topicul fixtures il
     * suprascrie (acelasi id).
     */
    private void ensureFixture(Long fixtureId, Long teamA, Long teamB) {
        if (fixtureId == null || fixtures.existsById(fixtureId)) {
            return;
        }
        ensureSeason(0L, 0);
        Fixture placeholder = new Fixture();
        placeholder.setId(fixtureId);
        placeholder.setLeagueId(0L);
        placeholder.setSeasonYear(0);
        placeholder.setHomeTeamId(teamA);
        placeholder.setAwayTeamId(teamB);
        fixtures.save(placeholder);
    }
}
