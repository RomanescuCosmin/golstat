package ro.golstat.collector.provider.bzzoiro;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * Config bzzoiro (sports.bzzoiro.com). {@code ligi} mapeaza id-ul de liga folosit in aplicatie
 * (cel din API-Football, pe care il stie frontend-ul) pe id-ul ligii la bzzoiro — astfel ligile
 * raman cu aceleasi id-uri indiferent de furnizor.
 */
@ConfigurationProperties(prefix = "golstat.bzzoiro")
public record BzzoiroProperties(String baseUrl, String apiKey, Map<Long, Long> ligi) {

    public BzzoiroProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://sports.bzzoiro.com/api/v2";
        }
        ligi = ligi == null ? Map.of() : Map.copyOf(ligi);
    }
}
