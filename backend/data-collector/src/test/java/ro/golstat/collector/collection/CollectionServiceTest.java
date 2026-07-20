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
import ro.golstat.common.dto.FixtureLiveDto;
import ro.golstat.common.dto.FixturePlayerStatsDto;
import ro.golstat.common.dto.FixtureTeamStatsDto;
import ro.golstat.common.dto.InjuryDto;
import ro.golstat.common.dto.CoachDto;
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
    void lineupsRequestedOnlyForTerminal_publishedAsBatchPerFixture() {
        StatusProvider provider = new StatusProvider();
        RecordingPublisher pub = new RecordingPublisher();
        new CollectionService(provider, pub, new LiveSchedule())
                .collectGoalsData(1, 2026, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 31));

        // pre-meci (NS) formatiile le aduce LineupPrematchPoller cu TTL 0; aici doar terminate,
        // ca raspunsul gol de dinaintea anuntului sa nu ramana in cache-ul istoric
        assertEquals(List.of(200L), provider.lineupsAskedFor);
        assertEquals(1, pub.countOn(GolstatConstants.KafkaTopics.FIXTURE_LINEUPS));
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

    /** Fereastra folosita de testele pe StatusProvider (meciul FT e pe 15 iunie 2026). */
    private static RecordingPublisher collect(DataProvider provider, LeagueTarget target) {
        RecordingPublisher pub = new RecordingPublisher();
        new CollectionService(provider, pub, new LiveSchedule())
                .collectGoalsData(target, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 31));
        return pub;
    }

    private static SeasonDto acoperire2026(boolean statisticiMeci, boolean statisticiJucatori) {
        return new SeasonDto(1L, 2026, null, null, true, true, true,
                statisticiMeci, statisticiJucatori, true);
    }

    @Test
    void playerStatsRequestedOnlyWhenFlagOn_publishedAsBatchPerFixture() {
        StatusProvider provider = new StatusProvider();
        RecordingPublisher pub = collect(provider, LeagueTarget.zilnica(1, 2026, false, true, true, false));

        assertEquals(List.of(200L), provider.playerStatsAskedFor, "NS nu declanseaza cerere");
        assertEquals(1, pub.countOn(GolstatConstants.KafkaTopics.FIXTURE_PLAYER_STATS));
        RecordingPublisher.Message msg = pub.first(GolstatConstants.KafkaTopics.FIXTURE_PLAYER_STATS);
        assertEquals("200", msg.key());
        List<?> batch = assertInstanceOf(List.class, msg.payload());   // lot: toti jucatorii intr-un mesaj
        assertEquals(2, batch.size());
    }

    @Test
    void playerStats_notRequestedWhenFlagOff() {
        StatusProvider provider = new StatusProvider();
        RecordingPublisher pub = collect(provider, LeagueTarget.zilnica(1, 2026, false, true, false, false));

        assertTrue(provider.playerStatsAskedFor.isEmpty(), "fara flag nu ardem 1 request/meci");
        assertEquals(0, pub.countOn(GolstatConstants.KafkaTopics.FIXTURE_PLAYER_STATS));
    }

    @Test
    void emptyPlayerStats_notPublished() {
        StatusProvider provider = new StatusProvider();
        provider.playerStatsAvailable = false;
        RecordingPublisher pub = collect(provider, LeagueTarget.zilnica(1, 2026, false, true, true, false));

        assertEquals(0, pub.countOn(GolstatConstants.KafkaTopics.FIXTURE_PLAYER_STATS));
    }

    @Test
    void coverageFalse_darFurnizorulAreDate_sondaDeblocheazaRestulLigii() {
        // Acoperirea declarata minte la sezoanele proaspat incepute (Liga I 2026 zicea false desi
        // /fixtures/statistics intorcea cornere/faulturi). Sonda pe primul meci trebuie sa o infirme.
        StatusProvider provider = new StatusProvider();
        provider.acoperire = acoperire2026(false, false);
        provider.alDoileaTerminat = true;
        RecordingPublisher pub = collect(provider, LeagueTarget.zilnica(1, 2026, false, true, true, false));

        assertEquals(List.of(200L, 202L), provider.statsAskedFor, "sonda a reusit → cerem pentru toate");
        assertEquals(List.of(200L, 202L), provider.playerStatsAskedFor);
        assertEquals(2, pub.countOn(GolstatConstants.KafkaTopics.FIXTURE_TEAM_STATS));
    }

    @Test
    void coverageFalse_siChiarNuExistaDate_sondaOpresteRestulLigii() {
        // Cazul in care acoperirea are dreptate (ex. amicale): platim UN request de sonda, nu unul per meci.
        StatusProvider provider = new StatusProvider();
        provider.acoperire = acoperire2026(false, false);
        provider.alDoileaTerminat = true;
        provider.statsAvailable = false;
        provider.playerStatsAvailable = false;
        RecordingPublisher pub = collect(provider, LeagueTarget.zilnica(1, 2026, false, true, true, false));

        assertEquals(List.of(200L), provider.statsAskedFor, "sonda a esuat → nu mai insistam pe restul ligii");
        assertEquals(List.of(200L), provider.playerStatsAskedFor);
        assertEquals(0, pub.countOn(GolstatConstants.KafkaTopics.FIXTURE_TEAM_STATS));
        assertEquals(List.of(200L, 202L), provider.eventsAskedFor, "evenimentele raman cerute pentru toate");
    }

    @Test
    void coverageTrue_requestsStatistics() {
        StatusProvider provider = new StatusProvider();
        provider.acoperire = acoperire2026(true, true);
        collect(provider, LeagueTarget.zilnica(1, 2026, false, true, true, false));

        assertEquals(List.of(200L), provider.statsAskedFor);
        assertEquals(List.of(200L), provider.playerStatsAskedFor);
    }

    @Test
    void doarStatisticiJucatori_skipsAlreadyCollectedDetails() {
        // liga deja colectata: cerem STRICT notele (1 req/meci), nu re-cerem formatii/evenimente/statistici
        StatusProvider provider = new StatusProvider();
        RecordingPublisher pub = collect(provider, LeagueTarget.zilnica(39, 2025, false, false, false, true));

        assertEquals(List.of(200L), provider.playerStatsAskedFor);
        assertTrue(provider.eventsAskedFor.isEmpty(), "evenimentele sunt deja in baza");
        assertTrue(provider.lineupsAskedFor.isEmpty(), "formatiile sunt deja in baza");
        assertTrue(provider.statsAskedFor.isEmpty(), "statisticile de echipa sunt deja in baza");
        assertEquals(1, pub.countOn(GolstatConstants.KafkaTopics.FIXTURE_PLAYER_STATS));
        assertEquals(0, pub.countOn(GolstatConstants.KafkaTopics.FIXTURE_EVENTS));
    }

    @Test
    void imbogatireOff_skipsInjuriesAndTeamEnrichment() {
        StatusProvider provider = new StatusProvider();
        RecordingPublisher pub = collect(provider, LeagueTarget.zilnica(1, 2026, false, false, false, false));

        // amicalele au mii de echipe: ~4 requesturi/echipa ar depasi cota zilnica
        assertEquals(0, pub.countOn(GolstatConstants.KafkaTopics.INJURIES));
        assertEquals(0, pub.countOn(GolstatConstants.KafkaTopics.TEAM_SEASON_STATS));
        assertEquals(0, pub.countOn(GolstatConstants.KafkaTopics.PLAYERS));
        assertEquals(1, pub.countOn(GolstatConstants.KafkaTopics.FIXTURE_EVENTS), "detaliile per meci raman");
    }

    @Test
    void enrichesTeams_publishesSeasonStatsPlayersAndCoaches() {
        RecordingPublisher pub = new RecordingPublisher();
        new CollectionService(new EnrichProvider(), pub, new LiveSchedule())
                .collectGoalsData(39, 2025, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 2));

        assertEquals(1, pub.countOn(GolstatConstants.KafkaTopics.TEAM_SEASON_STATS));
        assertEquals("50:39:2025", pub.first(GolstatConstants.KafkaTopics.TEAM_SEASON_STATS).key());

        RecordingPublisher.Message players = pub.first(GolstatConstants.KafkaTopics.PLAYERS);
        assertEquals("team:50", players.key());
        assertEquals(1, assertInstanceOf(List.class, players.payload()).size());

        RecordingPublisher.Message playerStats = pub.first(GolstatConstants.KafkaTopics.PLAYER_SEASON_STATS);
        assertEquals("team:50:2025", playerStats.key());
        assertEquals(1, assertInstanceOf(List.class, playerStats.payload()).size());

        assertEquals(1, pub.countOn(GolstatConstants.KafkaTopics.COACHES));
        assertEquals("18", pub.first(GolstatConstants.KafkaTopics.COACHES).key());
    }

    /** Furnizor minimal cu o singura echipa si datele de imbogatire (statistici sezon, jucatori, antrenor). */
    private static final class EnrichProvider implements DataProvider {
        @Override
        public List<TeamDto> teams(long leagueId, int season) {
            return List.of(new TeamDto(50L, "Man City", null, null, null, false, null, null));
        }

        @Override
        public List<TeamSeasonStatsDto> teamStatistics(long leagueId, int season, long teamId) {
            return List.of(new TeamSeasonStatsDto(teamId, leagueId, season, "WWWWW",
                    null, null, 38, null, null, 28, null, null, 5, null, null, 5,
                    null, null, 94, null, null, null, null, null, 33, null, null, null,
                    null, null, 18, null, null, null, 60, 2));
        }

        @Override
        public List<PlayerSezonDto> players(long teamId, int season) {
            PlayerDto profil = new PlayerDto(617L, "E. Haaland", null, null, null, null, null, null,
                    null, null, null, null, "h.png");
            PlayerSeasonStatsDto stats = new PlayerSeasonStatsDto(617L, teamId, 39L, season, "Attacker",
                    30, 28, 2500, java.math.BigDecimal.valueOf(7.5), false,
                    20, null, 8, null, 60, 30, 800, 40, 85,
                    null, null, null, null, null, null, null, null, null, 3, 0, 0,
                    null, null, null, null, null);
            return List.of(new PlayerSezonDto(profil, List.of(stats)));
        }

        @Override
        public List<CoachDto> coaches(long teamId) {
            return List.of(new CoachDto(18L, "P. Guardiola", null, null, null, null, null));
        }

        @Override
        public List<FixtureDto> fixtures(long leagueId, int season, LocalDate from, LocalDate to) {
            return List.of();
        }

        @Override
        public List<FixtureEventDto> fixtureEvents(long fixtureId) {
            return List.of();
        }

        @Override
        public List<FixtureTeamStatsDto> fixtureStatistics(long fixtureId) {
            return List.of();
        }

        @Override
        public List<FixtureLineupDto> fixtureLineups(long fixtureId) {
            return List.of();
        }

        @Override
        public List<InjuryDto> injuries(long leagueId, int season) {
            return List.of();
        }

        @Override
        public List<FixtureLiveDto> liveFixtures() {
            return List.of();
        }

        @Override
        public List<StandingDto> standings(long leagueId, int season) {
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
    }

    /** Furnizor cu un meci terminat (200, FT) si unul viitor (201, NS); retine pentru cine s-au cerut evenimente/statistici. */
    private static final class StatusProvider implements DataProvider {
        final List<Long> eventsAskedFor = new ArrayList<>();
        final List<Long> statsAskedFor = new ArrayList<>();
        final List<Long> lineupsAskedFor = new ArrayList<>();
        final List<Long> playerStatsAskedFor = new ArrayList<>();
        boolean statsAvailable = true;
        boolean lineupsAvailable = true;
        boolean playerStatsAvailable = true;
        /** Adauga un al doilea meci terminat (202) — necesar ca sa se vada daca sonda opreste restul ligii. */
        boolean alDoileaTerminat = false;
        /** Acoperirea sezonului 2026 raportata de furnizor; null = necunoscuta (lista de sezoane goala). */
        SeasonDto acoperire;

        @Override
        public List<FixturePlayerStatsDto> fixturePlayerStatistics(long fixtureId) {
            playerStatsAskedFor.add(fixtureId);
            if (!playerStatsAvailable) {
                return List.of();
            }
            return List.of(playerStats(fixtureId, 1L, 100L), playerStats(fixtureId, 2L, 200L));
        }

        private static FixturePlayerStatsDto playerStats(long fixtureId, long teamId, long playerId) {
            return new FixturePlayerStatsDto(fixtureId, teamId, playerId, "Jucator", 90,
                    java.math.BigDecimal.valueOf(7.2), false, false, "M",
                    null, null, null, null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null, null, null);
        }

        @Override
        public List<FixtureDto> fixtures(long leagueId, int season, LocalDate from, LocalDate to) {
            if (alDoileaTerminat) {
                return List.of(fixture(200, GolstatConstants.FixtureStatus.FINISHED),
                        fixture(201, GolstatConstants.FixtureStatus.NOT_STARTED),
                        fixture(202, GolstatConstants.FixtureStatus.FINISHED));
            }
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
        public List<FixtureLiveDto> liveFixtures() {
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
            return acoperire != null ? List.of(acoperire) : List.of();
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
