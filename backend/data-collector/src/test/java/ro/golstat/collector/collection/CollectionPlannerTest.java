package ro.golstat.collector.collection;

import org.junit.jupiter.api.Test;
import ro.golstat.collector.provider.apifootball.ApiFootballProperties;
import ro.golstat.collector.provider.apifootball.ApiFootballQuotaExceededException;
import ro.golstat.collector.provider.apifootball.InMemoryCounterStore;
import ro.golstat.collector.provider.apifootball.QuotaGuard;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CollectionPlannerTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-03T10:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 3);

    /** Dublu care retine apelurile (tinta + fereastra) si poate arunca cota la o liga anume. */
    private static final class RecordingCollectionService extends CollectionService {
        record Call(LeagueTarget target, LocalDate from, LocalDate to) {
            long leagueId() {
                return target.leagueId();
            }
        }

        final List<Call> calls = new ArrayList<>();
        Long quotaOnLeague;

        RecordingCollectionService() {
            super(null, null, null);
        }

        @Override
        public void collectGoalsData(LeagueTarget target, LocalDate from, LocalDate to) {
            if (quotaOnLeague != null && quotaOnLeague == target.leagueId()) {
                throw new ApiFootballQuotaExceededException("/fixtures");
            }
            calls.add(new Call(target, from, to));
        }
    }

    private static CollectionProperties props(LeagueTarget... leagues) {
        return CollectionProperties.scheduled(List.of(leagues), 90, 10);
    }

    /** Fara QuotaGuard = cota nelimitata (ca pe profilul stub); stare pe memorie de proces. */
    private CollectionPlanner planner(RecordingCollectionService svc, LeagueTarget... leagues) {
        return new CollectionPlanner(svc, props(leagues), CLOCK, new BackfillCursor(null),
                new UltimaRulare(null, CLOCK), null);
    }

    private CollectionPlanner planner(RecordingCollectionService svc, BackfillCursor cursor,
                                      QuotaGuard quota, LeagueTarget... leagues) {
        return new CollectionPlanner(svc, props(leagues), CLOCK, cursor,
                new UltimaRulare(null, CLOCK), quota);
    }

    /** Planner cu ceas propriu si stare partajata — pentru scenariile de pauza intre cicluri. */
    private static CollectionPlanner plannerLa(RecordingCollectionService svc, Clock ceas,
                                               UltimaRulare stare, int zileInUrma, LeagueTarget... leagues) {
        CollectionProperties p = CollectionProperties.scheduled(List.of(leagues), zileInUrma, 10);
        return new CollectionPlanner(svc, p, ceas, new BackfillCursor(null), stare, null);
    }

    private static QuotaGuard quota(int limita, int rezerva, int consumatDeja) {
        InMemoryCounterStore store = new InMemoryCounterStore();
        ApiFootballProperties p = new ApiFootballProperties(
                "http://x", "k", limita, rezerva, null, null, null, null, null, null, null);
        QuotaGuard guard = new QuotaGuard(store, p, CLOCK);
        for (int i = 0; i < consumatDeja; i++) {
            guard.tryAcquire();
        }
        return guard;
    }

    private static LeagueTarget backfill(long leagueId, int season) {
        return new LeagueTarget(leagueId, season, false, true, false, false,
                LocalDate.of(2025, 7, 1), LocalDate.of(2026, 6, 30));
    }

    private static List<Long> leagueIds(RecordingCollectionService svc) {
        return svc.calls.stream().map(RecordingCollectionService.Call::leagueId).toList();
    }

    @Test
    void iteratesEveryLeague_withRollingWindowFromClock() {
        RecordingCollectionService svc = new RecordingCollectionService();
        planner(svc, LeagueTarget.zilnica(1, 2026, false, true, false, false), LeagueTarget.zilnica(39, 2025, false, true, false, false)).collect();

        assertEquals(List.of(1L, 39L), leagueIds(svc));
        RecordingCollectionService.Call first = svc.calls.get(0);
        assertEquals(TODAY.minusDays(90), first.from());
        assertEquals(TODAY.plusDays(10), first.to());
    }

    @Test
    void quotaOnFirstLeague_stopsWholeCycle_withoutThrowing() {
        RecordingCollectionService svc = new RecordingCollectionService();
        svc.quotaOnLeague = 1L;
        planner(svc, LeagueTarget.zilnica(1, 2026, false, true, false, false), LeagueTarget.zilnica(39, 2025, false, true, false, false)).collect();
        assertTrue(svc.calls.isEmpty(), "cota la prima liga → a doua nu se mai apeleaza");
    }

    @Test
    void quotaOnSecondLeague_keepsFirst() {
        RecordingCollectionService svc = new RecordingCollectionService();
        svc.quotaOnLeague = 39L;
        planner(svc, LeagueTarget.zilnica(1, 2026, false, true, false, false), LeagueTarget.zilnica(39, 2025, false, true, false, false)).collect();
        assertEquals(List.of(1L), leagueIds(svc));
    }

    @Test
    void passesTargetFlagsToService() {
        RecordingCollectionService svc = new RecordingCollectionService();
        planner(svc, LeagueTarget.zilnica(667, 2026, true, false, false, false),
                LeagueTarget.zilnica(1, 2026, false, true, true, false)).collect();

        LeagueTarget amicale = svc.calls.get(0).target();
        assertTrue(amicale.doarFixtures());
        assertEquals(false, amicale.imbogatireEchipe());
        LeagueTarget mondial = svc.calls.get(1).target();
        assertTrue(mondial.statisticiJucatori());
        assertEquals(true, mondial.imbogatireEchipe());
    }

    @Test
    void emptyLeagueList_doesNothing() {
        RecordingCollectionService svc = new RecordingCollectionService();
        planner(svc).collect();
        assertTrue(svc.calls.isEmpty());
    }

    // --- backfill ---

    @Test
    void backfillTarget_usesItsOwnAbsoluteWindow_notTheRollingOne() {
        RecordingCollectionService svc = new RecordingCollectionService();
        planner(svc, backfill(39, 2025)).ruleazaUnCiclu();

        RecordingCollectionService.Call call = svc.calls.get(0);
        assertEquals(LocalDate.of(2025, 7, 1), call.from());
        assertEquals(LocalDate.of(2026, 6, 30), call.to());
    }

    @Test
    void dailyTargetsRunBeforeBackfill_regardlessOfConfigOrder() {
        RecordingCollectionService svc = new RecordingCollectionService();
        planner(svc, backfill(39, 2025), LeagueTarget.zilnica(1, 2026, false, true, false, false)).ruleazaUnCiclu();

        assertEquals(List.of(1L, 39L), leagueIds(svc), "zilnicul are prioritate, desi e al doilea in config");
    }

    @Test
    void backfillSkipped_whenQuotaBelowReserve_butDailyStillRuns() {
        RecordingCollectionService svc = new RecordingCollectionService();
        // limita 100, rezerva 40, consumat 70 → ramase 30 <= 40 → backfill blocat
        planner(svc, new BackfillCursor(null), quota(100, 40, 70),
                LeagueTarget.zilnica(1, 2026, false, true, false, false), backfill(39, 2025)).ruleazaUnCiclu();

        assertEquals(List.of(1L), leagueIds(svc));
    }

    @Test
    void backfillRuns_whenQuotaAboveReserve() {
        RecordingCollectionService svc = new RecordingCollectionService();
        planner(svc, new BackfillCursor(null), quota(100, 40, 10), backfill(39, 2025)).ruleazaUnCiclu();

        assertEquals(List.of(39L), leagueIds(svc));
    }

    @Test
    void finishedBackfillTarget_isSkippedOnNextCycle() {
        RecordingCollectionService svc = new RecordingCollectionService();
        BackfillCursor cursor = new BackfillCursor(null);
        LeagueTarget target = backfill(39, 2025);

        planner(svc, cursor, null, target).ruleazaUnCiclu();
        planner(svc, cursor, null, target).ruleazaUnCiclu();

        assertEquals(List.of(39L), leagueIds(svc), "a doua rulare nu re-colecteaza tinta terminata");
    }

    @Test
    void interruptedBackfillTarget_isRetriedOnNextCycle() {
        RecordingCollectionService svc = new RecordingCollectionService();
        BackfillCursor cursor = new BackfillCursor(null);
        LeagueTarget target = backfill(39, 2025);

        svc.quotaOnLeague = 39L;
        planner(svc, cursor, null, target).ruleazaUnCiclu();
        assertTrue(svc.calls.isEmpty());

        svc.quotaOnLeague = null;
        planner(svc, cursor, null, target).ruleazaUnCiclu();
        assertEquals(List.of(39L), leagueIds(svc), "tinta intrerupta nu s-a marcat gata → se reia");
    }

    @Test
    void backfillContinuesWithNextTarget_afterTheFirstIsDone() {
        RecordingCollectionService svc = new RecordingCollectionService();
        BackfillCursor cursor = new BackfillCursor(null);
        LeagueTarget prima = backfill(39, 2025);
        LeagueTarget aDoua = backfill(140, 2025);

        planner(svc, cursor, null, prima, aDoua).ruleazaUnCiclu();
        assertEquals(List.of(39L, 140L), leagueIds(svc));

        svc.calls.clear();
        planner(svc, cursor, null, prima, aDoua).ruleazaUnCiclu();
        assertTrue(svc.calls.isEmpty(), "ambele terminate → ciclul urmator nu mai are ce colecta");
    }

    @Test
    void oneShotMode_scheduledTriggerIsInert() {
        RecordingCollectionService svc = new RecordingCollectionService();
        CollectionProperties p = new CollectionProperties(
                List.of(LeagueTarget.zilnica(1, 2026, false, true, false, false)), 90, 10,
                CollectionProperties.MOD_ONE_SHOT);

        new CollectionPlanner(svc, p, CLOCK, new BackfillCursor(null), new UltimaRulare(null, CLOCK), null)
                .collect();

        assertTrue(svc.calls.isEmpty(), "in one-shot ciclul e pornit de OneShotRunner, nu de @Scheduled");
    }

    // --- recuperare dupa pauza (fereastra se largeste singura) ---

    /** Ceas care poate fi avansat, ca sa simulam zile intre doua cicluri. */
    private static final class CeasMutabil extends Clock {
        private Instant acum;

        CeasMutabil(Instant start) {
            this.acum = start;
        }

        void avanseazaZile(long zile) {
            acum = acum.plus(Duration.ofDays(zile));
        }

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return acum; }
    }

    private static final LeagueTarget LIGA_ZILNICA = LeagueTarget.zilnica(1, 2026, false, true, false, false);

    @Test
    void firstRun_usesConfiguredWindow() {
        RecordingCollectionService svc = new RecordingCollectionService();
        CeasMutabil ceas = new CeasMutabil(Instant.parse("2026-07-03T10:00:00Z"));

        plannerLa(svc, ceas, new UltimaRulare(null, ceas), 1, LIGA_ZILNICA).ruleazaUnCiclu();

        assertEquals(TODAY.minusDays(1), svc.calls.get(0).from());
    }

    @Test
    void afterPause_windowWidensToCoverTheGap_thenNarrowsBack() {
        RecordingCollectionService svc = new RecordingCollectionService();
        CeasMutabil ceas = new CeasMutabil(Instant.parse("2026-07-03T10:00:00Z"));
        UltimaRulare stare = new UltimaRulare(null, ceas);

        plannerLa(svc, ceas, stare, 1, LIGA_ZILNICA).ruleazaUnCiclu();

        ceas.avanseazaZile(10);   // laptopul sta inchis 10 zile
        svc.calls.clear();
        plannerLa(svc, ceas, stare, 1, LIGA_ZILNICA).ruleazaUnCiclu();

        LocalDate dupaPauza = LocalDate.of(2026, 7, 13);
        assertEquals(dupaPauza.minusDays(11), svc.calls.get(0).from(),
                "fereastra acopera pauza de 10 zile + 1 zi marja");

        svc.calls.clear();        // ciclul urmator, imediat: inapoi la fereastra ieftina
        plannerLa(svc, ceas, stare, 1, LIGA_ZILNICA).ruleazaUnCiclu();
        assertEquals(dupaPauza.minusDays(1), svc.calls.get(0).from(), "fara pauza → inapoi la 1 zi");
    }

    @Test
    void veryLongPause_isCappedSoItCannotBurnTheWholeQuota() {
        RecordingCollectionService svc = new RecordingCollectionService();
        CeasMutabil ceas = new CeasMutabil(Instant.parse("2026-07-03T10:00:00Z"));
        UltimaRulare stare = new UltimaRulare(null, ceas);

        plannerLa(svc, ceas, stare, 1, LIGA_ZILNICA).ruleazaUnCiclu();
        ceas.avanseazaZile(300);
        svc.calls.clear();
        plannerLa(svc, ceas, stare, 1, LIGA_ZILNICA).ruleazaUnCiclu();

        LocalDate azi = LocalDate.of(2027, 4, 29);
        assertEquals(azi.minusDays(UltimaRulare.ZILE_MAX_RECUPERARE), svc.calls.get(0).from());
    }

    @Test
    void abortedCycle_isNotMarkedAsRun_soTheGapStaysCovered() {
        RecordingCollectionService svc = new RecordingCollectionService();
        CeasMutabil ceas = new CeasMutabil(Instant.parse("2026-07-03T10:00:00Z"));
        UltimaRulare stare = new UltimaRulare(null, ceas);

        plannerLa(svc, ceas, stare, 1, LIGA_ZILNICA).ruleazaUnCiclu();   // reuseste, marcheaza

        ceas.avanseazaZile(6);
        svc.quotaOnLeague = 1L;                                          // ciclul cade pe cota
        plannerLa(svc, ceas, stare, 1, LIGA_ZILNICA).ruleazaUnCiclu();

        ceas.avanseazaZile(1);
        svc.quotaOnLeague = null;
        svc.calls.clear();
        plannerLa(svc, ceas, stare, 1, LIGA_ZILNICA).ruleazaUnCiclu();

        // marcajul a ramas la primul ciclu reusit → recuperarea acopera toate cele 7 zile
        assertEquals(LocalDate.of(2026, 7, 10).minusDays(8), svc.calls.get(0).from(),
                "un ciclu cazut nu are voie sa pretinda ca a acoperit fereastra");
    }
}
