package ro.golstat.collector.provider.apifootball;

import java.util.List;

/**
 * Un element din {@code /fixtures/lineups}: o echipa + formatie + startXI/rezerve + antrenor.
 * Nu poarta {@code fixtureId} (e parametrul cererii), asa ca maparea il primeste separat.
 */
public record LineupItem(Team team, String formation, List<Slot> startXI, List<Slot> substitutes, Coach coach) {

    public record Team(Long id) {
    }

    public record Coach(Long id, String name) {
    }

    public record Slot(Player player) {
    }

    public record Player(Long id, String name, Integer number, String pos, String grid) {
    }
}
