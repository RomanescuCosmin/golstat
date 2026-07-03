package ro.golstat.stats.goals;

import ro.golstat.stats.market.OverUnder;

import java.util.List;

/**
 * Probabilitatile empirice pe piata de goluri, calculate pe o fereastra de meciuri.
 * Ratele sunt fractii 0..1 (stratul de afisare le inmulteste cu 100).
 * {@code bttsRate} = fractia de meciuri in care AMBELE echipe au marcat.
 */
public record GoalLineStats(int sampleSize, List<OverUnder> lines, double bttsRate) {

    public boolean hasData() {
        return sampleSize > 0;
    }

    /** Rezultatul pentru o anumita linie (ex. 2.5); null daca linia n-a fost ceruta. */
    public OverUnder line(double line) {
        return lines.stream()
                .filter(l -> l.line() == line)
                .findFirst()
                .orElse(null);
    }
}
