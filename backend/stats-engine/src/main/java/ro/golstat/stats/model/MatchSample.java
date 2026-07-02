package ro.golstat.stats.model;

import java.time.LocalDate;

/**
 * Un meci trecut vazut din perspectiva unei echipe: "cate am dat / am primit EU",
 * indiferent daca echipa a fost gazda sau oaspete. Golurile din repriza 2 se deriva
 * ({@code goalsFor - goalsForHt}).
 */
public record MatchSample(
        LocalDate date,          // ca sa ordonam si sa alegem ultimele N
        boolean home,            // echipa NOASTRA a jucat acasa in acest meci?
        int goalsFor,            // goluri marcate de noi (final)
        int goalsAgainst,        // goluri primite de noi (final)
        int goalsForHt,          // goluri marcate de noi pana la pauza
        int goalsAgainstHt,      // goluri primite de noi pana la pauza
        Integer opponentRank     // pozitia adversarului in clasament (null = necunoscut)
) {
    public int goalsForSecondHalf() {
        return goalsFor - goalsForHt;
    }

    public int goalsAgainstSecondHalf() {
        return goalsAgainst - goalsAgainstHt;
    }
}
