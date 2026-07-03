package ro.golstat.collector.collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ro.golstat.collector.provider.apifootball.ApiFootballQuotaExceededException;

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

    public CollectionPlanner(CollectionService collection, CollectionProperties props) {
        this.collection = collection;
        this.props = props;
    }

    @Scheduled(
            fixedDelayString = "${golstat.collection.interval-ms:3600000}",
            initialDelayString = "${golstat.collection.initial-delay-ms:5000}"
    )
    public void collect() {
        for (LeagueTarget target : props.leagues()) {
            try {
                collection.collectGoalsData(target.leagueId(), target.season(), props.from(), props.to());
            } catch (ApiFootballQuotaExceededException e) {
                // Cota e globala pe zi: daca s-a atins la o liga, nu mai are rost sa incercam restul.
                log.warn("Colectare oprita (cota atinsa): {}", e.getMessage());
                return;
            }
        }
    }
}
