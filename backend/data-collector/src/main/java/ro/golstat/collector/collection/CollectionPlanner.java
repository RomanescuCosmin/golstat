package ro.golstat.collector.collection;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Declanseaza colectarea periodic. Deocamdata minimal: o fereastra fixa din config. Ulterior
 * devine planificatorul care intinde colectarea pe zi cu prioritati si respecta cota (Pasii 3-4).
 */
@Component
public class CollectionPlanner {

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
        collection.collectGoalsData(props.leagueId(), props.season(), props.from(), props.to());
    }
}
