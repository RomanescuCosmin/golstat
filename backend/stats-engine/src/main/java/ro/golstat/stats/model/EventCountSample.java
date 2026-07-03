package ro.golstat.stats.model;

import java.time.LocalDate;

/**
 * Un meci trecut din perspectiva unei echipe, pentru pietele NUMARABILE
 * (faulturi, cornere, cartonase): cate evenimente am facut EU vs cate a facut adversarul.
 * Ce fel de eveniment e (faulturi / cornere / cartonase) il da contextul ferestrei pe care
 * o construiesti, nu un camp — de-aia campurile sunt {@code countFor}/{@code countAgainst}
 * generice, nu specifice unei piete.
 */
public record EventCountSample(
        LocalDate date,
        boolean home,
        int countFor,          // evenimente ale echipei noastre in acel meci
        int countAgainst,      // evenimente ale adversarului
        Integer opponentRank
) implements TeamMatch {
}
