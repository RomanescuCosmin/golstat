package ro.golstat.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.golstat.api.entity.FixtureTeamStats;
import ro.golstat.api.stats.CountAverage;
import ro.golstat.api.stats.RefereeCardAverage;

import java.util.Collection;
import java.util.List;

public interface FixtureTeamStatsRepository extends JpaRepository<FixtureTeamStats, FixtureTeamStats.Pk> {

    List<FixtureTeamStats> findByFixtureIdIn(Collection<Long> fixtureIds);

    /** Statisticile pe meci ale unei echipe intr-o liga/sezon (meciuri TERMINALE) — mediate in Java pentru barele de sezon. */
    @Query("""
            select s from FixtureTeamStats s
            join Fixture f on f.id = s.fixtureId
            where s.teamId = :teamId
              and f.leagueId = :leagueId
              and f.seasonYear = :season
              and f.statusShort in :terminal
            """)
    List<FixtureTeamStats> findForTeamSeason(@Param("teamId") long teamId,
                                             @Param("leagueId") long leagueId,
                                             @Param("season") int season,
                                             @Param("terminal") Collection<String> terminal);

    /**
     * Mediile PE ECHIPA de cornere/faulturi/cartonase/suturi pe o liga/sezon, doar din meciuri
     * TERMINALE cu statistici colectate. Rosiile lipsa langa galbene inseamna 0 (coalesce).
     * Fara randuri → toate getter-ele {@code null}.
     */
    @Query("""
            select avg(s.cornerKicks) as avgCornere,
                   avg(s.fouls) as avgFaulturi,
                   avg(coalesce(s.yellowCards, 0) + coalesce(s.redCards, 0)) as avgCartonase,
                   avg(s.shotsTotal) as avgSuturi,
                   avg(s.shotsOnGoal) as avgSuturiPePoarta
            from FixtureTeamStats s
            join Fixture f on f.id = s.fixtureId
            where f.leagueId = :leagueId
              and f.seasonYear = :season
              and f.statusShort in :terminal
            """)
    CountAverage avgCounts(@Param("leagueId") long leagueId,
                           @Param("season") int season,
                           @Param("terminal") Collection<String> terminal);

    /**
     * Istoricul de cartonase al unui arbitru, doar din meciuri TERMINALE cu statistici colectate.
     * Fiecare meci are 2 randuri (unul per echipa), deci totalul pe meci = 2 × media pe rand.
     * Arbitru fara meciuri → {@code getAvgCards()} {@code null} si {@code getMatches()} 0.
     */
    @Query("""
            select 2 * avg(coalesce(s.yellowCards, 0) + coalesce(s.redCards, 0)) as avgCards,
                   count(distinct s.fixtureId) as matches
            from FixtureTeamStats s
            join Fixture f on f.id = s.fixtureId
            where f.referee = :referee
              and f.statusShort in :terminal
            """)
    RefereeCardAverage refereeCardAverage(@Param("referee") String referee,
                                          @Param("terminal") Collection<String> terminal);
}
