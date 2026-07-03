package ro.golstat.collector.provider.apifootball;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Config-ul clientului API-Football (din {@code golstat.api-football.*}).
 * {@code dailyRequestLimit} = cota planului (free: 100/zi); {@code cacheTtl} = cat tinem un raspuns
 * in Redis inainte sa mai lovim API-ul. Valori absente → default-uri sanatoase.
 */
@ConfigurationProperties(prefix = "golstat.api-football")
public record ApiFootballProperties(String baseUrl, String apiKey, Integer dailyRequestLimit, Duration cacheTtl) {

    public ApiFootballProperties {
        if (dailyRequestLimit == null) {
            dailyRequestLimit = 100;
        }
        if (cacheTtl == null) {
            cacheTtl = Duration.ofHours(6);
        }
    }
}
