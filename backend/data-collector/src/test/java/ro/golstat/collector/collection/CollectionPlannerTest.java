package ro.golstat.collector.collection;

import org.junit.jupiter.api.Test;
import ro.golstat.collector.provider.apifootball.ApiFootballQuotaExceededException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CollectionPlannerTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-03T10:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 3);

    /** Dublu care retine apelurile (liga + fereastra) si poate arunca cota la o liga anume. */
    private static final class RecordingCollectionService extends CollectionService {
        record Call(long leagueId, int season, LocalDate from, LocalDate to) {
        }

        final List<Call> calls = new ArrayList<>();
        Long quotaOnLeague;

        RecordingCollectionService() {
            super(null, null, null);
        }

        @Override
        public void collectGoalsData(long leagueId, int season, LocalDate from, LocalDate to) {
            if (quotaOnLeague != null && quotaOnLeague == leagueId) {
                throw new ApiFootballQuotaExceededException("/fixtures");
            }
            calls.add(new Call(leagueId, season, from, to));
        }
    }

    private static CollectionProperties props(LeagueTarget... leagues) {
        return new CollectionProperties(List.of(leagues), 90, 10);
    }

    private CollectionPlanner planner(RecordingCollectionService svc, LeagueTarget... leagues) {
        return new CollectionPlanner(svc, props(leagues), CLOCK);
    }

    private static List<Long> leagueIds(RecordingCollectionService svc) {
        return svc.calls.stream().map(RecordingCollectionService.Call::leagueId).toList();
    }

    @Test
    void iteratesEveryLeague_withRollingWindowFromClock() {
        RecordingCollectionService svc = new RecordingCollectionService();
        planner(svc, new LeagueTarget(1, 2026), new LeagueTarget(39, 2025)).collect();

        assertEquals(List.of(1L, 39L), leagueIds(svc));
        RecordingCollectionService.Call first = svc.calls.get(0);
        assertEquals(TODAY.minusDays(90), first.from());
        assertEquals(TODAY.plusDays(10), first.to());
    }

    @Test
    void quotaOnFirstLeague_stopsWholeCycle_withoutThrowing() {
        RecordingCollectionService svc = new RecordingCollectionService();
        svc.quotaOnLeague = 1L;
        planner(svc, new LeagueTarget(1, 2026), new LeagueTarget(39, 2025)).collect();
        assertTrue(svc.calls.isEmpty(), "cota la prima liga → a doua nu se mai apeleaza");
    }

    @Test
    void quotaOnSecondLeague_keepsFirst() {
        RecordingCollectionService svc = new RecordingCollectionService();
        svc.quotaOnLeague = 39L;
        planner(svc, new LeagueTarget(1, 2026), new LeagueTarget(39, 2025)).collect();
        assertEquals(List.of(1L), leagueIds(svc));
    }

    @Test
    void emptyLeagueList_doesNothing() {
        RecordingCollectionService svc = new RecordingCollectionService();
        planner(svc).collect();
        assertTrue(svc.calls.isEmpty());
    }
}
