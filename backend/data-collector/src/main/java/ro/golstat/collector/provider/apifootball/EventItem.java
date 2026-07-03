package ro.golstat.collector.provider.apifootball;

/**
 * Un element din {@code /fixtures/events}. Nu poarta {@code fixtureId} (e parametrul cererii),
 * asa ca maparea il primeste separat.
 */
public record EventItem(Time time, Team team, Player player, Player assist,
                        String type, String detail, String comments) {

    public record Time(Integer elapsed, Integer extra) {
    }

    public record Team(Long id) {
    }

    public record Player(Long id) {
    }
}
