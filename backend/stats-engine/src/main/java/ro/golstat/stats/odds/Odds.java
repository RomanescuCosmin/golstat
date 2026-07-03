package ro.golstat.stats.odds;

/**
 * Conversie probabilitate → cota statistica (stil Flashscore): {@code cota = 1 / p}.
 * Sursa de adevar ramane procentul; cota e derivata doar pentru afisare, alaturi de
 * cota informativa din API-Football.
 */
public final class Odds {

    private Odds() {
    }

    /**
     * Cota corecta (fara marja): {@code 1 / p}. {@code p} in [0, 1].
     * {@code p == 0} (eveniment nevazut in esantion) → cota infinita.
     */
    public static double fromProbability(double probability) {
        validate(probability);
        if (probability == 0.0) {
            return Double.POSITIVE_INFINITY;
        }
        return 1.0 / probability;
    }

    /**
     * Cota cu marja de casa: reduce plata cu {@code margin} (ex. 0.05 = 5%).
     * {@code cota = 1 / (p * (1 + margin))}. Marja mai mare → cota mai mica.
     */
    public static double withMargin(double probability, double margin) {
        if (margin < 0) {
            throw new IllegalArgumentException("margin trebuie >= 0, a fost " + margin);
        }
        validate(probability);
        if (probability == 0.0) {
            return Double.POSITIVE_INFINITY;
        }
        return 1.0 / (probability * (1.0 + margin));
    }

    private static void validate(double probability) {
        if (probability < 0.0 || probability > 1.0) {
            throw new IllegalArgumentException("probabilitatea trebuie in [0,1], a fost " + probability);
        }
    }
}
