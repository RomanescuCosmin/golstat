package ro.golstat.collector.provider.bzzoiro;

import com.fasterxml.jackson.databind.JsonNode;
import ro.golstat.common.FormatieGrid;
import ro.golstat.common.GolstatConstants.EventType;
import ro.golstat.common.GolstatConstants.FixtureStatus;
import ro.golstat.common.dto.FixtureDto;
import ro.golstat.common.dto.FixtureEventDto;
import ro.golstat.common.dto.FixtureLineupDto;
import ro.golstat.common.dto.FixtureLineupPlayerDto;
import ro.golstat.common.dto.FixtureTeamHalfStatsDto;
import ro.golstat.common.dto.FixtureTeamStatsDto;
import ro.golstat.common.dto.LeagueDto;
import ro.golstat.common.dto.SeasonDto;
import ro.golstat.common.dto.StandingDto;
import ro.golstat.common.dto.TeamDto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Traduce JSON-ul bzzoiro in DTO-urile din {@code common}, cu vocabularul API-Football pe care il
 * asteapta restul aplicatiei (status "FT"/"NS", evenimente "Goal"/"Card"/"subst").
 */
final class BzzoiroMapper {

    private BzzoiroMapper() {
    }

    /** Echipele gazda/oaspete ale unui meci; incidentele si statisticile vin doar cu home/away. */
    record Echipe(Long home, Long away) {
        Long dupaPartea(boolean isHome) {
            return isHome ? home : away;
        }
    }

    static FixtureDto toFixture(JsonNode e, long leagueId, int seasonYear) {
        return toFixture(e, leagueId, seasonYear, null);
    }

    static FixtureDto toFixture(JsonNode e, long leagueId, int seasonYear, String arbitru) {
        // home_score/away_score = scorul la 90'; extra_time_score = DOAR golurile din prelungiri.
        // Ca la API-Football: goals = scorul final (cu prelungiri), score.fulltime = scorul la 90'.
        JsonNode et = e.path("extra_time_score");
        Integer la90Home = intOrNull(e, "home_score");
        Integer la90Away = intOrNull(e, "away_score");
        Integer goalsHome = plus(la90Home, intOrNull(et, "home"));
        Integer goalsAway = plus(la90Away, intOrNull(et, "away"));
        JsonNode pen = e.path("penalty_shootout");
        String status = status(e);
        boolean terminat = FixtureStatus.TERMINAL.contains(status);
        String round = text(e, "round_label");
        return new FixtureDto(
                longOrNull(e, "id"),
                arbitru,
                "UTC",
                e.hasNonNull("event_date") ? OffsetDateTime.parse(e.get("event_date").asText()) : null,
                leagueId,
                seasonYear,
                round,
                longOrNull(e, "venue_id"),
                text(e, "status"),
                status,
                intOrNull(e, "current_minute"),
                longOrNull(e, "home_team_id"),
                longOrNull(e, "away_team_id"),
                goalsHome,
                goalsAway,
                intOrNull(e, "home_score_ht"),
                intOrNull(e, "away_score_ht"),
                terminat ? la90Home : null,
                terminat ? la90Away : null,
                intOrNull(et, "home"),
                intOrNull(et, "away"),
                intOrNull(pen, "home"),
                intOrNull(pen, "away"));
    }

    static String status(JsonNode e) {
        String s = text(e, "status");
        if (s == null) {
            return null;
        }
        return switch (s) {
            case "finished" -> {
                if (e.hasNonNull("penalty_shootout")) {
                    yield FixtureStatus.FINISHED_PEN;
                }
                yield e.hasNonNull("extra_time_score") ? FixtureStatus.FINISHED_AET : FixtureStatus.FINISHED;
            }
            case "notstarted" -> FixtureStatus.NOT_STARTED;
            case "postponed" -> FixtureStatus.POSTPONED;
            case "cancelled", "canceled" -> FixtureStatus.CANCELLED;
            case "inprogress" -> switch (String.valueOf(text(e, "period"))) {
                case "HT" -> FixtureStatus.HALF_TIME;
                case "2H" -> FixtureStatus.SECOND_HALF;
                case "ET" -> FixtureStatus.EXTRA_TIME;
                default -> FixtureStatus.FIRST_HALF;
            };
            default -> s.toUpperCase();
        };
    }

    static List<FixtureEventDto> toEvents(JsonNode body, long fixtureId, Echipe echipe) {
        List<FixtureEventDto> out = new ArrayList<>();
        for (JsonNode i : body.path("incidents")) {
            String type = text(i, "type");
            if (type == null || i.path("rescinded").asBoolean(false)) {
                continue;
            }
            Long team = i.has("is_home") ? echipe.dupaPartea(i.get("is_home").asBoolean()) : null;
            Integer minut = intOrNull(i, "minute");
            Integer extra = intOrNull(i, "added_time");
            switch (type) {
                case "goal" -> out.add(new FixtureEventDto(fixtureId, team, longOrNull(i, "player_id"),
                        asistent(i), minut, extra, EventType.GOAL, detaliuGol(text(i, "goal_type")), null));
                case "penalty_missed" -> out.add(new FixtureEventDto(fixtureId, team, longOrNull(i, "player_id"),
                        null, minut, extra, EventType.GOAL, "Missed Penalty", text(i, "reason")));
                case "card" -> out.add(new FixtureEventDto(fixtureId, team, longOrNull(i, "player_id"),
                        null, minut, extra, EventType.CARD, detaliuCartonas(text(i, "card_type")), text(i, "reason")));
                case "substitution" -> out.add(new FixtureEventDto(fixtureId, team, longOrNull(i, "player_out_id"),
                        longOrNull(i, "player_in_id"), minut, extra, EventType.SUBST, "Substitution", null));
                case "varDecision" -> out.add(new FixtureEventDto(fixtureId, team, longOrNull(i, "player_id"),
                        null, minut, extra, EventType.VAR, text(i, "text"), null));
                default -> {
                    // period / injuryTime: marcaje de timp, nu evenimente de joc
                }
            }
        }
        return out;
    }

    private static Long asistent(JsonNode gol) {
        for (JsonNode pas : gol.path("sequence")) {
            if (pas.path("assist").asBoolean(false) && pas.hasNonNull("pid")) {
                return pas.get("pid").asLong();
            }
        }
        return null;
    }

    private static String detaliuGol(String goalType) {
        return switch (normalizat(goalType)) {
            case "penalty" -> "Penalty";
            case "own", "owngoal" -> "Own Goal";
            default -> "Normal Goal";
        };
    }

    private static String detaliuCartonas(String cardType) {
        return switch (normalizat(cardType)) {
            case "red" -> "Red Card";
            case "yellowred", "secondyellow" -> "Second Yellow card";
            default -> "Yellow Card";
        };
    }

    /** bzzoiro scrie valorile camelCase ("ownGoal", "yellowRed"); comparam fara majuscule si fara "_". */
    private static String normalizat(String valoare) {
        return valoare == null ? "" : valoare.toLowerCase(java.util.Locale.ROOT).replace("_", "");
    }

    static List<FixtureTeamStatsDto> toTeamStats(JsonNode body, long fixtureId, Echipe echipe) {
        JsonNode stats = body.path("stats");
        List<FixtureTeamStatsDto> out = new ArrayList<>();
        if (stats.path("home").isObject()) {
            out.add(teamStats(stats.get("home"), fixtureId, echipe.home()));
        }
        if (stats.path("away").isObject()) {
            out.add(teamStats(stats.get("away"), fixtureId, echipe.away()));
        }
        return out;
    }

    private static FixtureTeamStatsDto teamStats(JsonNode s, long fixtureId, Long teamId) {
        Integer passes = intOrNull(s, "passes");
        Integer accurate = intOrNull(s, "accurate_passes");
        BigDecimal pct = passes != null && accurate != null && passes > 0
                ? BigDecimal.valueOf(accurate * 100.0 / passes).setScale(2, RoundingMode.HALF_UP)
                : null;
        return new FixtureTeamStatsDto(
                fixtureId,
                teamId,
                intOrNull(s, "shots_on_target"),
                intOrNull(s, "shots_off_target"),
                intOrNull(s, "total_shots"),
                intOrNull(s, "blocked_shots"),
                intOrNull(s, "shots_inside_box"),
                intOrNull(s, "shots_outside_box"),
                intOrNull(s, "fouls"),
                intOrNull(s, "corner_kicks"),
                intOrNull(s, "offsides"),
                decimalOrNull(s, "ball_possession"),
                intOrNull(s, "yellow_cards"),
                intOrNull(s, "red_cards"),
                intOrNull(s, "goalkeeper_saves"),
                passes,
                accurate,
                pct,
                decimalOrNull(s, "expected_goals"));
    }

    /** Pana la 4 randuri: (gazde, oaspeti) × (repriza 1, repriza 2), din {@code stats.first_half}/{@code stats.second_half}. */
    static List<FixtureTeamHalfStatsDto> toHalfStats(JsonNode body, long fixtureId, Echipe echipe) {
        List<FixtureTeamHalfStatsDto> out = new ArrayList<>();
        adaugaRepriza(out, body.path("stats").path("first_half"), fixtureId, echipe, 1);
        adaugaRepriza(out, body.path("stats").path("second_half"), fixtureId, echipe, 2);
        return out;
    }

    private static void adaugaRepriza(List<FixtureTeamHalfStatsDto> out, JsonNode repriza, long fixtureId,
                                      Echipe echipe, int half) {
        for (boolean home : new boolean[]{true, false}) {
            JsonNode s = repriza.path(home ? "home" : "away");
            // fara cornere raportate randul nu ajuta nicio piata pe reprize
            if (s.isObject() && s.path("corner_kicks").isNumber()) {
                out.add(new FixtureTeamHalfStatsDto(fixtureId, echipe.dupaPartea(home), half,
                        intOrNull(s, "corner_kicks"), intOrNull(s, "fouls"),
                        intOrNull(s, "yellow_cards"), intOrNull(s, "red_cards"),
                        intOrNull(s, "total_shots"), intOrNull(s, "shots_on_target"),
                        intOrNull(s, "offsides")));
            }
        }
    }

    static List<FixtureLineupDto> toLineups(JsonNode body, long fixtureId) {
        if (!"confirmed".equals(text(body, "lineup_status"))) {
            return List.of();
        }
        List<FixtureLineupDto> out = new ArrayList<>();
        for (String parte : List.of("home", "away")) {
            JsonNode l = body.path("lineups").path(parte);
            if (!l.isObject()) {
                continue;
            }
            Long teamId = longOrNull(l, "team_id");
            List<FixtureLineupPlayerDto> jucatori = new ArrayList<>();
            List<String> posturi = new ArrayList<>();
            for (JsonNode p : l.path("players")) {
                if (p.hasNonNull("id")) {
                    posturi.add(text(p, "position"));
                }
            }
            adaugaJucatori(jucatori, l.path("players"), fixtureId, teamId, false,
                    FormatieGrid.grid(posturi, text(l, "formation")));
            adaugaJucatori(jucatori, l.path("substitutes"), fixtureId, teamId, true, List.of());
            out.add(new FixtureLineupDto(fixtureId, teamId, text(l, "formation"), null, jucatori));
        }
        return out;
    }

    private static void adaugaJucatori(List<FixtureLineupPlayerDto> out, JsonNode lista, long fixtureId,
                                       Long teamId, boolean rezerva, List<String> grid) {
        int i = 0;
        for (JsonNode p : lista) {
            if (p.hasNonNull("id")) {
                String pozitie = i < grid.size() ? grid.get(i) : null;
                i++;
                out.add(new FixtureLineupPlayerDto(fixtureId, teamId, p.get("id").asLong(),
                        text(p, "short_name") != null ? text(p, "short_name") : text(p, "name"),
                        intOrNull(p, "jersey_number"), text(p, "position"), pozitie, rezerva));
            }
        }
    }

    // Image API bzzoiro: public, fara token si fara cota; hotlinking permis.
    private static final String IMG = "https://sports.bzzoiro.com/img/";

    static LeagueDto toLeague(JsonNode l, long leagueId) {
        Long idBzzoiro = longOrNull(l, "id");
        // competitiile continentale (tara "Europe" etc.) sunt cupe: faza ligii + eliminatorii
        String tip = "Europe".equals(text(l, "country")) ? "Cup" : "League";
        return new LeagueDto(leagueId, text(l, "name"), tip,
                idBzzoiro == null ? null : IMG + "league/" + idBzzoiro + "/", text(l, "country"));
    }

    static SeasonDto toSeason(JsonNode sezon, long leagueId) {
        return new SeasonDto(leagueId, intOrNull(sezon, "year"),
                dateOrNull(sezon, "start_date"), dateOrNull(sezon, "end_date"),
                sezon.path("is_current").asBoolean(false),
                true, true, true, true, true);
    }

    static TeamDto toTeam(JsonNode t) {
        Long id = longOrNull(t, "id");
        return new TeamDto(id, text(t, "name"), null, text(t, "country"),
                null, false, id == null ? null : IMG + "team/" + id + "/", longOrNull(t, "venue_id"));
    }

    static List<StandingDto> toStandings(JsonNode body, long leagueId, int seasonYear) {
        List<StandingDto> out = new ArrayList<>();
        for (JsonNode r : body.path("standings")) {
            out.add(new StandingDto(leagueId, seasonYear, longOrNull(r, "team_id"),
                    intOrNull(r, "position"), text(r, "group"), intOrNull(r, "pts"), intOrNull(r, "gd"),
                    text(r, "form"), null, text(r.path("zone"), "label"),
                    intOrNull(r, "played"), intOrNull(r, "won"), intOrNull(r, "drawn"), intOrNull(r, "lost"),
                    intOrNull(r, "gf"), intOrNull(r, "ga"),
                    null, null, null, null, null, null,
                    null, null, null, null, null, null));
        }
        return out;
    }

    private static Integer plus(Integer a, Integer b) {
        if (a == null) {
            return null;
        }
        return b == null ? a : a + b;
    }

    private static String text(JsonNode n, String f) {
        JsonNode v = n.path(f);
        return v.isNull() || v.isMissingNode() ? null : v.asText();
    }

    private static Integer intOrNull(JsonNode n, String f) {
        JsonNode v = n.path(f);
        return v.isNumber() ? v.asInt() : null;
    }

    private static Long longOrNull(JsonNode n, String f) {
        JsonNode v = n.path(f);
        return v.isNumber() ? v.asLong() : null;
    }

    private static BigDecimal decimalOrNull(JsonNode n, String f) {
        JsonNode v = n.path(f);
        return v.isNumber() ? v.decimalValue() : null;
    }

    private static LocalDate dateOrNull(JsonNode n, String f) {
        String s = text(n, f);
        return s == null ? null : LocalDate.parse(s);
    }
}
