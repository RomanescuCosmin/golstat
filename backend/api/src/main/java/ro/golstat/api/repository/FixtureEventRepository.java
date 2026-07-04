package ro.golstat.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.golstat.api.entity.FixtureEvent;

import java.util.Collection;
import java.util.List;

public interface FixtureEventRepository extends JpaRepository<FixtureEvent, Long> {

    /** Sterge toate evenimentele unui meci — folosit la re-colectare (replace idempotent). */
    long deleteByFixtureId(Long fixtureId);

    /** Cronologia unui meci ordonata dupa minut (coalesce 0), minut extra, apoi id (ordine stabila). */
    @Query("""
            select e from FixtureEvent e
            where e.fixtureId = :fixtureId
            order by coalesce(e.timeElapsed, 0) asc, coalesce(e.timeExtra, 0) asc, e.id asc
            """)
    List<FixtureEvent> findTimeline(@Param("fixtureId") long fixtureId);

    /** Minutele golurilor unei echipe pe o liga/sezon, doar din meciuri TERMINALE (pentru distributie). */
    @Query("""
            select e.timeElapsed from FixtureEvent e
            join Fixture f on f.id = e.fixtureId
            where e.teamId = :teamId
              and e.type = :goal
              and f.leagueId = :leagueId
              and f.seasonYear = :season
              and f.statusShort in :terminal
            """)
    List<Integer> goalMinutes(@Param("teamId") long teamId,
                              @Param("leagueId") long leagueId,
                              @Param("season") int season,
                              @Param("goal") String goal,
                              @Param("terminal") Collection<String> terminal);
}
