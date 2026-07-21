package ro.golstat.collector.provider.apifootball;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Config-ul clientului API-Football (din {@code golstat.api-football.*}).
 * {@code dailyRequestLimit} = cota planului; TTL-urile de cache sunt diferentiate pe volatilitate:
 * {@code cacheTtl} (implicit, catalog/clasament), {@code ttlUpcoming} (fixtures — contin meciuri
 * viitoare/live care se schimba des), {@code ttlHistoric} (evenimente de meci terminat — imuabile),
 * {@code ttlDetaliiGoale} (raspuns GOL la detalii de meci terminat — datele apar cu intarziere).
 * {@code rezervaZilnica} = cate requesturi din cota raman INTANGIBILE pentru munca zilnica (meciuri
 * recente, program, live); backfill-ul istoric are voie sa consume doar ce e peste rezerva, ca o
 * colectare de sezoane vechi sa nu lase ligile urmarite fara date. Implicit 0 (fara rezerva).
 * Valori absente → default-uri sanatoase.
 */
@ConfigurationProperties(prefix = "golstat.api-football")
public record ApiFootballProperties(
        String baseUrl,
        String apiKey,
        Integer dailyRequestLimit,
        Integer rezervaZilnica,
        Duration cacheTtl,
        Duration ttlUpcoming,
        Duration ttlHistoric,
        Duration ttlDetaliiGoale,
        Duration ttlTeamStats,
        Duration ttlPlayers,
        Duration ttlCoaches) {

    public ApiFootballProperties {
        if (dailyRequestLimit == null) {
            dailyRequestLimit = 100;
        }
        if (rezervaZilnica == null) {
            rezervaZilnica = 0;
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
        if (ttlDetaliiGoale == null) {
            // detalii de meci terminat inca nepublicate de furnizor: re-cerem la ciclul urmator,
            // dar dedublam retry-urile din interiorul aceluiasi ciclu / rularile manuale repetate
            ttlDetaliiGoale = Duration.ofMinutes(15);
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
