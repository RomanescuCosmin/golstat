package ro.golstat.api.ingest;

import ro.golstat.api.entity.Fixture;
import ro.golstat.api.entity.FixtureEvent;
import ro.golstat.api.entity.Standing;
import ro.golstat.api.entity.Team;
import ro.golstat.common.dto.FixtureDto;
import ro.golstat.common.dto.FixtureEventDto;
import ro.golstat.common.dto.StandingDto;
import ro.golstat.common.dto.TeamDto;

/** Mapare DTO (din Kafka) → entitate JPA. Pura, fara efecte secundare. */
final class EntityMapper {

    private EntityMapper() {
    }

    static Team toTeam(TeamDto d) {
        Team t = new Team();
        t.setId(d.id());
        t.setName(d.name());
        t.setCode(d.code());
        t.setCountryName(d.countryName());
        t.setFounded(d.founded());
        t.setIsNational(d.isNational());
        t.setLogo(d.logo());
        t.setVenueId(d.venueId());
        return t;
    }

    static Fixture toFixture(FixtureDto d) {
        Fixture f = new Fixture();
        f.setId(d.id());
        f.setReferee(d.referee());
        f.setTimezone(d.timezone());
        f.setKickoff(d.kickoff());
        f.setLeagueId(d.leagueId());
        f.setSeasonYear(d.seasonYear());
        f.setRound(d.round());
        f.setVenueId(d.venueId());
        f.setStatusLong(d.statusLong());
        f.setStatusShort(d.statusShort());
        f.setStatusElapsed(d.statusElapsed());
        f.setHomeTeamId(d.homeTeamId());
        f.setAwayTeamId(d.awayTeamId());
        f.setGoalsHome(d.goalsHome());
        f.setGoalsAway(d.goalsAway());
        f.setScoreHtHome(d.scoreHtHome());
        f.setScoreHtAway(d.scoreHtAway());
        f.setScoreFtHome(d.scoreFtHome());
        f.setScoreFtAway(d.scoreFtAway());
        f.setScoreEtHome(d.scoreEtHome());
        f.setScoreEtAway(d.scoreEtAway());
        f.setScorePenHome(d.scorePenHome());
        f.setScorePenAway(d.scorePenAway());
        return f;
    }

    static Standing toStanding(StandingDto d) {
        Standing s = new Standing();
        s.setLeagueId(d.leagueId());
        s.setSeasonYear(d.seasonYear());
        s.setTeamId(d.teamId());
        s.setRank(d.rank());
        s.setGroupName(d.groupName());
        s.setPoints(d.points());
        s.setGoalsDiff(d.goalsDiff());
        s.setForm(d.form());
        s.setStatus(d.status());
        s.setDescription(d.description());
        s.setPlayedAll(d.playedAll());
        s.setWinAll(d.winAll());
        s.setDrawAll(d.drawAll());
        s.setLoseAll(d.loseAll());
        s.setGoalsForAll(d.goalsForAll());
        s.setGoalsAgainstAll(d.goalsAgainstAll());
        s.setPlayedHome(d.playedHome());
        s.setWinHome(d.winHome());
        s.setDrawHome(d.drawHome());
        s.setLoseHome(d.loseHome());
        s.setGoalsForHome(d.goalsForHome());
        s.setGoalsAgainstHome(d.goalsAgainstHome());
        s.setPlayedAway(d.playedAway());
        s.setWinAway(d.winAway());
        s.setDrawAway(d.drawAway());
        s.setLoseAway(d.loseAway());
        s.setGoalsForAway(d.goalsForAway());
        s.setGoalsAgainstAway(d.goalsAgainstAway());
        return s;
    }

    /** id-ul e surogat (IDENTITY) — nu se seteaza; se genereaza la insert. */
    static FixtureEvent toFixtureEvent(FixtureEventDto d) {
        FixtureEvent e = new FixtureEvent();
        e.setFixtureId(d.fixtureId());
        e.setTeamId(d.teamId());
        e.setPlayerId(d.playerId());
        e.setAssistId(d.assistId());
        e.setTimeElapsed(d.timeElapsed());
        e.setTimeExtra(d.timeExtra());
        e.setType(d.type());
        e.setDetail(d.detail());
        e.setComments(d.comments());
        return e;
    }
}
