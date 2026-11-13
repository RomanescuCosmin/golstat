package ro.golstat.api.live;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ro.golstat.api.entity.FixtureTeamStats;
import ro.golstat.api.ingest.IngestService;
import ro.golstat.api.repository.FixtureTeamStatsRepository;
import ro.golstat.collector.provider.DataProvider;
import ro.golstat.collector.provider.bzzoiro.BzzoiroProperties;
import ro.golstat.common.GolstatConstants;
import ro.golstat.common.dto.FixtureDto;
import ro.golstat.common.dto.SeasonDto;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Actualizarea programului din bzzoiro, la pornire si zilnic (implicit 06:00): pentru fiecare liga
 * urmarita reciteste meciurile din ultimele {@value #ZILE_INAPOI} zile si urmatoarele {@value #ZILE_INAINTE}
 * (ore mutate, meciuri noi, scoruri), iar meciurile terminate fara statistici — jucate cat aplicatia a fost
 * oprita — isi iau datele finale prin {@link FinalizareMeci}. Cost: ~2 requesturi per liga + ~3 per meci ratat.
 */
@Component
@Profile("bzzoiro")
@ConditionalOnProperty(name = "golstat.live.enabled", havingValue = "true")
public class ActualizareProgram {

    private static final Logger log = LoggerFactory.getLogger(ActualizareProgram.class);

    static final int ZILE_INAPOI = 3;
    static final int ZILE_INAINTE = 14;

    private final DataProvider provider;
    private final IngestService ingest;
    private final FixtureTeamStatsRepository teamStats;
    private final FinalizareMeci finalizare;
    private final BzzoiroProperties props;

    public ActualizareProgram(DataProvider provider, IngestService ingest, FixtureTeamStatsRepository teamStats,
                              FinalizareMeci finalizare, BzzoiroProperties props) {
        this.provider = provider;
        this.ingest = ingest;
        this.teamStats = teamStats;
        this.finalizare = finalizare;
        this.props = props;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void laPornire() {
        actualizeaza();
    }

    @Scheduled(cron = "${golstat.actualizare.cron:0 0 6 * * *}")
    public void actualizeaza() {
        LocalDate azi = LocalDate.now(ZoneOffset.UTC);
        int meciuri = 0;
        int completate = 0;
        for (Long liga : props.ligi().keySet()) {
            try {
                List<SeasonDto> sezoane = provider.seasons(liga);
                if (sezoane.isEmpty() || sezoane.get(0).year() == null) {
                    continue;
                }
                List<FixtureDto> lista = provider.fixtures(liga, sezoane.get(0).year(),
                        azi.minusDays(ZILE_INAPOI), azi.plusDays(ZILE_INAINTE));
                lista.forEach(ingest::ingestFixture);
                meciuri += lista.size();

                List<FixtureDto> terminate = lista.stream()
                        .filter(f -> GolstatConstants.FixtureStatus.TERMINAL.contains(f.statusShort()))
                        .toList();
                if (terminate.isEmpty()) {
                    continue;
                }
                Set<Long> auStatistici = teamStats.findByFixtureIdIn(terminate.stream().map(FixtureDto::id).toList())
                        .stream().map(FixtureTeamStats::getFixtureId).collect(Collectors.toSet());
                for (FixtureDto f : terminate) {
                    if (!auStatistici.contains(f.id())) {
                        finalizare.finalizeaza(f);
                        completate++;
                    }
                }
            } catch (RuntimeException e) {
                log.warn("Actualizare program liga {} esuata: {}", liga, e.toString());
            }
        }
        log.info("Actualizare program: {} meciuri reimprospatate, {} meciuri terminate completate", meciuri, completate);
    }
}
