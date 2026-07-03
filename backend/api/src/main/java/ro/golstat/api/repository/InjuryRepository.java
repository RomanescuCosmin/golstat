package ro.golstat.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.golstat.api.entity.Injury;

import java.util.Collection;
import java.util.List;

public interface InjuryRepository extends JpaRepository<Injury, Long> {

    List<Injury> findByLeagueIdAndSeasonYearAndTeamIdIn(Long leagueId, Integer seasonYear,
                                                        Collection<Long> teamIds);

    /** Bulk delete (executat imediat) — re-colectarea unei ligi inlocuieste tot setul. */
    @Modifying
    @Query("delete from Injury i where i.leagueId = :leagueId and i.seasonYear = :seasonYear")
    void deleteByLeagueIdAndSeasonYear(@Param("leagueId") Long leagueId, @Param("seasonYear") Integer seasonYear);
}
