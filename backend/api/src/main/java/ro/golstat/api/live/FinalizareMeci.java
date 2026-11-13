package ro.golstat.api.live;

import ro.golstat.api.ingest.IngestService;
import ro.golstat.api.ingest.ReprizeIngestListener;
import ro.golstat.collector.provider.DataProvider;
import ro.golstat.common.GolstatConstants;
import ro.golstat.common.dto.FixtureDto;
import ro.golstat.common.dto.FixtureEventDto;
import ro.golstat.common.dto.FixtureLineupDto;
import ro.golstat.common.dto.FixtureTeamStatsDto;

import java.util.List;

/**
 * Datele finale ale unui meci terminat, o singura data: statisticile (cu reprizele, din acelasi raspuns),
 * incidentele (goluri, cartonase) si formatiile. Fara ele meciul ar intra in ferestrele de forma fara
 * cornere / cartonase. Folosit de bucla live (la fluier) si de actualizarea zilnica (meciuri ratate).
 * ~3 requesturi per meci. Bean declarat in {@link LiveCollectorConfig}, langa furnizor.
 */
public class FinalizareMeci {

    private final DataProvider provider;
    private final IngestService ingest;
    private final ReprizeIngestListener reprize;

    public FinalizareMeci(DataProvider provider, IngestService ingest, ReprizeIngestListener reprize) {
        this.provider = provider;
        this.ingest = ingest;
        this.reprize = reprize;
    }

    public void finalizeaza(FixtureDto f) {
        if (f == null || f.id() == null || !GolstatConstants.FixtureStatus.TERMINAL.contains(f.statusShort())) {
            return;
        }
        List<FixtureTeamStatsDto> stats = provider.fixtureStatistics(f.id());
        if (!stats.isEmpty()) {
            ingest.ingestFixtureTeamStats(stats);
        }
        reprize.salveaza(provider.fixtureHalfStatistics(f.id()));
        List<FixtureEventDto> incidente = provider.fixtureEvents(f.id());
        if (!incidente.isEmpty()) {
            ingest.ingestEvents(incidente);
        }
        List<FixtureLineupDto> formatii = provider.fixtureLineups(f.id());
        if (!formatii.isEmpty()) {
            ingest.ingestFixtureLineups(formatii);
        }
    }
}
