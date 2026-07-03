package ro.golstat.collector.collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ro.golstat.collector.live.LiveSchedule;
import ro.golstat.collector.provider.DataProvider;
import ro.golstat.collector.publish.EventPublisher;
import ro.golstat.common.GolstatConstants;
import ro.golstat.common.dto.FixtureDto;
import ro.golstat.common.dto.FixtureEventDto;
import ro.golstat.common.dto.LeagueDto;
import ro.golstat.common.dto.SeasonDto;
import ro.golstat.common.dto.StandingDto;
import ro.golstat.common.dto.TeamDto;
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
 * </ul>
 */
@Service
public class CollectionService {

    private static final Logger log = LoggerFactory.getLogger(CollectionService.class);

    /** Doar meciurile terminate au evenimente stabile; unul NS/live n-are → nu-i cerem (economie de cota). */
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
        // Catalog intai: fixtures/standings au FK spre venue/league/season/team.
        for (VenueDto venue : provider.venues()) {
            publisher.publish(GolstatConstants.KafkaTopics.VENUES, String.valueOf(venue.id()), venue);
        }
        for (LeagueDto league : provider.leagues()) {
            publisher.publish(GolstatConstants.KafkaTopics.LEAGUES, String.valueOf(league.id()), league);
        }
        for (SeasonDto season2 : provider.seasons(leagueId)) {
            publisher.publish(GolstatConstants.KafkaTopics.SEASONS, seasonKey(season2), season2);
        }
        for (TeamDto team : provider.teams(leagueId, season)) {
            publisher.publish(GolstatConstants.KafkaTopics.TEAMS, String.valueOf(team.id()), team);
        }

        for (StandingDto standing : provider.standings(leagueId, season)) {
            publisher.publish(GolstatConstants.KafkaTopics.STANDINGS, standingKey(standing), standing);
        }

        List<FixtureDto> fixtures = provider.fixtures(leagueId, season, from, to);
        for (FixtureDto fixture : fixtures) {
            publisher.publish(GolstatConstants.KafkaTopics.FIXTURES, String.valueOf(fixture.id()), fixture);

            if (!TERMINAL.contains(fixture.statusShort())) {
                continue;   // meci viitor/live → fara evenimente
            }
            List<FixtureEventDto> events = provider.fixtureEvents(fixture.id());
            if (!events.isEmpty()) {
                publisher.publish(GolstatConstants.KafkaTopics.FIXTURE_EVENTS, String.valueOf(fixture.id()), events);
            }
        }

        // orarul pentru bucla LIVE: kickoff-urile meciurilor din fereastra (gating-ul poll-ului)
        List<OffsetDateTime> kickoffs = fixtures.stream()
                .map(FixtureDto::kickoff)
                .filter(Objects::nonNull)
                .toList();
        liveSchedule.replaceForLeague(leagueId, kickoffs);

        log.info("Colectat liga {} sezon {}: {} fixtures in fereastra {}..{}",
                leagueId, season, fixtures.size(), from, to);
    }

    private static String standingKey(StandingDto standing) {
        return standing.leagueId() + ":" + standing.seasonYear() + ":" + standing.teamId();
    }

    private static String seasonKey(SeasonDto season) {
        return season.leagueId() + ":" + season.year();
    }
}
