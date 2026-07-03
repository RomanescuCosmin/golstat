package ro.golstat.collector.provider.apifootball;

/**
 * Un element din {@code /injuries}: jucatorul indisponibil (cu {@code type}/{@code reason} in
 * nodul {@code player}, asa vine din API), echipa, meciul vizat si liga/sezonul.
 */
public record InjuryItem(Player player, Team team, Fixture fixture, League league) {

    public record Player(Long id, String name, String type, String reason) {
    }

    public record Team(Long id) {
    }

    public record Fixture(Long id, String date) {
    }

    public record League(Long id, Integer season) {
    }
}
