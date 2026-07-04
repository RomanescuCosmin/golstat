package ro.golstat.collector.provider.apifootball;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Config-ul clientului API-Football (din {@code golstat.api-football.*}).
 * {@code dailyRequestLimit} = cota planului; TTL-urile de cache sunt diferentiate pe volatilitate:
 * {@code cacheTtl} (implicit, catalog/clasament), {@code ttlUpcoming} (fixtures — contin meciuri
 * viitoare/live care se schimba des), {@code ttlHistoric} (evenimente de meci terminat — imuabile).
 * Valori absente → default-uri sanatoase.
 */
@ConfigurationProperties(prefix = "golstat.api-football")
public record ApiFootballProperties(
        String baseUrl,
        String apiKey,
        Integer dailyRequestLimit,
        Duration cacheTtl,
        Duration ttlUpcoming,
        Duration ttlHistoric,
        Duration ttlTeamStats,
        Duration ttlPlayers,
        Duration ttlCoaches) {

    public ApiFootballProperties {
        if (dailyRequestLimit == null) {
            dailyRequestLimit = 100;
        }
        if (cacheTtl == null) {
            cacheTtl = Duration.ofHours(6);
        }
        if (ttlUpcoming == null) {
            ttlUpcoming = Duration.ofHours(1);
        }
        if (ttlHistoric == null) {
            ttlHistoric = Duration.ofHours(24);
        }
        if (ttlTeamStats == null) {
            ttlTeamStats = Duration.ofHours(20);   // statisticile de sezon se schimba lent
        }
        if (ttlPlayers == null) {
            ttlPlayers = Duration.ofDays(7);
        }
        if (ttlCoaches == null) {
            ttlCoaches = Duration.ofDays(7);
        }
    }
}
