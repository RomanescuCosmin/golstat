package ro.golstat.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.golstat.api.entity.FixtureTeamHalfStats;
import ro.golstat.api.stats.CornereReprizeAverage;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface FixtureTeamHalfStatsRepository
        extends JpaRepository<FixtureTeamHalfStats, FixtureTeamHalfStats.Pk> {

    List<FixtureTeamHalfStats> findByFixtureIdIn(Collection<Long> fixtureIds);

    @Query("select distinct h.fixtureId from FixtureTeamHalfStats h where h.fixtureId in :fixtureIds")
    Set<Long> meciuriCuReprize(Collection<Long> fixtureIds);

    /** Media de cornere pe echipa, pe repriza si locatie, pe o liga/sezon (meciuri terminale inainte de {@code before}). */
    @Query("""
            select avg(case when h.teamId = f.homeTeamId and h.half = 1 then h.cornerKicks end) as repriza1Gazde,
                   avg(case when h.teamId = f.awayTeamId and h.half = 1 then h.cornerKicks end) as repriza1Oaspeti,
                   avg(case when h.teamId = f.homeTeamId and h.half = 2 then h.cornerKicks end) as repriza2Gazde,
                   avg(case when h.teamId = f.awayTeamId and h.half = 2 then h.cornerKicks end) as repriza2Oaspeti
            from FixtureTeamHalfStats h
            join Fixture f on f.id = h.fixtureId
            where f.leagueId = :leagueId
              and f.seasonYear = :season
              and f.statusShort in :terminal
              and f.kickoff < :before
            """)
    CornereReprizeAverage avgCornereReprize(@Param("leagueId") long leagueId,
                                            @Param("season") int season,
                                            @Param("terminal") Collection<String> terminal,
                                            @Param("before") OffsetDateTime before);
}
