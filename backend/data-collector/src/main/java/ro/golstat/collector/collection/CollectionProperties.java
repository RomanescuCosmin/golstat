package ro.golstat.collector.collection;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.LocalDate;
import java.util.List;

/**
 * Ce colecteaza scheduler-ul (din {@code golstat.collection.*}): lista de ligi/sezoane urmarite si
 * fereastra de meciuri. Fereastra e inca fixa ({@code from}/{@code to}) — devine rulanta la GS-11.
 */
@ConfigurationProperties(prefix = "golstat.collection")
public record CollectionProperties(List<LeagueTarget> leagues, LocalDate from, LocalDate to) {

    public CollectionProperties {
        if (leagues == null) {
            leagues = List.of();
        }
    }
}
