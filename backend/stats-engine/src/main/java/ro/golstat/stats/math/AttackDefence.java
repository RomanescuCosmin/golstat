package ro.golstat.stats.math;

/**
 * Factorul atac × aparare al unei echipe fata de media ligii:
 *
 * <pre>
 *   factor = shrink(facute de echipa / medie) × shrink(primite de adversar / medie)
 * </pre>
 *
 * Fiecare raport e tras spre 1.0 cu {@link Shrinkage} (esantion mic ⇒ factor ≈ 1 ⇒ media ligii).
 * {@code medie} e media ligii pe LOCATIA ferestrei (gazdele acasa se compara cu media gazdelor).
 * Ferestrele sunt ultimele 7 meciuri pe locatie, deci K mic: forma echipelor cantareste cel mai mult.
 */
public final class AttackDefence {

    public static final double K = 3;

    private AttackDefence() {
    }

    public static double factor(double medie, double facute, int nEchipa, double primiteAdversar, int nAdversar,
                                double k) {
        if (medie <= 0) {
            return 1.0;
        }
        double atac = Shrinkage.toward(facute / medie, nEchipa, 1.0, k);
        double slabiciune = Shrinkage.toward(primiteAdversar / medie, nAdversar, 1.0, k);
        return atac * slabiciune;
    }
}
