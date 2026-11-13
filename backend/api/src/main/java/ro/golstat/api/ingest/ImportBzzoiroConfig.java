package ro.golstat.api.ingest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import ro.golstat.api.repository.FixtureTeamHalfStatsRepository;
import ro.golstat.api.repository.FixtureTeamStatsRepository;
import ro.golstat.collector.collection.CollectionService;
import ro.golstat.collector.collection.LeagueTarget;
import ro.golstat.collector.live.LiveSchedule;
import ro.golstat.collector.provider.DataProvider;
import ro.golstat.collector.provider.bzzoiro.BzzoiroClient;
import ro.golstat.collector.provider.bzzoiro.BzzoiroProperties;
import ro.golstat.collector.publish.EventPublisher;
import ro.golstat.common.GolstatConstants;
import ro.golstat.common.dto.FixtureDto;
import ro.golstat.common.dto.FixtureEventDto;
import ro.golstat.common.dto.FixtureTeamHalfStatsDto;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Import one-shot din bzzoiro direct in baza (fara Kafka): la pornirea cu profilul {@code import}
 * colecteaza tintele din {@code golstat.import.tinte}, apoi aplicatia ramane pornita normal.
 *
 * <p>O tinta cu {@code doar-reprize: true} e o COMPLETARE pentru o liga deja importata: re-citeste
 * lista de meciuri (actualizeaza scoruri + arbitri) si cere statisticile doar la meciurile terminate
 * care nu au inca randuri pe reprize — 1 request per meci in loc de 3.
 *
 * <p>O tinta cu {@code doar-incidente: true} re-citeste incidentele meciurilor terminate la care
 * statisticile au mai multe cartonase decat incidentele (al doilea galben pierdut la importurile vechi).
 * Ingest-ul inlocuieste incidentele meciului si recalculeaza cartonasele, deci reluarea e sigura.
 */
@Configuration
@Profile("import")
@ComponentScan(basePackages = "ro.golstat.collector.provider.bzzoiro")
@EnableConfigurationProperties({BzzoiroProperties.class, ImportBzzoiroConfig.ImportProperties.class})
class ImportBzzoiroConfig {

    private static final Logger log = LoggerFactory.getLogger(ImportBzzoiroConfig.class);

    @ConfigurationProperties(prefix = "golstat.import")
    record ImportProperties(List<Tinta> tinte, Integer zileInainte) {
        record Tinta(long leagueId, int season, LocalDate from, boolean doarReprize, boolean doarIncidente) {
        }
    }

    @Bean
    ApplicationRunner importBzzoiro(DataProvider provider, BzzoiroClient client, DataIngestListeners listeners,
                                    ReprizeIngestListener reprizeListener,
                                    FixtureTeamHalfStatsRepository reprize, FixtureTeamStatsRepository statistici,
                                    ObjectMapper mapper, ImportProperties props) {
        return args -> {
            EventPublisher publisher = new DirectEventPublisher(listeners, reprizeListener, mapper);
            CollectionService colectare = new CollectionService(provider, publisher, new LiveSchedule());
            LocalDate to = LocalDate.now().plusDays(props.zileInainte() == null ? 14 : props.zileInainte());
            for (ImportProperties.Tinta t : props.tinte() == null ? List.<ImportProperties.Tinta>of() : props.tinte()) {
                int inainte = client.requesturiFacute();
                try {
                    if (t.doarIncidente()) {
                        reimportaIncidente(provider, publisher, statistici, t, to);
                    } else if (t.doarReprize()) {
                        completeazaReprize(provider, publisher, reprize, t, to);
                    } else {
                        colectare.collectGoalsData(new LeagueTarget(t.leagueId(), t.season(), false, false,
                                false, false, t.from(), to), t.from(), to);
                    }
                    log.info("Import bzzoiro liga {} sezon {}{}: gata ({} requesturi)", t.leagueId(), t.season(),
                            t.doarIncidente() ? " (incidente)" : t.doarReprize() ? " (reprize)" : "", client.requesturiFacute() - inainte);
                } catch (RuntimeException e) {
                    log.error("Import bzzoiro liga {} sezon {} esuat: {}", t.leagueId(), t.season(), e.toString());
                }
            }
            log.info("Import bzzoiro terminat: {} requesturi in total", client.requesturiFacute());
        };
    }

    private static void reimportaIncidente(DataProvider provider, EventPublisher publisher,
                                           FixtureTeamStatsRepository statistici,
                                           ImportProperties.Tinta t, LocalDate to) {
        List<FixtureDto> meciuri = provider.fixtures(t.leagueId(), t.season(), t.from(), to);
        for (FixtureDto f : meciuri) {
            publisher.publish(GolstatConstants.KafkaTopics.FIXTURES, String.valueOf(f.id()), f);
        }
        List<Long> terminate = meciuri.stream()
                .filter(f -> GolstatConstants.FixtureStatus.TERMINAL.contains(f.statusShort()))
                .map(FixtureDto::id)
                .toList();
        Set<Long> suspecte = terminate.isEmpty() ? Set.of() : statistici.meciuriCuCartonaseDiferite(terminate);
        for (Long id : suspecte) {
            List<FixtureEventDto> incidente = provider.fixtureEvents(id);
            if (!incidente.isEmpty()) {
                publisher.publish(GolstatConstants.KafkaTopics.FIXTURE_EVENTS, String.valueOf(id), incidente);
            }
        }
    }

    private static void completeazaReprize(DataProvider provider, EventPublisher publisher,
                                           FixtureTeamHalfStatsRepository reprize,
                                           ImportProperties.Tinta t, LocalDate to) {
        List<FixtureDto> meciuri = provider.fixtures(t.leagueId(), t.season(), t.from(), to);
        for (FixtureDto f : meciuri) {
            publisher.publish(GolstatConstants.KafkaTopics.FIXTURES, String.valueOf(f.id()), f);
        }
        List<Long> terminate = meciuri.stream()
                .filter(f -> GolstatConstants.FixtureStatus.TERMINAL.contains(f.statusShort()))
                .map(FixtureDto::id)
                .toList();
        Set<Long> auDeja = terminate.isEmpty() ? Set.of() : reprize.meciuriCuReprize(terminate);
        for (Long id : terminate) {
            if (auDeja.contains(id)) {
                continue;
            }
            List<FixtureTeamHalfStatsDto> randuri = provider.fixtureHalfStatistics(id);
            if (!randuri.isEmpty()) {
                publisher.publish(GolstatConstants.KafkaTopics.FIXTURE_TEAM_HALF_STATS, String.valueOf(id), randuri);
            }
        }
    }
}
