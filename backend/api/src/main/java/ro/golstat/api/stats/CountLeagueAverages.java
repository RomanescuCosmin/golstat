package ro.golstat.api.stats;

/**
 * Mediile TOTALURILOR pe meci ale ligii (ambele echipe la un loc), rezolvate cu fallback global —
 * intra ca "media ligii" in modelele numarabile. Pentru cornere / faulturi / cartonase avem si
 * impartirea gazde / oaspeti, pe care se aplica modelul per echipa ({@code TeamCountModel}), iar la
 * cornere si media pe fiecare repriza.
 */
public record CountLeagueAverages(
        double cornerePeMeci,
        double faulturiPeMeci,
        double cartonasePeMeci,
        double suturiPeMeci,
        double suturiPePoartaPeMeci,
        PeLocatie cornere,
        PeLocatie faulturi,
        PeLocatie cartonase,
        PeLocatie cornereRepriza1,
        PeLocatie cornereRepriza2
) {

    /** Fara date pe reprize: ~45% din cornere cad in prima repriza (media ligilor importate). */
    static final double PONDERE_REPRIZA1 = 0.45;

    /** Media pe meci a gazdelor, respectiv a oaspetilor. */
    public record PeLocatie(double gazde, double oaspeti) {

        static PeLocatie egal(double total) {
            return new PeLocatie(total / 2, total / 2);
        }

        PeLocatie parte(double pondere) {
            return new PeLocatie(gazde * pondere, oaspeti * pondere);
        }
    }

    /** Doar totalurile; impartirea pe locatie cade pe jumatate-jumatate (teste, fallback). */
    public CountLeagueAverages(double cornerePeMeci, double faulturiPeMeci, double cartonasePeMeci,
                               double suturiPeMeci, double suturiPePoartaPeMeci) {
        this(cornerePeMeci, faulturiPeMeci, cartonasePeMeci, suturiPeMeci, suturiPePoartaPeMeci,
                PeLocatie.egal(cornerePeMeci), PeLocatie.egal(faulturiPeMeci), PeLocatie.egal(cartonasePeMeci));
    }

    public CountLeagueAverages(double cornerePeMeci, double faulturiPeMeci, double cartonasePeMeci,
                               double suturiPeMeci, double suturiPePoartaPeMeci,
                               PeLocatie cornere, PeLocatie faulturi, PeLocatie cartonase) {
        this(cornerePeMeci, faulturiPeMeci, cartonasePeMeci, suturiPeMeci, suturiPePoartaPeMeci,
                cornere, faulturi, cartonase, cornere.parte(PONDERE_REPRIZA1), cornere.parte(1 - PONDERE_REPRIZA1));
    }
}
