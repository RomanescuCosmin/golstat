package ro.golstat.collector.collection;

/** O tinta de colectare: o competitie intr-un sezon anume (ex. CM 2026, Premier League 2025). */
public record LeagueTarget(long leagueId, int season) {
}
