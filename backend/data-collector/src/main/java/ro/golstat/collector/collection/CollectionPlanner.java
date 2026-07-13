package ro.golstat.collector.collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ro.golstat.collector.provider.apifootball.ApiFootballQuotaExceededException;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Declanseaza colectarea periodic. Deocamdata minimal: o fereastra fixa din config. Ulterior
 * devine planificatorul care intinde colectarea pe zi cu prioritati si respecta cota (Pasii 3-4).
 *
 * <p>Cand cota zilnica e atinsa, ciclul se opreste elegant (nu arunca mai departe): ce era deja
 * in cache s-a colectat, restul se reia la urmatorul ciclu dupa reset.
 */
@Component
public class CollectionPlanner {

    private static final Logger log = LoggerFactory.getLogger(CollectionPlanner.class);

    private final CollectionService collection;
    private final CollectionProperties props;
    private final Clock clock;

    public CollectionPlanner(CollectionService collection, CollectionProperties props, Clock clock) {
        this.collection = collection;
        this.props = props;
        this.clock = clock;
    }

    @Scheduled(
            fixedDelayString = "${golstat.collection.interval-ms:3600000}",
            initialDelayString = "${golstat.collection.initial-delay-ms:5000}"
    )
    public void collect() {
        LocalDate today = LocalDate.now(clock);
        LocalDate from = today.minusDays(props.zileInUrma());
        LocalDate to = today.plusDays(props.zileInainte());

        for (LeagueTarget target : props.leagues()) {
            try {
                collection.collectGoalsData(target, from, to);
            } catch (ApiFootballQuotaExceededException e) {
                // Cota e globala pe zi: daca s-a atins la o liga, nu mai are rost sa incercam restul.
                log.warn("Colectare oprita (cota atinsa): {}", e.getMessage());
                return;
            }
        }
    }
}
