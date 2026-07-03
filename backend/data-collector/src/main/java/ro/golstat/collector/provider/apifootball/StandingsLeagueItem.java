package ro.golstat.collector.provider.apifootball;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Un element din {@code /standings}: liga contine {@code standings} ca lista de GRUPE, fiecare
 * grupa o lista de randuri (echipe). Ligile fara grupe au o singura grupa.
 */
public record StandingsLeagueItem(League league) {

    public record League(Long id, Integer season, List<List<Row>> standings) {
    }

    public record Row(Integer rank, Team team, Integer points, Integer goalsDiff,
                      String group, String form, String status, String description,
                      Stat all, Stat home, Stat away) {
    }

    public record Team(Long id) {
    }

    public record Stat(Integer played, Integer win, Integer draw, Integer lose, Goals goals) {
    }

    public record Goals(@JsonProperty("for") Integer forGoals, Integer against) {
    }
}
