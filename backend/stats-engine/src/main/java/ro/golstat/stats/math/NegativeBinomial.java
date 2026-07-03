package ro.golstat.stats.math;

/**
 * Distributia Negative Binomial pentru numere de evenimente cu SUPRA-DISPERSIE
 * ({@code Var > medie}) — faulturi, cartonase. Parametrizata prin medie {@code mean} si
 * dispersie {@code r} (nu {@code r, p}): apelantii gandesc in medii.
 *
 * <pre>
 *   p = r / (r + mean)            Var = mean + mean²/r      (r → ∞ ⇒ Poisson)
 *   pmf(0) = p^r
 *   pmf(k) = pmf(k−1) · (k−1+r)/k · (1−p)
 * </pre>
 *
 * Recurenta e stabila numeric si oglindeste stilul iterativ al clasei {@link Poisson}.
 */
public final class NegativeBinomial {

    private NegativeBinomial() {
    }

    /** P(X = k). */
    public static double pmf(double mean, double r, int k) {
        requireValid(mean, r, k);
        double p = r / (r + mean);
        double term = Math.pow(p, r); // pmf(0)
        for (int i = 1; i <= k; i++) {
            term *= (i - 1 + r) / i * (1 - p);
        }
        return term;
    }

    /** P(X &lt;= k). */
    public static double cdf(double mean, double r, int k) {
        requireValid(mean, r, k);
        double p = r / (r + mean);
        double term = Math.pow(p, r);
        double sum = term;
        for (int i = 1; i <= k; i++) {
            term *= (i - 1 + r) / i * (1 - p);
            sum += term;
        }
        return Math.min(1.0, sum);
    }

    /** P(X &gt;= k). */
    public static double atLeast(double mean, double r, int k) {
        requireValid(mean, r, k);
        if (k == 0) {
            return 1.0;
        }
        return Math.max(0.0, 1.0 - cdf(mean, r, k - 1));
    }

    /** P(total &gt; line) pentru o linie x.5 (ex. 4.5 → P(X &gt;= 5)). */
    public static double probabilityOver(double mean, double r, double line) {
        int threshold = (int) Math.floor(line) + 1;
        return atLeast(mean, r, threshold);
    }

    private static void requireValid(double mean, double r, int k) {
        if (mean < 0) {
            throw new IllegalArgumentException("mean trebuie >= 0, a fost " + mean);
        }
        if (r <= 0) {
            throw new IllegalArgumentException("r (dispersie) trebuie > 0, a fost " + r);
        }
        if (k < 0) {
            throw new IllegalArgumentException("k trebuie >= 0, a fost " + k);
        }
    }
}
