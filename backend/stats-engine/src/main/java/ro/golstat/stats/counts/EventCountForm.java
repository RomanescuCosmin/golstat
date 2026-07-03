package ro.golstat.stats.counts;

import ro.golstat.stats.model.EventCountSample;

import java.util.List;

/** Agregate empirice de evenimente (media facute/primite pe meci) pe o fereastra. */
public final class EventCountForm {

    private EventCountForm() {
    }

    public static EventCountStats of(List<EventCountSample> window) {
        int n = window.size();
        if (n == 0) {
            return new EventCountStats(0, 0, 0);
        }
        int sumFor = window.stream().mapToInt(EventCountSample::countFor).sum();
        int sumAgainst = window.stream().mapToInt(EventCountSample::countAgainst).sum();
        return new EventCountStats(n, (double) sumFor / n, (double) sumAgainst / n);
    }
}
