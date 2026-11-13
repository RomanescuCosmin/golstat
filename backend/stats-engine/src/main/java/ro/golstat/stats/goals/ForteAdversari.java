package ro.golstat.stats.goals;

import java.util.Map;

/**
 * Forta echipelor intalnite: golurile marcate / primite pe meci din toate meciurile lor anterioare, plus
 * {@code medie} = golurile pe echipa pe meci la nivel global (referinta „adversar obisnuit").
 */
public record ForteAdversari(Map<Long, Forta> forte, double medie) {

    public record Forta(double marcatePeMeci, double primitePeMeci, int meciuri) {
    }

    public static ForteAdversari fara() {
        return new ForteAdversari(Map.of(), 0);
    }

    /**
     * Cat valoreaza un gol MARCAT contra acestui adversar: {@code medie / primitePeMeci} (o aparare care
     * primeste putin → gol mai valoros), tras spre 1.0 cand adversarul are putine meciuri.
     */
    public double factorMarcate(Long adversar) {
        return factor(adversar, true);
    }

    /** Cat valoreaza un gol PRIMIT de la acest adversar: {@code medie / marcatePeMeci} (atac slab → gol mai grav). */
    public double factorPrimite(Long adversar) {
        return factor(adversar, false);
    }

    /** Constanta de incredere in forta unui adversar (in meciuri). */
    static final double K = 5;
    /** Podea pentru media adversarului, ca o echipa cu 0 goluri sa nu dea factori infiniti. */
    static final double MINIM = 0.2;

    private double factor(Long adversar, boolean marcate) {
        Forta f = adversar != null ? forte.get(adversar) : null;
        if (f == null || f.meciuri() == 0 || medie <= 0) {
            return 1.0;
        }
        double referinta = marcate ? f.primitePeMeci() : f.marcatePeMeci();
        double raport = medie / Math.max(referinta, MINIM);
        return (f.meciuri() * raport + K) / (f.meciuri() + K);
    }
}
