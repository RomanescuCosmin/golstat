package ro.golstat.collector.collection;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Ce colecteaza scheduler-ul (din {@code golstat.collection.*}): lista de ligi/sezoane urmarite si
 * fereastra RULANTA relativa la azi — {@code zileInUrma} (rezultate recente care hranesc forma) si
 * {@code zileInainte} (meciuri viitoare {@code NS}). Fereastra concreta o calculeaza planner-ul din
 * ceas, la fiecare ciclu.
 */
@ConfigurationProperties(prefix = "golstat.collection")
public record CollectionProperties(List<LeagueTarget> leagues, int zileInUrma, int zileInainte) {

    public CollectionProperties {
        if (leagues == null) {
            leagues = List.of();
        }
        if (zileInUrma <= 0) {
            zileInUrma = 90;
        }
        if (zileInainte <= 0) {
            zileInainte = 10;
        }
    }
}
