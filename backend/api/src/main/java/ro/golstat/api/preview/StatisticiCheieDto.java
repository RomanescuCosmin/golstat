package ro.golstat.api.preview;

/**
 * Statisticile cheie pe fereastra recenta (barele din design), medii per echipa din
 * {@code fixture_team_stats}. O valoare este {@code null} cand echipa nu are statistici colectate
 * pentru acel camp — "fara date", diferit de media 0. Golurile pe meci NU se repeta aici: sunt
 * deja in {@link FormaEchipaDto}.
 */
public record StatisticiCheieDto(StatisticiEchipaDto gazde, StatisticiEchipaDto oaspeti) {

    public record StatisticiEchipaDto(
            Double posesieMedie,
            Double suturiPeMeci,
            Double suturiPePoarta,
            Double cornerePeMeci,
            Double cartonasePeMeci
    ) {
    }
}
