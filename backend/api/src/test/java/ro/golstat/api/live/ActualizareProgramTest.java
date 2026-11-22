package ro.golstat.api.live;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ro.golstat.api.entity.FixtureTeamStats;
import ro.golstat.api.ingest.IngestService;
import ro.golstat.api.repository.FixtureTeamStatsRepository;
import ro.golstat.collector.provider.DataProvider;
import ro.golstat.collector.provider.bzzoiro.BzzoiroProperties;
import ro.golstat.common.GolstatConstants;
import ro.golstat.common.dto.FixtureDto;
import ro.golstat.common.dto.SeasonDto;
import ro.golstat.common.dto.StandingDto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Actualizarea zilnica: program + clasamentul oficial al fiecarei ligi + completarea meciurilor ratate. */
class ActualizareProgramTest {

    private static final long LIGA = 39L;
    private static final int SEZON = 2026;

    private final DataProvider provider = mock(DataProvider.class);
    private final IngestService ingest = mock(IngestService.class);
    private final FixtureTeamStatsRepository teamStats = mock(FixtureTeamStatsRepository.class);
    private final FinalizareMeci finalizare = mock(FinalizareMeci.class);
    private final StandingDto rand = mock(StandingDto.class);

    private ActualizareProgram actualizare;

    @BeforeEach
    void setUp() {
        actualizare = new ActualizareProgram(provider, ingest, teamStats, finalizare,
                new BzzoiroProperties(null, "cheie", Map.of(LIGA, 17L)));
        when(provider.seasons(LIGA)).thenReturn(List.of(
                new SeasonDto(LIGA, SEZON, null, null, true, null, null, null, null, null)));
        when(provider.fixtures(eq(LIGA), eq(SEZON), any(), any())).thenReturn(List.of(terminat(500)));
        when(provider.standings(LIGA, SEZON)).thenReturn(List.of(rand));
        when(teamStats.findByFixtureIdIn(any())).thenReturn(List.of());
    }

    private static FixtureDto terminat(long id) {
        return new FixtureDto(id, null, "UTC", OffsetDateTime.parse("2026-11-15T15:00:00Z"), LIGA, SEZON, "R", null,
                "long", GolstatConstants.FixtureStatus.FINISHED, 90, 1L, 2L, 1, 0, 0, 0,
                null, null, null, null, null, null);
    }

    @Test
    void clasamentulOficialEsteCerutSiIngeratPeSezonulCurent() {
        actualizare.actualizeaza();

        verify(provider).standings(LIGA, SEZON);
        verify(ingest).ingestStanding(rand);
    }

    @Test
    void clasamentEsuat_meciurileTerminateSeCompleteazaTotusi() {
        when(provider.standings(anyLong(), anyInt())).thenThrow(new IllegalStateException("standings down"));

        actualizare.actualizeaza();

        verify(finalizare).finalizeaza(terminat(500));
    }

    @Test
    void meciCuStatisticiDeja_nuSeReFinalizeaza() {
        FixtureTeamStats are = new FixtureTeamStats();
        are.setFixtureId(500L);
        when(teamStats.findByFixtureIdIn(any())).thenReturn(List.of(are));

        actualizare.actualizeaza();

        verify(finalizare, never()).finalizeaza(any());
        verify(ingest).ingestStanding(rand);
    }

    @Test
    void faraSezon_nimicCerut() {
        when(provider.seasons(LIGA)).thenReturn(List.of());

        actualizare.actualizeaza();

        verify(provider, never()).standings(anyLong(), anyInt());
        verify(provider, never()).fixtures(anyLong(), anyInt(), any(), any());
    }
}
