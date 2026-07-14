package ro.golstat.collector.provider.apifootball;

import java.util.List;

/**
 * Un element din {@code /fixtures/players?fixture=}: o ECHIPA cu jucatorii ei, fiecare jucator avand
 * {@code statistics[]} cu o singura intrare (statisticile lui din acel meci).
 *
 * <p>Fata de {@code /players}: {@code games.rating} e string ("6.6"), iar {@code passes.accuracy} vine
 * ca string ("72") pe acest endpoint, desi e numar in {@code /players} → il tinem {@code Object} si il
 * trecem prin {@code toInt} (acelasi tratament ca {@code StatisticsItem.Stat.value}).
 */
public record FixturePlayersItem(Team team, List<PlayerEntry> players) {

    public record Team(Long id) {
    }

    public record PlayerEntry(Player player, List<Statistic> statistics) {
    }

    public record Player(Long id, String name) {
    }

    public record Statistic(Games games, Goals goals, Shots shots, Passes passes, Tackles tackles,
                            Duels duels, Dribbles dribbles, Fouls fouls, Cards cards, Penalty penalty) {
    }

    public record Games(Integer minutes, String position, String rating, Boolean captain, Boolean substitute) {
    }

    public record Goals(Integer total, Integer conceded, Integer assists, Integer saves) {
    }

    public record Shots(Integer total, Integer on) {
    }

    public record Passes(Integer total, Integer key, Object accuracy) {
    }

    public record Tackles(Integer total, Integer blocks, Integer interceptions) {
    }

    public record Duels(Integer total, Integer won) {
    }

    public record Dribbles(Integer attempts, Integer success) {
    }

    public record Fouls(Integer drawn, Integer committed) {
    }

    public record Cards(Integer yellow, Integer red) {
    }

    public record Penalty(Integer won, Integer commited, Integer scored, Integer missed, Integer saved) {
    }
}
