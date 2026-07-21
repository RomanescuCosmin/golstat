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

    /**
     * Cate cartonase au fost aratate intr-un meci, numarate din cronologie. Plasa de siguranta pentru
     * meciurile la care furnizorul n-a publicat statistici de echipa, dar A publicat evenimentele
     * (tipic la ligile cu acoperire partiala, ex. Liga I 2026): fara asta pagina zicea "fara date"
     * desi numarul real era deja in baza. Verificat pe 6920 de meciuri cu ambele surse: coincide cu
     * suma din fixture_team_stats la 6903 (99,75%).
     */
    @Query("select count(e) from FixtureEvent e where e.fixtureId = :fixtureId and e.type = :card")
    long countCards(@Param("fixtureId") long fixtureId, @Param("card") String card);

    /**
     * Minutele golurilor unei echipe pe o liga/sezon, doar din meciuri TERMINALE (pentru distributie).
     * Golurile din prelungirea reprizei a doua (minut 90 cu timeExtra > 0) primesc 91 ca sa cada in
     * intervalul "90+"; prelungirea primei reprize (45+X) ramane in intervalul minutului de baza.
     */
    @Query("""
            select case when e.timeElapsed >= 90 and coalesce(e.timeExtra, 0) > 0 then 91 else e.timeElapsed end
            from FixtureEvent e
            join Fixture f on f.id = e.fixtureId
            where e.teamId = :teamId
              and e.type = :goal
              and f.leagueId = :leagueId
              and f.seasonYear = :season
              and f.statusShort in :terminal
              and e.timeElapsed is not null
            """)
    List<Integer> goalMinutes(@Param("teamId") long teamId,
                              @Param("leagueId") long leagueId,
                              @Param("season") int season,
                              @Param("goal") String goal,
                              @Param("terminal") Collection<String> terminal);

    /**
     * Minutele golurilor PRIMITE de o echipa (goluri marcate de adversar in meciurile ei), pe liga/sezon,
     * doar din meciuri TERMINALE — pentru distributia goluri marcate vs primite.
     */
    @Query("""
            select e.timeElapsed + coalesce(e.timeExtra, 0) from FixtureEvent e
            join Fixture f on f.id = e.fixtureId
            where (f.homeTeamId = :teamId or f.awayTeamId = :teamId)
              and e.teamId <> :teamId
              and e.type = :goal
              and f.leagueId = :leagueId
              and f.seasonYear = :season
              and f.statusShort in :terminal
              and e.timeElapsed is not null
            """)
    List<Integer> concededMinutes(@Param("teamId") long teamId,
                                  @Param("leagueId") long leagueId,
                                  @Param("season") int season,
                                  @Param("goal") String goal,
                                  @Param("terminal") Collection<String> terminal);
}
