package ro.golstat.stats.math;

/**
 * Distributia Poisson pentru numarul de evenimente dintr-un meci (goluri, cornere).
 * Toate metodele sunt pure si deterministe. {@code lambda} = rata medie asteptata (>= 0).
 *
 * <p>pmf/cdf se calculeaza iterativ ({@code term *= lambda / i}) ca sa evitam
 * factorial + putere care ar da overflow; e si mai stabil numeric.
 */
public final class Poisson {

    private Poisson() {
    }

    /** P(X = k): probabilitatea de exact k evenimente. */
    public static double pmf(double lambda, int k) {
        requireValid(lambda, k);
        double term = Math.exp(-lambda); // pmf(0)
        for (int i = 1; i <= k; i++) {
            term *= lambda / i;
        }
        return term;
    }

    /** P(X &lt;= k): probabilitatea cumulata pana la k inclusiv. */
    public static double cdf(double lambda, int k) {
        requireValid(lambda, k);
        double term = Math.exp(-lambda);
        double sum = term;
        for (int i = 1; i <= k; i++) {
            term *= lambda / i;
            sum += term;
        }
        return Math.min(1.0, sum);
    }

    /** P(X &gt;= k): coada superioara (survival). */
    public static double atLeast(double lambda, int k) {
        requireValid(lambda, k);
        if (k == 0) {
            return 1.0;
        }
        return Math.max(0.0, 1.0 - cdf(lambda, k - 1));
    }

    /**
     * P(total &gt; line) pentru o linie de pariere (ex. 2.5 → P(X &gt;= 3)).
     * Liniile sunt de forma x.5, deci nu exista egalitate exact pe linie.
     */
    public static double probabilityOver(double lambda, double line) {
        int threshold = (int) Math.floor(line) + 1; // 2.5 → 3
        return atLeast(lambda, threshold);
    }

    private static void requireValid(double lambda, int k) {
        if (lambda < 0) {
            throw new IllegalArgumentException("lambda trebuie >= 0, a fost " + lambda);
        }
        if (k < 0) {
            throw new IllegalArgumentException("k trebuie >= 0, a fost " + k);
        }
    }
}
