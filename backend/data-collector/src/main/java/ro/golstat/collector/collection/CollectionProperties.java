package ro.golstat.collector.collection;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.LocalDate;

/** Ce liga/sezon/interval colecteaza scheduler-ul (din {@code golstat.collection.*}). */
@ConfigurationProperties(prefix = "golstat.collection")
public record CollectionProperties(long leagueId, int season, LocalDate from, LocalDate to) {
}
