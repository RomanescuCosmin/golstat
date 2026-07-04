package ro.golstat.collector.provider.apifootball;

import java.util.List;

/**
 * Un element din {@code /players?team=&season=}: profilul jucatorului + {@code statistics[]}
 * (o intrare per competitie in sezonul cerut). {@code rating} vine string ("7.53"); {@code games.appearences}
 * e scris cu grafia API-Football.
 */
public record PlayerItem(Player player, List<Statistic> statistics) {

    public record Player(Long id, String name, String firstname, String lastname, Integer age, Birth birth,
                         String nationality, String height, String weight, Boolean injured, String photo) {
    }

    public record Birth(String date, String place, String country) {
    }

    public record Statistic(Team team, League league, Games games, Goals goals, Shots shots, Passes passes,
                            Tackles tackles, Duels duels, Dribbles dribbles, Fouls fouls, Cards cards,
                            Penalty penalty) {
    }

    public record Team(Long id) {
    }

    public record League(Long id, Integer season) {
    }

    public record Games(String position, Integer appearences, Integer lineups, Integer minutes,
                        String rating, Boolean captain) {
    }

    public record Goals(Integer total, Integer conceded, Integer assists, Integer saves) {
    }

    public record Shots(Integer total, Integer on) {
    }

    public record Passes(Integer total, Integer key, Integer accuracy) {
    }

    public record Tackles(Integer total, Integer blocks, Integer interceptions) {
    }

    public record Duels(Integer total, Integer won) {
    }

    public record Dribbles(Integer attempts, Integer success) {
    }

    public record Fouls(Integer drawn, Integer committed) {
    }

    public record Cards(Integer yellow, Integer yellowred, Integer red) {
    }

    public record Penalty(Integer won, Integer commited, Integer scored, Integer missed, Integer saved) {
    }
}
