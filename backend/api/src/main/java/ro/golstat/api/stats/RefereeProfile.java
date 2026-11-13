package ro.golstat.api.stats;

/** Profilul unui arbitru din meciurile terminate colectate: totaluri PE MECI (ambele echipe). */
public interface RefereeProfile {

    Long getMatches();

    Double getAvgCards();

    Double getAvgFouls();

    Long getReds();
}
