package ro.golstat.api.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.golstat.api.entity.Fixture;

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
}
