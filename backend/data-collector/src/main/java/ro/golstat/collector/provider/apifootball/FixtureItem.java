package ro.golstat.collector.provider.apifootball;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Un element din {@code /fixtures}: meciul, liga/sezon, cele doua echipe, golurile si scorurile.
 * La {@code fixtures?live=all} raspunsul include si {@code events} inline (gratis) — {@code null}
 * la raspunsurile {@code /fixtures} normale.
 */
public record FixtureItem(Fixture fixture, League league, Teams teams, Goals goals, Score score,
                          List<EventItem> events) {

    public record Fixture(Long id, String referee, String timezone, String date, Venue venue, Status status) {
    }

    public record Venue(Long id, String name, String city) {
    }

    public record Status(
            @JsonProperty("long") String longStatus,
            @JsonProperty("short") String shortStatus,
            Integer elapsed) {
    }

    public record League(Long id, Integer season, String round) {
    }

    public record Teams(Side home, Side away) {
    }

    public record Side(Long id) {
    }

    public record Goals(Integer home, Integer away) {
    }

    public record Score(Pair halftime, Pair fulltime, Pair extratime, Pair penalty) {
    }

    public record Pair(Integer home, Integer away) {
    }
}
