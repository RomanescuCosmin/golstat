package ro.golstat.api.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.golstat.api.entity.Fixture;
import ro.golstat.api.stats.GoalAverage;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

public interface FixtureRepository extends JpaRepository<Fixture, Long> {

    /**
     * Ultimele meciuri TERMINALE ale unei echipe (acasa sau deplasare) dinaintea unei date,
     * cele mai recente primele. Numarul se limiteaza prin {@link Pageable}.
     */
    @Query("""
            select f from Fixture f
            where (f.homeTeamId = :teamId or f.awayTeamId = :teamId)
              and f.statusShort in :terminal
              and f.kickoff < :before
            order by f.kickoff desc
            """)
    List<Fixture> findRecentForTeam(@Param("teamId") long teamId,
                                    @Param("terminal") Collection<String> terminal,
                                    @Param("before") OffsetDateTime before,
                                    Pageable pageable);

    /**
     * Media golurilor marcate de gazde / de oaspeti pe o liga/sezon, doar din meciuri TERMINALE.
     * Preferam scorul la 90 min ({@code scoreFt}), altfel {@code goals} (aceeasi logica ca la mapare).
     * Fara meciuri terminale → ambele getter-e {@code null}.
     */
    @Query("""
            select avg(coalesce(f.scoreFtHome, f.goalsHome)) as avgGazde,
                   avg(coalesce(f.scoreFtAway, f.goalsAway)) as avgOaspeti
            from Fixture f
            where f.leagueId = :leagueId
              and f.seasonYear = :season
              and f.statusShort in :terminal
            """)
    GoalAverage avgGoals(@Param("leagueId") long leagueId,
                         @Param("season") int season,
                         @Param("terminal") Collection<String> terminal);

    /** Meciurile viitoare ({@code NS}) ale unei ligi intr-o fereastra, cele mai apropiate primele. */
    @Query("""
            select f from Fixture f
            where f.leagueId = :leagueId
              and f.statusShort = :status
              and f.kickoff >= :from
              and f.kickoff < :to
            order by f.kickoff asc
            """)
    List<Fixture> findUpcoming(@Param("leagueId") long leagueId,
                               @Param("status") String status,
                               @Param("from") OffsetDateTime from,
                               @Param("to") OffsetDateTime to);
}
