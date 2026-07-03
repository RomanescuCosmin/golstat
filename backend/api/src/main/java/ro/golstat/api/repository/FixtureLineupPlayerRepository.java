package ro.golstat.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.golstat.api.entity.FixtureLineupPlayer;

import java.util.List;

public interface FixtureLineupPlayerRepository extends JpaRepository<FixtureLineupPlayer, FixtureLineupPlayer.Pk> {

    List<FixtureLineupPlayer> findByFixtureId(Long fixtureId);

    /** Bulk delete (executat imediat), ca INSERT-urile replace-ului sa vina garantat DUPA. */
    @Modifying
    @Query("delete from FixtureLineupPlayer p where p.fixtureId = :fixtureId")
    void deleteByFixtureId(@Param("fixtureId") long fixtureId);
}
