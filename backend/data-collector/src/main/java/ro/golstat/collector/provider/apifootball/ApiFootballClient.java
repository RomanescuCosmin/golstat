package ro.golstat.collector.provider.apifootball;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
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

    private static final String CACHE_PREFIX = "golstat:af:cache:";

    private static final JsonMapper JSON = JsonMapper.builder()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
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
        List<T> items = deserialize(json, path, itemType);   // valideaza errors INAINTE de cache
        if (useCache) {
            store.set(cacheKey, json, ttl);
        }
        return items;
    }

    private String fetch(String path, Map<String, Object> params) {
        return http.get()
                .uri(builder -> {
                    builder.path(path);
                    params.forEach(builder::queryParam);
                    return builder.build();
                })
                .retrieve()
                .body(String.class);
    }

    private <T> List<T> deserialize(String json, String path, Class<T> itemType) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        ApiFootballResponse<T> body;
        try {
            JavaType type = JSON.getTypeFactory().constructParametricType(ApiFootballResponse.class, itemType);
            body = JSON.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new ApiFootballException("API-Football " + path + " raspuns invalid: " + e.getOriginalMessage());
        }
        if (body == null) {
            return List.of();
        }
        if (body.errors() != null && !body.errors().isEmpty()) {
            throw new ApiFootballException("API-Football " + path + " a returnat erori: " + body.errors());
        }
        return body.response() != null ? body.response() : List.of();
    }

    /** Cheie stabila (parametri sortati) → aceleasi query, aceeasi cheie, indiferent de ordine. */
    private static String cacheKey(String path, Map<String, Object> params) {
        StringBuilder sb = new StringBuilder(CACHE_PREFIX).append(path);
        new TreeMap<>(params).forEach((k, v) -> sb.append(';').append(k).append('=').append(v));
        return sb.toString();
    }
}
