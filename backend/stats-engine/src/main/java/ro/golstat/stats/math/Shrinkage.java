package ro.golstat.stats.math;

/**
 * Regresie spre medie (shrinkage) ponderata pe marimea esantionului:
 * {@code (n·value + K·target) / (n + K)}.
 *
 * <p>{@code n} mare → rezultatul e aproape de {@code value} (avem incredere in observatie);
 * {@code n} mic → aproape de {@code target} (cadem pe medie/baseline). {@code K} e constanta de
 * incredere. Utilitar comun: blend goluri/faulturi, factor arbitru, rate marcatori.
 */
public final class Shrinkage {

    private Shrinkage() {
    }

    public static double toward(double value, double n, double target, double k) {
        if (n < 0) {
            throw new IllegalArgumentException("n trebuie >= 0, a fost " + n);
        }
        if (k < 0) {
            throw new IllegalArgumentException("K trebuie >= 0, a fost " + k);
        }
        double denom = n + k;
        if (denom == 0) {
            return target; // n = 0 si K = 0 → nedefinit, cadem pe target
        }
        return (n * value + k * target) / denom;
    }
}
