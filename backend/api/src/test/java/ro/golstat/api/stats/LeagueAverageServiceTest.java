package ro.golstat.api.stats;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ro.golstat.api.repository.FixtureRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeagueAverageServiceTest {

    private static final double EPS = 1e-9;

    @Mock FixtureRepository fixtures;
    @InjectMocks LeagueAverageService service;

    private static GoalAverage avg(Double gazde, Double oaspeti) {
        return new GoalAverage() {
            @Override
            public Double getAvgGazde() {
                return gazde;
            }

            @Override
            public Double getAvgOaspeti() {
                return oaspeti;
            }
        };
    }

    @Test
    void usesRepositoryAveragesWhenPresent() {
        when(fixtures.avgGoals(anyLong(), anyInt(), any())).thenReturn(avg(1.7, 1.2));
        LeagueAverages a = service.averages(39, 2025);
        assertEquals(1.7, a.mediaLigaGazde(), EPS);
        assertEquals(1.2, a.mediaLigaOaspeti(), EPS);
    }

    @Test
    void fallsBackWhenNoTerminalMatches() {
        // avg peste zero randuri → getter-e null → fallback global (ex. CM la primul meci)
        when(fixtures.avgGoals(anyLong(), anyInt(), any())).thenReturn(avg(null, null));
        LeagueAverages a = service.averages(1, 2026);
        assertEquals(LeagueAverageService.DEFAULT_GAZDE, a.mediaLigaGazde(), EPS);
        assertEquals(LeagueAverageService.DEFAULT_OASPETI, a.mediaLigaOaspeti(), EPS);
    }

    @Test
    void fallsBackOnPartialNull() {
        when(fixtures.avgGoals(anyLong(), anyInt(), any())).thenReturn(avg(1.6, null));
        LeagueAverages a = service.averages(1, 2026);
        assertEquals(1.6, a.mediaLigaGazde(), EPS);
        assertEquals(LeagueAverageService.DEFAULT_OASPETI, a.mediaLigaOaspeti(), EPS);
    }
}
