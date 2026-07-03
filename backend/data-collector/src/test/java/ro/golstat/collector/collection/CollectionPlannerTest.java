package ro.golstat.collector.collection;

import org.junit.jupiter.api.Test;
import ro.golstat.collector.provider.apifootball.ApiFootballQuotaExceededException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CollectionPlannerTest {

    private static final LocalDate FROM = LocalDate.of(2024, 8, 1);
    private static final LocalDate TO = LocalDate.of(2024, 9, 30);

    /** Dublu care retine ce ligi s-au colectat si poate arunca cota la o liga anume. */
    private static final class RecordingCollectionService extends CollectionService {
        final List<Long> collected = new ArrayList<>();
        Long quotaOnLeague;

        RecordingCollectionService() {
            super(null, null);
        }

        @Override
        public void collectGoalsData(long leagueId, int season, LocalDate from, LocalDate to) {
            if (quotaOnLeague != null && quotaOnLeague == leagueId) {
                throw new ApiFootballQuotaExceededException("/fixtures");
            }
            collected.add(leagueId);
        }
    }

    private static CollectionProperties props(LeagueTarget... leagues) {
        return new CollectionProperties(List.of(leagues), FROM, TO);
    }

    @Test
    void iteratesEveryConfiguredLeague() {
        RecordingCollectionService svc = new RecordingCollectionService();
        new CollectionPlanner(svc, props(new LeagueTarget(1, 2026), new LeagueTarget(39, 2025))).collect();
        assertEquals(List.of(1L, 39L), svc.collected);
    }

    @Test
    void quotaOnFirstLeague_stopsWholeCycle_withoutThrowing() {
        RecordingCollectionService svc = new RecordingCollectionService();
        svc.quotaOnLeague = 1L;
        new CollectionPlanner(svc, props(new LeagueTarget(1, 2026), new LeagueTarget(39, 2025))).collect();
        assertTrue(svc.collected.isEmpty(), "cota la prima liga → a doua nu se mai apeleaza");
    }

    @Test
    void quotaOnSecondLeague_keepsFirst() {
        RecordingCollectionService svc = new RecordingCollectionService();
        svc.quotaOnLeague = 39L;
        new CollectionPlanner(svc, props(new LeagueTarget(1, 2026), new LeagueTarget(39, 2025))).collect();
        assertEquals(List.of(1L), svc.collected);
    }

    @Test
    void emptyLeagueList_doesNothing() {
        RecordingCollectionService svc = new RecordingCollectionService();
        new CollectionPlanner(svc, props()).collect();
        assertTrue(svc.collected.isEmpty());
    }
}
