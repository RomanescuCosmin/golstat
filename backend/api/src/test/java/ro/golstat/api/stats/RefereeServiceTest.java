package ro.golstat.api.stats;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ro.golstat.api.repository.FixtureTeamStatsRepository;
import ro.golstat.stats.cards.RefereeFactor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefereeServiceTest {

    private static final double EPS = 1e-9;

    @Mock FixtureTeamStatsRepository teamStats;
    @InjectMocks RefereeService service;

    private static RefereeCardAverage agg(Double avgCards, Long matches) {
        return new RefereeCardAverage() {
            @Override
            public Double getAvgCards() {
                return avgCards;
            }

            @Override
            public Long getMatches() {
                return matches;
            }
        };
    }

    @Test
    void arbitruDur_factorPesteUnu() {
        when(teamStats.refereeCardAverage(eq("M. Oliver"), any())).thenReturn(agg(6.0, 20L));

        double factor = service.factor("M. Oliver", 4.0);

        assertTrue(factor > 1.0);
        assertEquals(RefereeFactor.of(6.0, 20, 4.0), factor, EPS);
    }

    @Test
    void arbitruBland_factorSubUnu() {
        when(teamStats.refereeCardAverage(eq("A. Taylor"), any())).thenReturn(agg(2.5, 15L));

        assertTrue(service.factor("A. Taylor", 4.0) < 1.0);
    }

    @Test
    void arbitruFaraMeciuri_neutru() {
        when(teamStats.refereeCardAverage(eq("Necunoscut"), any())).thenReturn(agg(null, 0L));

        assertEquals(RefereeFactor.NEUTRAL, service.factor("Necunoscut", 4.0), EPS);
    }

    @Test
    void arbitruNullSauGol_neutruFaraQuery() {
        assertEquals(RefereeFactor.NEUTRAL, service.factor(null, 4.0), EPS);
        assertEquals(RefereeFactor.NEUTRAL, service.factor("  ", 4.0), EPS);
        verifyNoInteractions(teamStats);
    }
}
