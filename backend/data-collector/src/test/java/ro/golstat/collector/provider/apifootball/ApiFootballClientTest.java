package ro.golstat.collector.provider.apifootball;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ApiFootballClientTest {

    private static final String FIXTURES_JSON = "{\"response\":[{\"fixture\":{\"id\":215}}]}";
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-03T10:00:00Z"), ZoneOffset.UTC);

    private static ApiFootballProperties props(int limit) {
        return new ApiFootballProperties("http://api.test", "k", limit,
                Duration.ofHours(6), Duration.ofHours(1), Duration.ofHours(24),
                Duration.ofHours(20), Duration.ofDays(7), Duration.ofDays(7));
    }

    @Test
    void cacheHit_secondCall_skipsHttpAndQuota() {
        InMemoryCounterStore store = new InMemoryCounterStore();
        QuotaGuard quota = new QuotaGuard(store, props(100), CLOCK);
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(ExpectedCount.once(), requestTo("http://api.test/fixtures?league=39"))
                .andRespond(withSuccess(FIXTURES_JSON, MediaType.APPLICATION_JSON));

        ApiFootballClient client = new ApiFootballClient(props(100), builder, store, quota);
        List<FixtureItem> first = client.get("/fixtures", Map.of("league", 39), FixtureItem.class);
        List<FixtureItem> second = client.get("/fixtures", Map.of("league", 39), FixtureItem.class);

        server.verify();   // exact o cerere HTTP, desi am cerut de doua ori
        assertEquals(215L, first.get(0).fixture().id());
        assertEquals(215L, second.get(0).fixture().id());
        assertEquals(1, quota.used(), "doar HTTP-ul real consuma cota");
    }

    @Test
    void quotaExhausted_cacheMiss_throwsWithoutHttp() {
        InMemoryCounterStore store = new InMemoryCounterStore();
        QuotaGuard quota = new QuotaGuard(store, props(0), CLOCK);   // limita 0 → totul blocat
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        // niciun expect: daca clientul face HTTP, verificarea de mai jos pica

        ApiFootballClient client = new ApiFootballClient(props(0), builder, store, quota);
        assertThrows(ApiFootballQuotaExceededException.class,
                () -> client.get("/fixtures", Map.of("league", 39), FixtureItem.class));

        server.verify();   // zero cereri
    }

    @Test
    void errorEnvelope_throwsAndIsNotCached() {
        InMemoryCounterStore store = new InMemoryCounterStore();
        QuotaGuard quota = new QuotaGuard(store, props(100), CLOCK);
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        String errorJson = "{\"response\":[],\"errors\":{\"token\":\"invalid\"}}";
        server.expect(ExpectedCount.once(), requestTo("http://api.test/standings?league=39"))
                .andRespond(withSuccess(errorJson, MediaType.APPLICATION_JSON));

        ApiFootballClient client = new ApiFootballClient(props(100), builder, store, quota);
        assertThrows(ApiFootballException.class,
                () -> client.get("/standings", Map.of("league", 39), StandingsLeagueItem.class));

        boolean anythingCached = store.values.keySet().stream().anyMatch(k -> k.startsWith("golstat:af:cache:"));
        assertTrue(!anythingCached, "raspunsul de eroare nu trebuie cache-uit");
    }

    @Test
    void ttlZero_bypassesCache_hitsHttpEveryCall() {
        InMemoryCounterStore store = new InMemoryCounterStore();
        QuotaGuard quota = new QuotaGuard(store, props(100), CLOCK);
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(ExpectedCount.twice(), requestTo("http://api.test/fixtures?live=all"))
                .andRespond(withSuccess(FIXTURES_JSON, MediaType.APPLICATION_JSON));

        ApiFootballClient client = new ApiFootballClient(props(100), builder, store, quota);
        client.get("/fixtures", Map.of("live", "all"), FixtureItem.class, Duration.ZERO);
        client.get("/fixtures", Map.of("live", "all"), FixtureItem.class, Duration.ZERO);

        server.verify();   // doua cereri HTTP: ttl=0 nu citeste/nu scrie cache
        boolean anythingCached = store.values.keySet().stream().anyMatch(k -> k.startsWith("golstat:af:cache:"));
        assertFalse(anythingCached, "ttl=0 nu scrie in cache");
        assertEquals(2, quota.used());
    }

    @Test
    void customTtl_isWrittenToStore() {
        InMemoryCounterStore store = new InMemoryCounterStore();
        QuotaGuard quota = new QuotaGuard(store, props(100), CLOCK);
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(ExpectedCount.once(), requestTo("http://api.test/fixtures?league=39"))
                .andRespond(withSuccess(FIXTURES_JSON, MediaType.APPLICATION_JSON));

        ApiFootballClient client = new ApiFootballClient(props(100), builder, store, quota);
        client.get("/fixtures", Map.of("league", 39), FixtureItem.class, Duration.ofHours(24));

        String cacheKey = store.values.keySet().stream()
                .filter(k -> k.startsWith("golstat:af:cache:")).findFirst().orElseThrow();
        assertEquals(Duration.ofHours(24), store.ttls.get(cacheKey));
    }

    @Test
    void detecteazaDoarLimitaPeMinut() {
        assertTrue(ApiFootballClient.esteRateLimitPeMinut(
                "{\"response\":[],\"errors\":{\"rateLimit\":\"Too many requests...\"}}"));
        assertFalse(ApiFootballClient.esteRateLimitPeMinut(
                "{\"response\":[],\"errors\":{\"token\":\"invalid\"}}"));
        assertFalse(ApiFootballClient.esteRateLimitPeMinut(
                "{\"response\":[{\"fixture\":{\"id\":1}}]}"));
        assertFalse(ApiFootballClient.esteRateLimitPeMinut(null));
    }
}
