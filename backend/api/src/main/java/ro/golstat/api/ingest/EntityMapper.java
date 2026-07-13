package ro.golstat.api.ingest;

import ro.golstat.api.entity.Coach;
import ro.golstat.api.entity.Fixture;
import ro.golstat.api.entity.FixtureEvent;
import ro.golstat.api.entity.FixtureLineup;
import ro.golstat.api.entity.FixtureLineupPlayer;
import ro.golstat.api.entity.FixturePlayerStats;
import ro.golstat.api.entity.FixtureTeamStats;
import ro.golstat.api.entity.Injury;
import ro.golstat.api.entity.League;
import ro.golstat.api.entity.Player;
import ro.golstat.api.entity.PlayerSeasonStats;
import ro.golstat.api.entity.Season;
import ro.golstat.api.entity.Standing;
import ro.golstat.api.entity.Team;
import ro.golstat.api.entity.TeamSeasonStats;
import ro.golstat.api.entity.Venue;
import ro.golstat.common.dto.CoachDto;
import ro.golstat.common.dto.FixtureDto;
import ro.golstat.common.dto.FixtureEventDto;
import ro.golstat.common.dto.FixtureLineupDto;
import ro.golstat.common.dto.FixtureLineupPlayerDto;
import ro.golstat.common.dto.FixturePlayerStatsDto;
import ro.golstat.common.dto.FixtureTeamStatsDto;
import ro.golstat.common.dto.InjuryDto;
import ro.golstat.common.dto.LeagueDto;
import ro.golstat.common.dto.PlayerDto;
import ro.golstat.common.dto.PlayerSeasonStatsDto;
import ro.golstat.common.dto.SeasonDto;
import ro.golstat.common.dto.StandingDto;
import ro.golstat.common.dto.TeamDto;
import ro.golstat.common.dto.TeamSeasonStatsDto;
import ro.golstat.common.dto.VenueDto;

/** Mapare DTO (din Kafka) → entitate JPA. Pura, fara efecte secundare. */
final class EntityMapper {

    private EntityMapper() {
    }

    static League toLeague(LeagueDto d) {
        League l = new League();
        l.setId(d.id());
        l.setName(d.name());
        l.setType(d.type());
        l.setLogo(d.logo());
        l.setCountryName(d.countryName());
        return l;
    }

    static Season toSeason(SeasonDto d) {
        Season s = new Season();
        s.setLeagueId(d.leagueId());
        s.setYear(d.year());
        s.setStartDate(d.startDate());
        s.setEndDate(d.endDate());
        s.setIsCurrent(d.isCurrent());
        s.setHasEvents(d.hasEvents());
        s.setHasLineups(d.hasLineups());
        s.setHasStatisticsFixtures(d.hasStatisticsFixtures());
        s.setHasStatisticsPlayers(d.hasStatisticsPlayers());
        s.setHasStandings(d.hasStandings());
        return s;
    }

    static Venue toVenue(VenueDto d) {
        Venue v = new Venue();
        v.setId(d.id());
        v.setName(d.name());
        v.setAddress(d.address());
        v.setCity(d.city());
        v.setCountryName(d.countryName());
        v.setCapacity(d.capacity());
        v.setSurface(d.surface());
        v.setImage(d.image());
        return v;
    }

    static Team toTeam(TeamDto d) {
        Team t = new Team();
        t.setId(d.id());
        t.setName(d.name());
        t.setCode(d.code());
        t.setCountryName(d.countryName());
        t.setFounded(d.founded());
        t.setIsNational(d.isNational() != null ? d.isNational() : false);   // is_national e NOT NULL in schema
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

    static FixtureLineup toFixtureLineup(FixtureLineupDto d) {
        FixtureLineup l = new FixtureLineup();
        l.setFixtureId(d.fixtureId());
        l.setTeamId(d.teamId());
        l.setFormation(d.formation());
        l.setCoachId(d.coachId());
        return l;
    }

    static FixtureLineupPlayer toFixtureLineupPlayer(FixtureLineupPlayerDto d) {
        FixtureLineupPlayer p = new FixtureLineupPlayer();
        p.setFixtureId(d.fixtureId());
        p.setTeamId(d.teamId());
        p.setPlayerId(d.playerId());
        p.setPlayerName(d.playerName());
        p.setNumber(d.number());
        p.setPosition(d.position());
        p.setGrid(d.grid());
        p.setIsSubstitute(d.isSubstitute() != null ? d.isSubstitute() : false);   // NOT NULL in schema
        return p;
    }

    /** id-ul e surogat (IDENTITY) — nu se seteaza; se genereaza la insert. */
    static Injury toInjury(InjuryDto d) {
        Injury i = new Injury();
        i.setPlayerId(d.playerId());
        i.setTeamId(d.teamId());
        i.setFixtureId(d.fixtureId());
        i.setLeagueId(d.leagueId());
        i.setSeasonYear(d.seasonYear());
        i.setType(d.type());
        i.setReason(d.reason());
        i.setReportedAt(d.reportedAt());
        return i;
    }

    static Player toPlayer(PlayerDto d) {
        Player p = new Player();
        p.setId(d.id());
        p.setName(d.name());
        p.setFirstname(d.firstname());
        p.setLastname(d.lastname());
        p.setAge(d.age());
        p.setBirthDate(d.birthDate());
        p.setBirthPlace(d.birthPlace());
        p.setBirthCountry(d.birthCountry());
        p.setNationality(d.nationality());
        p.setHeight(d.height());
        p.setWeight(d.weight());
        p.setIsInjured(d.isInjured());
        p.setPhoto(d.photo());
        return p;
    }

    static Coach toCoach(CoachDto d) {
        Coach c = new Coach();
        c.setId(d.id());
        c.setName(d.name());
        c.setFirstname(d.firstname());
        c.setLastname(d.lastname());
        c.setAge(d.age());
        c.setNationality(d.nationality());
        c.setPhoto(d.photo());
        return c;
    }

    static TeamSeasonStats toTeamSeasonStats(TeamSeasonStatsDto d) {
        TeamSeasonStats s = new TeamSeasonStats();
        s.setTeamId(d.teamId());
        s.setLeagueId(d.leagueId());
        s.setSeasonYear(d.seasonYear());
        s.setForm(d.form());
        s.setPlayedHome(d.playedHome());
        s.setPlayedAway(d.playedAway());
        s.setPlayedTotal(d.playedTotal());
        s.setWinsHome(d.winsHome());
        s.setWinsAway(d.winsAway());
        s.setWinsTotal(d.winsTotal());
        s.setDrawsHome(d.drawsHome());
        s.setDrawsAway(d.drawsAway());
        s.setDrawsTotal(d.drawsTotal());
        s.setLosesHome(d.losesHome());
        s.setLosesAway(d.losesAway());
        s.setLosesTotal(d.losesTotal());
        s.setGoalsForHome(d.goalsForHome());
        s.setGoalsForAway(d.goalsForAway());
        s.setGoalsForTotal(d.goalsForTotal());
        s.setGoalsForAvgHome(d.goalsForAvgHome());
        s.setGoalsForAvgAway(d.goalsForAvgAway());
        s.setGoalsForAvgTotal(d.goalsForAvgTotal());
        s.setGoalsAgainstHome(d.goalsAgainstHome());
        s.setGoalsAgainstAway(d.goalsAgainstAway());
        s.setGoalsAgainstTotal(d.goalsAgainstTotal());
        s.setGoalsAgainstAvgHome(d.goalsAgainstAvgHome());
        s.setGoalsAgainstAvgAway(d.goalsAgainstAvgAway());
        s.setGoalsAgainstAvgTotal(d.goalsAgainstAvgTotal());
        s.setCleanSheetHome(d.cleanSheetHome());
        s.setCleanSheetAway(d.cleanSheetAway());
        s.setCleanSheetTotal(d.cleanSheetTotal());
        s.setFailedToScoreHome(d.failedToScoreHome());
        s.setFailedToScoreAway(d.failedToScoreAway());
        s.setFailedToScoreTotal(d.failedToScoreTotal());
        s.setYellowCardsTotal(d.yellowCardsTotal());
        s.setRedCardsTotal(d.redCardsTotal());
        return s;
    }

    static PlayerSeasonStats toPlayerSeasonStats(PlayerSeasonStatsDto d) {
        PlayerSeasonStats s = new PlayerSeasonStats();
        s.setPlayerId(d.playerId());
        s.setTeamId(d.teamId());
        s.setLeagueId(d.leagueId());
        s.setSeasonYear(d.seasonYear());
        s.setPosition(d.position());
        s.setAppearances(d.appearances());
        s.setLineups(d.lineups());
        s.setMinutes(d.minutes());
        s.setRating(d.rating());
        s.setCaptain(d.captain());
        s.setGoalsTotal(d.goalsTotal());
        s.setGoalsConceded(d.goalsConceded());
        s.setGoalsAssists(d.goalsAssists());
        s.setGoalsSaves(d.goalsSaves());
        s.setShotsTotal(d.shotsTotal());
        s.setShotsOn(d.shotsOn());
        s.setPassesTotal(d.passesTotal());
        s.setPassesKey(d.passesKey());
        s.setPassesAccuracy(d.passesAccuracy());
        s.setTacklesTotal(d.tacklesTotal());
        s.setTacklesBlocks(d.tacklesBlocks());
        s.setTacklesIntercep(d.tacklesIntercep());
        s.setDuelsTotal(d.duelsTotal());
        s.setDuelsWon(d.duelsWon());
        s.setDribblesAttempts(d.dribblesAttempts());
        s.setDribblesSuccess(d.dribblesSuccess());
        s.setFoulsDrawn(d.foulsDrawn());
        s.setFoulsCommitted(d.foulsCommitted());
        s.setCardsYellow(d.cardsYellow());
        s.setCardsYellowred(d.cardsYellowred());
        s.setCardsRed(d.cardsRed());
        s.setPenaltyWon(d.penaltyWon());
        s.setPenaltyCommitted(d.penaltyCommitted());
        s.setPenaltyScored(d.penaltyScored());
        s.setPenaltyMissed(d.penaltyMissed());
        s.setPenaltySaved(d.penaltySaved());
        return s;
    }

    /** {@code playerName} nu se mapeaza: n-are coloana, serveste doar la placeholder-ul de jucator. */
    static FixturePlayerStats toFixturePlayerStats(FixturePlayerStatsDto d) {
        FixturePlayerStats s = new FixturePlayerStats();
        s.setFixtureId(d.fixtureId());
        s.setPlayerId(d.playerId());
        s.setTeamId(d.teamId());
        s.setMinutes(d.minutes());
        s.setRating(d.rating());
        s.setCaptain(d.captain());
        s.setSubstitute(d.substitute());
        s.setPosition(d.position());
        s.setShotsTotal(d.shotsTotal());
        s.setShotsOn(d.shotsOn());
        s.setGoalsTotal(d.goalsTotal());
        s.setGoalsConceded(d.goalsConceded());
        s.setGoalsAssists(d.goalsAssists());
        s.setGoalsSaves(d.goalsSaves());
        s.setPassesTotal(d.passesTotal());
        s.setPassesKey(d.passesKey());
        s.setPassesAccuracy(d.passesAccuracy());
        s.setTacklesTotal(d.tacklesTotal());
        s.setTacklesBlocks(d.tacklesBlocks());
        s.setTacklesIntercep(d.tacklesIntercep());
        s.setDuelsTotal(d.duelsTotal());
        s.setDuelsWon(d.duelsWon());
        s.setDribblesAttempts(d.dribblesAttempts());
        s.setDribblesSuccess(d.dribblesSuccess());
        s.setFoulsDrawn(d.foulsDrawn());
        s.setFoulsCommitted(d.foulsCommitted());
        s.setCardsYellow(d.cardsYellow());
        s.setCardsRed(d.cardsRed());
        s.setPenaltyWon(d.penaltyWon());
        s.setPenaltyCommitted(d.penaltyCommitted());
        s.setPenaltyScored(d.penaltyScored());
        s.setPenaltyMissed(d.penaltyMissed());
        s.setPenaltySaved(d.penaltySaved());
        return s;
    }

    static FixtureTeamStats toFixtureTeamStats(FixtureTeamStatsDto d) {
        FixtureTeamStats s = new FixtureTeamStats();
        s.setFixtureId(d.fixtureId());
        s.setTeamId(d.teamId());
        s.setShotsOnGoal(d.shotsOnGoal());
        s.setShotsOffGoal(d.shotsOffGoal());
        s.setShotsTotal(d.shotsTotal());
        s.setShotsBlocked(d.shotsBlocked());
        s.setShotsInsidebox(d.shotsInsidebox());
        s.setShotsOutsidebox(d.shotsOutsidebox());
        s.setFouls(d.fouls());
        s.setCornerKicks(d.cornerKicks());
        s.setOffsides(d.offsides());
        s.setBallPossession(d.ballPossession());
        s.setYellowCards(d.yellowCards());
        s.setRedCards(d.redCards());
        s.setGoalkeeperSaves(d.goalkeeperSaves());
        s.setPassesTotal(d.passesTotal());
        s.setPassesAccurate(d.passesAccurate());
        s.setPassesPercentage(d.passesPercentage());
        s.setExpectedGoals(d.expectedGoals());
        return s;
    }
}
