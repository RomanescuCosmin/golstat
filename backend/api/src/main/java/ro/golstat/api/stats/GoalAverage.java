package ro.golstat.api.stats;

/**
 * Proiectie agregata a mediilor de goluri pe o liga/sezon (gazde vs. oaspeti). Getterele sunt
 * {@code null} cand liga nu are inca meciuri terminale (avg peste zero randuri).
 */
public interface GoalAverage {

    Double getAvgGazde();

    Double getAvgOaspeti();
}
