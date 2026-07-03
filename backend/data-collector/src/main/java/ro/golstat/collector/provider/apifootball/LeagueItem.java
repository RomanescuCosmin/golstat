package ro.golstat.collector.provider.apifootball;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Un element din {@code /leagues}: liga, tara si sezoanele ei (cu acoperirea datelor). */
public record LeagueItem(League league, Country country, List<Season> seasons) {

    public record League(Long id, String name, String type, String logo) {
    }

    public record Country(String name) {
    }

    public record Season(Integer year, String start, String end, Boolean current, Coverage coverage) {
    }

    public record Coverage(Fixtures fixtures, Boolean standings) {
    }

    public record Fixtures(
            Boolean events,
            Boolean lineups,
            @JsonProperty("statistics_fixtures") Boolean statisticsFixtures,
            @JsonProperty("statistics_players") Boolean statisticsPlayers) {
    }
}
