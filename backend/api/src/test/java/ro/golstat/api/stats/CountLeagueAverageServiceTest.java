package ro.golstat.api.stats;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ro.golstat.api.repository.FixtureTeamStatsRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CountLeagueAverageServiceTest {

    private static final double EPS = 1e-9;

    @Mock FixtureTeamStatsRepository teamStats;
    @InjectMocks CountLeagueAverageService service;

    private static CountAverage avg(Double cornere, Double faulturi, Double cartonase) {
        return new CountAverage() {
            @Override
            public Double getAvgCornere() {
                return cornere;
            }

            @Override
            public Double getAvgFaulturi() {
                return faulturi;
            }

            @Override
            public Double getAvgCartonase() {
                return cartonase;
            }
        };
    }

    @Test
    void dubleazaMediaPeEchipa_inTotalPeMeci() {
        when(teamStats.avgCounts(anyLong(), anyInt(), any())).thenReturn(avg(5.1, 12.3, 2.2));

        CountLeagueAverages a = service.averages(39L, 2025);

        assertEquals(10.2, a.cornerePeMeci(), EPS);
        assertEquals(24.6, a.faulturiPeMeci(), EPS);
        assertEquals(4.4, a.cartonasePeMeci(), EPS);
    }

    @Test
    void faraStatistici_cadePeFallbackGlobal() {
        when(teamStats.avgCounts(anyLong(), anyInt(), any())).thenReturn(avg(null, null, null));

        CountLeagueAverages a = service.averages(1L, 2026);

        assertEquals(CountLeagueAverageService.DEFAULT_CORNERE, a.cornerePeMeci(), EPS);
        assertEquals(CountLeagueAverageService.DEFAULT_FAULTURI, a.faulturiPeMeci(), EPS);
        assertEquals(CountLeagueAverageService.DEFAULT_CARTONASE, a.cartonasePeMeci(), EPS);
    }

    @Test
    void fallbackPartial() {
        when(teamStats.avgCounts(anyLong(), anyInt(), any())).thenReturn(avg(4.8, null, null));

        CountLeagueAverages a = service.averages(1L, 2026);

        assertEquals(9.6, a.cornerePeMeci(), EPS);
        assertEquals(CountLeagueAverageService.DEFAULT_FAULTURI, a.faulturiPeMeci(), EPS);
    }

    @Test
    void ligaSauSezonNecunoscut_fallbackFaraQuery() {
        CountLeagueAverages a = service.averages(null, null);

        assertEquals(CountLeagueAverageService.DEFAULT_CORNERE, a.cornerePeMeci(), EPS);
        verifyNoInteractions(teamStats);
    }
}
