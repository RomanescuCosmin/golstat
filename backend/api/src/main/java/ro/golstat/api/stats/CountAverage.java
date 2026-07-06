package ro.golstat.api.stats;

/**
 * Proiectie agregata a mediilor de evenimente numarabile PE ECHIPA pe o liga/sezon, din
 * {@code fixture_team_stats}. Getterele sunt {@code null} cand liga nu are inca statistici
 * colectate (avg peste zero randuri).
 */
public interface CountAverage {

    Double getAvgCornere();

    Double getAvgFaulturi();

    Double getAvgCartonase();

    Double getAvgSuturi();

    Double getAvgSuturiPePoarta();
}
