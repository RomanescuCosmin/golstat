package ro.golstat.collector.collection;

/**
 * O tinta de colectare: o competitie intr-un sezon anume (ex. CM 2026, Premier League 2025).
 * {@code doarFixtures} = colecteaza NUMAI meciurile (pentru afisare), fara detalii per-meci
 * (formatii/evenimente/statistici) si fara imbogatirea echipelor (jucatori/statistici sezon) —
 * pentru competitii cu foarte multe meciuri unde ne intereseaza doar scorul (ex. amicale), ca sa
 * nu ardem cota API. Implicit {@code false}.
 */
public record LeagueTarget(long leagueId, int season, boolean doarFixtures) {
}
