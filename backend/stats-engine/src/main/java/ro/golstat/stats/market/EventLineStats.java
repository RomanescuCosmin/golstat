package ro.golstat.stats.market;

import java.util.List;

/**
 * Probabilitatile pe linii Over/Under pentru o piata numarabila (faulturi, cornere, cartonase).
 * Spre deosebire de goluri, nu are BTTS. {@code sampleSize} = nr. meciuri din fereastra
 * (pentru increderea afisata).
 */
public record EventLineStats(int sampleSize, List<OverUnder> lines) {

    public boolean hasData() {
        return sampleSize > 0;
    }

    /** Rezultatul pentru o linie (ex. 4.5); null daca linia n-a fost ceruta. */
    public OverUnder line(double line) {
        return lines.stream()
                .filter(l -> l.line() == line)
                .findFirst()
                .orElse(null);
    }
}
