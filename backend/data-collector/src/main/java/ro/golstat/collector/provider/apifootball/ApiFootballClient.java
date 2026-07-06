package ro.golstat.collector.provider.apifootball;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Client HTTP sincron peste API-Football, cu doua paze pentru cota planului free:
 * <ol>
 *   <li><b>cache-first</b>: raspunsul (JSON brut) sta in Redis {@code cacheTtl}; hit → zero HTTP;</li>
 *   <li><b>garda de cota</b>: la cache-miss cerem un slot de la {@link QuotaGuard}; epuizat → exceptie
 *       ({@link ApiFootballQuotaExceededException}), fara sa lovim API-ul.</li>
 * </ol>
 *
 * <p>Cheia merge in header-ul {@code x-apisports-key} (apel direct la api-sports.io). Doar
 * raspunsurile VALIDE se cache-uiesc: un envelope cu {@code errors} ne-gol devine exceptie si NU
 * intra in cache (altfel am servi la nesfarsit o eroare). {@code RestClient} (spring-web) e suficient
 * — nu tragem webflux pentru un colector blocant.
 */
@Component
@Profile("!stub")
public class ApiFootballClient {

    private static final Logger log = LoggerFactory.getLogger(ApiFootballClient.class);

    private static final String CACHE_PREFIX = "golstat:af:cache:";

    // Limita PE-MINUT a API-Football (diferita de cota zilnica pazita de QuotaGuard) se reseteaza la
    // fiecare minut. La 429 asteptam si reincercam acelasi apel cu backoff crescator (5s, 10s, 20s,
    // 40s, 60s) care depaseste sigur fereastra de un minut; dupa MAX incercari lasam eroarea sa urce.
    private static final int MAX_RETRIES_429 = 5;
    private static final Duration BACKOFF_INITIAL = Duration.ofSeconds(5);
    private static final Duration BACKOFF_MAX = Duration.ofSeconds(60);

    private static final JsonMapper JSON = JsonMapper.builder()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            // /teams/statistics intoarce `response` ca OBIECT unic, nu lista → il tratam ca lista de 1
            .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
            .build();

    private final RestClient http;
    private final CounterStore store;
    private final QuotaGuard quota;
    private final Duration cacheTtl;

    public ApiFootballClient(ApiFootballProperties props, RestClient.Builder builder,
                             CounterStore store, QuotaGuard quota) {
        this.http = builder
                .baseUrl(props.baseUrl())
                .defaultHeader("x-apisports-key", props.apiKey())
                .build();
        this.store = store;
        this.quota = quota;
        this.cacheTtl = props.cacheTtl();
    }

    public <T> List<T> get(String path, Map<String, Object> params, Class<T> itemType) {
        return get(path, params, itemType, cacheTtl);
    }

    /**
     * {@code ttl == 0} → ocoleste cache-ul complet (nici citire, nici scriere): fiecare apel loveste
     * API-ul. Folosit pentru LIVE, unde datele se schimba la fiecare poll (GS-15).
     */
    public <T> List<T> get(String path, Map<String, Object> params, Class<T> itemType, Duration ttl) {
        return getPage(path, params, itemType, ttl).items();
    }

    /**
     * Ca {@link #get}, dar parcurge TOATE paginile ({@code page=1..total}). Fiecare pagina = un slot de
     * cota si o cheie de cache proprie (parametrul {@code page} face parte din cheie). Folosit la
     * {@code /players}, unde un lot mare vine paginat.
     */
    public <T> List<T> getPaged(String path, Map<String, Object> params, Class<T> itemType, Duration ttl) {
        List<T> all = new ArrayList<>();
        int page = 1;
        int total;
        do {
            Map<String, Object> pageParams = new HashMap<>(params);
            pageParams.put("page", page);
            Page<T> p = getPage(path, pageParams, itemType, ttl);
            all.addAll(p.items());
            total = p.total();
            page++;
        } while (page <= total);
        return all;
    }

    private <T> Page<T> getPage(String path, Map<String, Object> params, Class<T> itemType, Duration ttl) {
        boolean useCache = !ttl.isZero();
        String cacheKey = cacheKey(path, params);
        if (useCache) {
            Optional<String> cached = store.get(cacheKey);
            if (cached.isPresent()) {
                return deserialize(cached.get(), path, itemType);
            }
        }
        if (!quota.tryAcquire()) {
            throw new ApiFootballQuotaExceededException(path);
        }
        String json = fetch(path, params);
        Page<T> page = deserialize(json, path, itemType);   // valideaza errors INAINTE de cache
        if (useCache) {
            store.set(cacheKey, json, ttl);
        }
        return page;
    }

    private String fetch(String path, Map<String, Object> params) {
        Duration backoff = BACKOFF_INITIAL;
        for (int incercare = 1; ; incercare++) {
            try {
                String body = http.get()
                        .uri(builder -> {
                            builder.path(path);
                            params.forEach(builder::queryParam);
                            return builder.build();
                        })
                        .retrieve()
                        .body(String.class);
                // API-Football raporteaza limita pe-minut ca 200 + errors.rateLimit (nu 429).
                // O tratam la fel ca 429: backoff + retry acelasi apel; dupa MAX, lasam body-ul
                // sa urce si deserialize() sa arunce eroarea normala.
                if (esteRateLimitPeMinut(body) && incercare <= MAX_RETRIES_429) {
                    log.warn("API-Football limita pe-minut (200+rateLimit) la {}; astept {}s si reincerc ({}/{})",
                            path, backoff.toSeconds(), incercare, MAX_RETRIES_429);
                    dormi(backoff);
                    backoff = backoff.multipliedBy(2).compareTo(BACKOFF_MAX) > 0 ? BACKOFF_MAX : backoff.multipliedBy(2);
                    continue;
                }
                return body;
            } catch (HttpClientErrorException.TooManyRequests e) {
                if (incercare > MAX_RETRIES_429) {
                    throw e;
                }
                log.warn("API-Football 429 (limita pe-minut) la {}; astept {}s si reincerc ({}/{})",
                        path, backoff.toSeconds(), incercare, MAX_RETRIES_429);
                dormi(backoff);
                backoff = backoff.multipliedBy(2).compareTo(BACKOFF_MAX) > 0 ? BACKOFF_MAX : backoff.multipliedBy(2);
            }
        }
    }

    /**
     * API-Football raporteaza depasirea limitei PE MINUT ca 200 cu {@code errors.rateLimit}
     * (distinct de limita zilnica, care are alta cheie si NU trebuie reincercata la nesfarsit).
     * Fast-path pe substring ca sa nu parsam fiecare raspuns valid.
     */
    static boolean esteRateLimitPeMinut(String body) {
        if (body == null || !body.contains("rateLimit")) {
            return false;
        }
        try {
            JsonNode errors = JSON.readTree(body).path("errors");
            return errors.has("rateLimit");
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    private static void dormi(Duration d) {
        try {
            Thread.sleep(d.toMillis());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new ApiFootballException("Colectare intrerupta in timpul backoff-ului 429");
        }
    }

    private <T> Page<T> deserialize(String json, String path, Class<T> itemType) {
        if (json == null || json.isBlank()) {
            return new Page<>(List.of(), 1);
        }
        ApiFootballResponse<T> body;
        try {
            JavaType type = JSON.getTypeFactory().constructParametricType(ApiFootballResponse.class, itemType);
            body = JSON.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new ApiFootballException("API-Football " + path + " raspuns invalid: " + e.getOriginalMessage());
        }
        if (body == null) {
            return new Page<>(List.of(), 1);
        }
        if (body.errors() != null && !body.errors().isEmpty()) {
            throw new ApiFootballException("API-Football " + path + " a returnat erori: " + body.errors());
        }
        List<T> items = body.response() != null ? body.response() : List.of();
        int total = body.paging() != null && body.paging().total() != null && body.paging().total() > 0
                ? body.paging().total() : 1;
        return new Page<>(items, total);
    }

    /** Rezultatul unei pagini: elementele + numarul total de pagini (pentru iterare in {@link #getPaged}). */
    private record Page<T>(List<T> items, int total) {
    }

    /** Cheie stabila (parametri sortati) → aceleasi query, aceeasi cheie, indiferent de ordine. */
    private static String cacheKey(String path, Map<String, Object> params) {
        StringBuilder sb = new StringBuilder(CACHE_PREFIX).append(path);
        new TreeMap<>(params).forEach((k, v) -> sb.append(';').append(k).append('=').append(v));
        return sb.toString();
    }
}
