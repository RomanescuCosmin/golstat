package ro.golstat.api.stats;

/**
 * Proiectie agregata a istoricului de cartonase al unui arbitru: media TOTALULUI de cartonase
 * pe meci (ambele echipe) si cate meciuri a arbitrat. {@code getAvgCards()} e {@code null} cand
 * arbitrul nu are meciuri terminale cu statistici colectate.
 */
public interface RefereeCardAverage {

    Double getAvgCards();

    Long getMatches();
}
