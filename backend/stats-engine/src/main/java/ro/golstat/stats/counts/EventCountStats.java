package ro.golstat.stats.counts;

/**
 * Agregate empirice pentru o piata numarabila pe o fereastra: media de evenimente FACUTE
 * si PRIMITE pe meci. {@code avgCountTotal()} (facute + primite) e media care alimenteaza
 * modelul distributional (media NB la faulturi/cartonase, λ Poisson la cornere).
 */
public record EventCountStats(int sampleSize, double avgCountFor, double avgCountAgainst) {

    public boolean hasData() {
        return sampleSize > 0;
    }

    public double avgCountTotal() {
        return avgCountFor + avgCountAgainst;
    }
}
