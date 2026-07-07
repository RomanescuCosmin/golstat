package ro.golstat.api.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.golstat.api.entity.FixtureLineup;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

public interface FixtureLineupRepository extends JpaRepository<FixtureLineup, FixtureLineup.Pk> {

    List<FixtureLineup> findByFixtureId(Long fixtureId);

    /** Antrenorii din cele mai recente formatii ale echipei (cel mai recent primul); primul e antrenorul curent. */
    @Query("""
            select l.coachId from FixtureLineup l
            join Fixture f on f.id = l.fixtureId
            where l.teamId = :teamId and l.coachId is not null
            order by f.kickoff desc
            """)
    List<Long> recentCoachIds(@Param("teamId") long teamId, Pageable pageable);

    /**
     * Cele mai recente formatii ale echipei din meciuri TERMINALE dinaintea unei date
     * (cea mai recenta prima) — sursa echipei PROBABILE cand lineup-ul meciului nu e anuntat.
     */
    @Query("""
            select l from FixtureLineup l
            join Fixture f on f.id = l.fixtureId
            where l.teamId = :teamId
              and f.statusShort in :terminal
              and f.kickoff < :before
            order by f.kickoff desc
            """)
    List<FixtureLineup> findRecentForTeam(@Param("teamId") long teamId,
                                          @Param("terminal") Collection<String> terminal,
                                          @Param("before") OffsetDateTime before,
                                          Pageable pageable);

    /** Bulk delete (executat imediat), ca INSERT-urile replace-ului sa vina garantat DUPA. */
    @Modifying
    @Query("delete from FixtureLineup l where l.fixtureId = :fixtureId")
    void deleteByFixtureId(@Param("fixtureId") long fixtureId);
}
