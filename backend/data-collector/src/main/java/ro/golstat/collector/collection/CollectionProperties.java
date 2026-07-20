package ro.golstat.collector.collection;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Ce colecteaza scheduler-ul (din {@code golstat.collection.*}): lista de ligi/sezoane urmarite si
 * fereastra RULANTA relativa la azi — {@code zileInUrma} (rezultate recente care hranesc forma) si
 * {@code zileInainte} (meciuri viitoare {@code NS}). Fereastra concreta o calculeaza planner-ul din
 * ceas, la fiecare ciclu. Tintele de backfill isi aduc fereastra proprie (vezi {@link LeagueTarget}).
 *
 * <p>{@code mod}: {@code scheduled} (implicit) = bucla periodica, procesul ramane pornit;
 * {@code one-shot} = un singur ciclu, apoi procesul iese cu 0. Modul one-shot exista pentru rularea
 * din Windows Task Scheduler, cand PC-ul e trezit din sleep doar cat sa colecteze.
 */
@ConfigurationProperties(prefix = "golstat.collection")
public record CollectionProperties(List<LeagueTarget> leagues, int zileInUrma, int zileInainte, String mod) {

    public static final String MOD_SCHEDULED = "scheduled";
    public static final String MOD_ONE_SHOT = "one-shot";

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
        if (mod == null || mod.isBlank()) {
            mod = MOD_SCHEDULED;
        }
    }

    /**
     * Forma scurta pentru call-site-uri (teste): mod {@code scheduled}. Deliberat factory, nu al doilea
     * constructor — cu doua constructoare Spring nu mai stie pe care sa lege, cade pe binding JavaBean
     * si crapa cu „No default constructor found", fiindca un record n-are constructor fara argumente.
     */
    public static CollectionProperties scheduled(List<LeagueTarget> leagues, int zileInUrma, int zileInainte) {
        return new CollectionProperties(leagues, zileInUrma, zileInainte, MOD_SCHEDULED);
    }

    public boolean esteOneShot() {
        return MOD_ONE_SHOT.equalsIgnoreCase(mod);
    }
}
