package ro.golstat.api.stats;

import ro.golstat.api.entity.Fixture;
import ro.golstat.stats.model.MatchSample;

import java.time.LocalDate;

/**
 * Traduce un {@link Fixture} persistat in {@link MatchSample} (intrarea motorului), din
 * perspectiva unei echipe: "cate am dat / am primit EU", indiferent daca am fost gazda sau oaspete.
 * {@code stats-engine} nu cunoaste entitati/DTO-uri — maparea sta aici, in {@code api}.
 */
public final class MatchSampleMapper {

    private MatchSampleMapper() {
    }

    public static MatchSample toSample(Fixture fixture, long teamId, Integer opponentRank) {
        boolean home = fixture.getHomeTeamId() != null && fixture.getHomeTeamId() == teamId;

        int goalsFor = home
                ? finalGoals(fixture.getScoreFtHome(), fixture.getGoalsHome())
                : finalGoals(fixture.getScoreFtAway(), fixture.getGoalsAway());
        int goalsAgainst = home
                ? finalGoals(fixture.getScoreFtAway(), fixture.getGoalsAway())
                : finalGoals(fixture.getScoreFtHome(), fixture.getGoalsHome());
        int goalsForHt = home ? nz(fixture.getScoreHtHome()) : nz(fixture.getScoreHtAway());
        int goalsAgainstHt = home ? nz(fixture.getScoreHtAway()) : nz(fixture.getScoreHtHome());

        LocalDate date = fixture.getKickoff() != null ? fixture.getKickoff().toLocalDate() : null;
        return new MatchSample(date, home, goalsFor, goalsAgainst, goalsForHt, goalsAgainstHt, opponentRank);
    }

    /** Scorul la 90 min: prefera {@code scoreFt} (exclude prelungirile la cupe), altfel {@code goals}. */
    private static int finalGoals(Integer scoreFt, Integer goals) {
        return scoreFt != null ? scoreFt : nz(goals);
    }

    private static int nz(Integer value) {
        return value != null ? value : 0;
    }
}
