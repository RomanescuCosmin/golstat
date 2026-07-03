package ro.golstat.collector.provider.apifootball;

import java.util.List;

/**
 * Un element din {@code /fixtures/statistics}: o echipa + lista ei de perechi type/value.
 * Nu poarta {@code fixtureId} (e parametrul cererii), asa ca maparea il primeste separat.
 * {@code value} vine eterogen (numar, String "62%"/"1.8" sau null) → {@link Object}.
 */
public record StatisticsItem(Team team, List<Stat> statistics) {

    public record Team(Long id) {
    }

    public record Stat(String type, Object value) {
    }
}
