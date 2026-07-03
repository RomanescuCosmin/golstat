package ro.golstat.stats.goals;

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

    /**
     * O linie de goluri (ex. 2.5): probabilitatea ca totalul din meci sa fie
     * peste (over) sau sub (under) linie. {@code overRate + underRate == 1}
     * (liniile x.5 nu au egalitate exact pe linie).
     */
    public record OverUnder(double line, double overRate, double underRate) {
    }
}
