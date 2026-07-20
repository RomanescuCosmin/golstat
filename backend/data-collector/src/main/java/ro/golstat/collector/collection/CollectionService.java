package ro.golstat.collector.collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ro.golstat.collector.live.LiveSchedule;
import ro.golstat.collector.provider.DataProvider;
import ro.golstat.collector.provider.apifootball.ApiFootballQuotaExceededException;
import ro.golstat.collector.publish.EventPublisher;
import ro.golstat.common.GolstatConstants;
import ro.golstat.common.dto.CoachDto;
import ro.golstat.common.dto.FixtureDto;
import ro.golstat.common.dto.FixtureEventDto;
import ro.golstat.common.dto.FixtureLineupDto;
import ro.golstat.common.dto.FixturePlayerStatsDto;
import ro.golstat.common.dto.FixtureTeamStatsDto;
import ro.golstat.common.dto.InjuryDto;
import ro.golstat.common.dto.LeagueDto;
import ro.golstat.common.dto.PlayerDto;
import ro.golstat.common.dto.PlayerSeasonStatsDto;
import ro.golstat.common.dto.PlayerSezonDto;
import ro.golstat.common.dto.SeasonDto;
import ro.golstat.common.dto.StandingDto;
import ro.golstat.common.dto.TeamDto;
import ro.golstat.common.dto.TeamSeasonStatsDto;
import ro.golstat.common.dto.VenueDto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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
 *   <li>fixture-team-stats → {@code fixtureId}, tot ca LOT (ambele echipe intr-un mesaj).</li>
 *   <li>fixture-lineups → {@code fixtureId}, LOT (ambele formatii intr-un mesaj).</li>
 *   <li>fixture-player-stats → {@code fixtureId}, LOT (toti jucatorii ambelor echipe); doar la tintele
 *       cu {@code statisticiJucatori} si unde furnizorul are acoperire.</li>
 *   <li>injuries → {@code leagueId:season}, LOT (toata lista ligii; re-colectarea inlocuieste setul).</li>
 * </ul>
 */
@Service
public class CollectionService {

    private static final Logger log = LoggerFactory.getLogger(CollectionService.class);

    /** Doar meciurile terminate au evenimente/statistici stabile; unul NS/live n-are → nu-i cerem (economie de cota). */
    private static final Set<String> TERMINAL = Set.of(
            GolstatConstants.FixtureStatus.FINISHED,
            GolstatConstants.FixtureStatus.FINISHED_AET,
            GolstatConstants.FixtureStatus.FINISHED_PEN
    );

    private final DataProvider provider;
    private final EventPublisher publisher;
    private final LiveSchedule liveSchedule;

    public CollectionService(DataProvider provider, EventPublisher publisher, LiveSchedule liveSchedule) {
        this.provider = provider;
        this.publisher = publisher;
        this.liveSchedule = liveSchedule;
    }

    public void collectGoalsData(long leagueId, int season, LocalDate from, LocalDate to) {
        collectGoalsData(leagueId, season, from, to, false);
    }

    public void collectGoalsData(long leagueId, int season, LocalDate from, LocalDate to, boolean doarFixtures) {
        collectGoalsData(LeagueTarget.zilnica(leagueId, season, doarFixtures, true, false, false), from, to);
    }

    public void collectGoalsData(LeagueTarget target, LocalDate from, LocalDate to) {
        long leagueId = target.leagueId();
        int season = target.season();
        boolean doarFixtures = target.doarFixtures();

        // Catalog intai: fixtures/standings au FK spre venue/league/season/team.
        for (VenueDto venue : provider.venues()) {
            publisher.publish(GolstatConstants.KafkaTopics.VENUES, String.valueOf(venue.id()), venue);
        }
        for (LeagueDto league : provider.leagues()) {
            publisher.publish(GolstatConstants.KafkaTopics.LEAGUES, String.valueOf(league.id()), league);
        }
        // Sezoanele aduc si ACOPERIREA furnizorului pentru sezonul tinta — gratis (apelul se face oricum),
        // si ne scuteste de a cere per meci date care nu exista (ex. statistici la cupe/amicale).
        SeasonDto acoperire = null;
        for (SeasonDto season2 : provider.seasons(leagueId)) {
            publisher.publish(GolstatConstants.KafkaTopics.SEASONS, seasonKey(season2), season2);
            if (season2.year() != null && season2.year() == season) {
                acoperire = season2;
            }
        }
        // Acoperirea declarata de furnizor e doar un INDICIU, nu o interdictie: la sezoanele proaspat
        // incepute o raporteaza gresit (Liga I 2026 declara statistics_fixtures=false desi datele exista).
        // Cand zice "nu", sondam primul meci terminat si lasam raspunsul real sa decida pentru restul ligii.
        Boolean statisticiMeciDisponibile = dupaAcoperire(
                acoperire != null ? acoperire.hasStatisticsFixtures() : null);
        Boolean statisticiJucatoriDisponibile = target.statisticiJucatori()
                ? dupaAcoperire(acoperire != null ? acoperire.hasStatisticsPlayers() : null)
                : Boolean.FALSE;

        List<TeamDto> teams = provider.teams(leagueId, season);
        for (TeamDto team : teams) {
            publisher.publish(GolstatConstants.KafkaTopics.TEAMS, String.valueOf(team.id()), team);
        }
        if (!doarFixtures) {
            for (StandingDto standing : provider.standings(leagueId, season)) {
                publisher.publish(GolstatConstants.KafkaTopics.STANDINGS, standingKey(standing), standing);
            }
        }

        List<FixtureDto> fixtures = provider.fixtures(leagueId, season, from, to);
        // Publica INTAI toate meciurile + orarul LIVE + logheaza (ieftin, un singur apel API pentru toata
        // lista), ca sa avem programul/amicalele/live-ul chiar daca detaliile scumpe de mai jos raman
        // blocate de cota (enrichment-ul de echipe/jucatori era cel care ardea cota inainte de fixtures).
        for (FixtureDto fixture : fixtures) {
            publisher.publish(GolstatConstants.KafkaTopics.FIXTURES, String.valueOf(fixture.id()), fixture);
        }
        List<OffsetDateTime> kickoffs = fixtures.stream()
                .map(FixtureDto::kickoff)
                .filter(Objects::nonNull)
                .toList();
        liveSchedule.replaceForLeague(leagueId, kickoffs);
        log.info("Colectat liga {} sezon {}: {} fixtures in fereastra {}..{}",
                leagueId, season, fixtures.size(), from, to);

        if (doarFixtures) {
            // Competitie „doar meciuri" (ex. amicale): oprim aici — fara detalii per-meci si fara
            // imbogatire de echipe, ca sa nu ardem cota API pe mii de meciuri care ne intereseaza doar ca scor.
            return;
        }

        boolean doarNote = target.doarStatisticiJucatori();

        // Abia apoi detaliile per meci (lineups/events/stats) — pot esua fara sa pierdem meciurile deja publicate.
        for (FixtureDto fixture : fixtures) {
            boolean terminal = TERMINAL.contains(fixture.statusShort());
            // lineup-urile PRE-meci le aduce LineupPrematchPoller (TTL 0, fereastra de kickoff);
            // aici le cerem doar pentru meciurile terminate, unde raspunsul e stabil si cache-uibil
            if (terminal && !doarNote) {
                List<FixtureLineupDto> lineups = provider.fixtureLineups(fixture.id());
                if (!lineups.isEmpty()) {
                    publisher.publish(GolstatConstants.KafkaTopics.FIXTURE_LINEUPS, String.valueOf(fixture.id()), lineups);
                }
            }
            if (!terminal) {
                continue;   // meci viitor/live → fara evenimente
            }
            if (!doarNote) {
                List<FixtureEventDto> events = provider.fixtureEvents(fixture.id());
                if (!events.isEmpty()) {
                    publisher.publish(GolstatConstants.KafkaTopics.FIXTURE_EVENTS, String.valueOf(fixture.id()), events);
                }
            }
            if (statisticiMeciDisponibile != Boolean.FALSE && !doarNote) {
                List<FixtureTeamStatsDto> teamStats = provider.fixtureStatistics(fixture.id());
                if (!teamStats.isEmpty()) {
                    statisticiMeciDisponibile = Boolean.TRUE;
                    publisher.publish(GolstatConstants.KafkaTopics.FIXTURE_TEAM_STATS, String.valueOf(fixture.id()), teamStats);
                } else if (statisticiMeciDisponibile == null) {
                    statisticiMeciDisponibile = Boolean.FALSE;
                    log.info("Liga {} sezon {}: sonda confirma ca nu exista statistici de meci — le sarim", leagueId, season);
                }
            }
            if (statisticiJucatoriDisponibile != Boolean.FALSE) {
                List<FixturePlayerStatsDto> playerStats = provider.fixturePlayerStatistics(fixture.id());
                if (!playerStats.isEmpty()) {
                    statisticiJucatoriDisponibile = Boolean.TRUE;
                    publisher.publish(GolstatConstants.KafkaTopics.FIXTURE_PLAYER_STATS,
                            String.valueOf(fixture.id()), playerStats);
                } else if (statisticiJucatoriDisponibile == null) {
                    statisticiJucatoriDisponibile = Boolean.FALSE;
                    log.info("Liga {} sezon {}: sonda confirma ca nu exista note de jucatori — le sarim", leagueId, season);
                }
            }
        }

        if (!target.imbogatireEchipe()) {
            // competitie cu foarte multe echipe (ex. amicale): detaliile per meci ne ajung; lotul,
            // statisticile de sezon si indisponibilii ar costa ~4 requesturi × mii de echipe
            return;
        }

        List<InjuryDto> injuries = provider.injuries(leagueId, season);
        if (!injuries.isEmpty()) {
            publisher.publish(GolstatConstants.KafkaTopics.INJURIES, leagueId + ":" + season, injuries);
        }

        // Imbogatirea echipelor (jucatori + statistici de sezon) LA FINAL — cea mai scumpa la cota;
        // daca se atinge limita aici, meciurile/orarul live sunt deja colectate mai sus.
        enrichTeams(teams, leagueId, season);
    }

    /**
     * Imbogatire per echipa (pagina echipei): statistici de sezon, lot de jucatori (profil + stats) si
     * antrenorul curent. Fara scheduler nou — TTL-urile Redis fac ciclul auto-limitant. La cota epuizata,
     * ne oprim din imbogatit in ciclul curent (restul se reia cand cota se reseteaza).
     */
    private void enrichTeams(List<TeamDto> teams, long leagueId, int season) {
        for (TeamDto team : teams) {
            if (team.id() == null) {
                continue;
            }
            try {
                for (TeamSeasonStatsDto stats : provider.teamStatistics(leagueId, season, team.id())) {
                    publisher.publish(GolstatConstants.KafkaTopics.TEAM_SEASON_STATS,
                            stats.teamId() + ":" + stats.leagueId() + ":" + stats.seasonYear(), stats);
                }
                List<PlayerSezonDto> jucatori = provider.players(team.id(), season);
                List<PlayerDto> profile = jucatori.stream()
                        .map(PlayerSezonDto::profil)
                        .filter(p -> p != null && p.id() != null)
                        .toList();
                if (!profile.isEmpty()) {
                    publisher.publish(GolstatConstants.KafkaTopics.PLAYERS, "team:" + team.id(), profile);
                }
                List<PlayerSeasonStatsDto> statistici = jucatori.stream()
                        .flatMap(p -> p.statistici().stream())
                        .toList();
                if (!statistici.isEmpty()) {
                    publisher.publish(GolstatConstants.KafkaTopics.PLAYER_SEASON_STATS,
                            "team:" + team.id() + ":" + season, statistici);
                }
                for (CoachDto coach : provider.coaches(team.id())) {
                    publisher.publish(GolstatConstants.KafkaTopics.COACHES, String.valueOf(coach.id()), coach);
                }
            } catch (ApiFootballQuotaExceededException e) {
                log.warn("Imbogatire echipe oprita (cota atinsa) la echipa {}: {}", team.id(), e.getMessage());
                return;
            } catch (RuntimeException e) {
                // eroare izolata pe o echipa (raspuns API invalid, limita pe minut, etc.) → o sarim,
                // se reia la ciclul urmator. NU oprim intreg ciclul pentru o singura echipa.
                log.warn("Imbogatire esuata pentru echipa {}: {}", team.id(), e.toString());
            }
        }
    }

    /**
     * Verdict initial pornind de la acoperirea declarata: "da"/necunoscut → cerem direct;
     * "nu" → {@code null} (nedecis), adica sondam un meci inainte sa renuntam pe toata liga.
     */
    private static Boolean dupaAcoperire(Boolean declarat) {
        return Boolean.FALSE.equals(declarat) ? null : Boolean.TRUE;
    }

    private static String standingKey(StandingDto standing) {
        return standing.leagueId() + ":" + standing.seasonYear() + ":" + standing.teamId();
    }

    private static String seasonKey(SeasonDto season) {
        return season.leagueId() + ":" + season.year();
    }
}
