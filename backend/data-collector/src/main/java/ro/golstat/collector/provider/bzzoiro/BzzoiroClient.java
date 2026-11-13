package ro.golstat.collector.provider.bzzoiro;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Client HTTP sincron peste bzzoiro. Fara cache Redis: e folosit la importuri one-shot, unde fiecare
 * resursa se cere o singura data. Cota (7500/zi) o raporteaza serverul in header-ul {@code RateLimit}.
 */
@Component
@Profile("bzzoiro")
public class BzzoiroClient {

    private static final Logger log = LoggerFactory.getLogger(BzzoiroClient.class);
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final int PAGE_SIZE = 200;
    private static final int MAX_RETRIES = 5;

    private final RestClient http;
    private final AtomicInteger requesturi = new AtomicInteger();

    public BzzoiroClient(BzzoiroProperties props, RestClient.Builder builder) {
        this.http = builder
                .baseUrl(props.baseUrl())
                .defaultHeader("Authorization", "Token " + props.apiKey())
                .build();
    }

    public JsonNode get(String path, Map<String, Object> params) {
        String body = fetch(path, params);
        try {
            return JSON.readTree(body == null ? "{}" : body);
        } catch (JsonProcessingException e) {
            throw new BzzoiroException("bzzoiro " + path + " raspuns invalid: " + e.getOriginalMessage());
        }
    }

    /** Parcurge toate paginile ({@code limit}/{@code offset}) si intoarce elementele din {@code results}. */
    public List<JsonNode> getAll(String path, Map<String, Object> params) {
        List<JsonNode> all = new ArrayList<>();
        int offset = 0;
        while (true) {
            Map<String, Object> p = new java.util.HashMap<>(params);
            p.put("limit", PAGE_SIZE);
            p.put("offset", offset);
            JsonNode page = get(path, p);
            JsonNode results = page.path("results");
            results.forEach(all::add);
            // avansam cu cat a venit efectiv (serverul poate plafona `limit`); pagina goala = gata
            if (results.isEmpty() || page.path("next").isNull() || page.path("next").isMissingNode()) {
                return all;
            }
            offset += results.size();
        }
    }

    public int requesturiFacute() {
        return requesturi.get();
    }

    private String fetch(String path, Map<String, Object> params) {
        for (int incercare = 1; ; incercare++) {
            try {
                requesturi.incrementAndGet();
                return http.get()
                        .uri(b -> {
                            b.path(path);
                            params.forEach(b::queryParam);
                            return b.build();
                        })
                        .retrieve()
                        .body(String.class);
            } catch (HttpClientErrorException.TooManyRequests e) {
                // "taster_exhausted" = cota zilnica terminata → nu are rost sa reincercam
                if (e.getResponseBodyAsString().contains("taster_exhausted")) {
                    throw new BzzoiroException("bzzoiro: cota zilnica epuizata");
                }
                if (incercare > MAX_RETRIES) {
                    throw e;
                }
                log.warn("bzzoiro 429 la {}; reincerc ({}/{})", path, incercare, MAX_RETRIES);
                dormi(1000L * incercare);
            } catch (HttpClientErrorException.NotFound e) {
                return "{}";
            } catch (HttpServerErrorException | ResourceAccessException e) {
                // 5xx / timeout tranzitorii: fara retry, o singura eroare oprea toata liga
                if (incercare > MAX_RETRIES) {
                    throw e;
                }
                log.warn("bzzoiro {} la {}; reincerc ({}/{})", e.getClass().getSimpleName(), path,
                        incercare, MAX_RETRIES);
                dormi(2000L * incercare);
            }
        }
    }

    private static void dormi(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new BzzoiroException("Import intrerupt");
        }
    }
}
