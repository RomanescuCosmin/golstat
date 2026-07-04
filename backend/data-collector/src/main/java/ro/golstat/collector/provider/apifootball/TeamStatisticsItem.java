package ro.golstat.collector.provider.apifootball;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Raspunsul {@code /teams/statistics} (un OBIECT unic, nu lista — vezi
 * {@code ACCEPT_SINGLE_VALUE_AS_ARRAY} in {@link ApiFootballClient}). Cartonasele vin pe bucket-uri
 * de minute ({@code "0-15"}, ...) → se insumeaza pentru totaluri. Mediile de goluri sunt string-uri.
 */
public record TeamStatisticsItem(
        League league,
        Team team,
        String form,
        Fixtures fixtures,
        Goals goals,
        @JsonProperty("clean_sheet") HomeAwayTotal cleanSheet,
        @JsonProperty("failed_to_score") HomeAwayTotal failedToScore,
        Cards cards) {

    public record League(Long id, Integer season) {
    }

    public record Team(Long id) {
    }

    public record Fixtures(HomeAwayTotal played, HomeAwayTotal wins, HomeAwayTotal draws, HomeAwayTotal loses) {
    }

    public record HomeAwayTotal(Integer home, Integer away, Integer total) {
    }

    public record Goals(@JsonProperty("for") Side forGoals, Side against) {
    }

    public record Side(HomeAwayTotal total, Average average) {
    }

    public record Average(String home, String away, String total) {
    }

    /** {@code yellow}/{@code red}: harta bucket-minute → detaliu; insumam {@code total} peste bucket-uri. */
    public record Cards(Map<String, Bucket> yellow, Map<String, Bucket> red) {
    }

    public record Bucket(Integer total, String percentage) {
    }
}
