package ro.golstat.api.stats;

/** Media de cornere PE ECHIPA pe repriza si locatie (gazde / oaspeti); {@code null} fara date. */
public interface CornereReprizeAverage {

    Double getRepriza1Gazde();

    Double getRepriza1Oaspeti();

    Double getRepriza2Gazde();

    Double getRepriza2Oaspeti();
}
