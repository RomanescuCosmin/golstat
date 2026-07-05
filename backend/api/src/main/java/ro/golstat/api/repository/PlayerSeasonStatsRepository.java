package ro.golstat.api.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.golstat.api.entity.PlayerSeasonStats;

import java.util.List;

public interface PlayerSeasonStatsRepository extends JpaRepository<PlayerSeasonStats, PlayerSeasonStats.Pk> {

    List<PlayerSeasonStats> findByTeamIdAndLeagueIdAndSeasonYear(Long teamId, Long leagueId, Integer seasonYear);

    /** Golgheterii unei ligi/sezon (doar cu goluri > 0, cei mai buni primii); limitat prin {@link Pageable}. */
    @Query("""
            select p from PlayerSeasonStats p
            where p.leagueId = :leagueId and p.seasonYear = :season and p.goalsTotal > 0
            order by p.goalsTotal desc
            """)
    List<PlayerSeasonStats> topGolgheteri(@Param("leagueId") Long leagueId, @Param("season") Integer season,
                                          Pageable pageable);

    /** Pasatorii unei ligi/sezon (doar cu pase decisive > 0, cei mai buni primii). */
    @Query("""
            select p from PlayerSeasonStats p
            where p.leagueId = :leagueId and p.seasonYear = :season and p.goalsAssists > 0
            order by p.goalsAssists desc
            """)
    List<PlayerSeasonStats> topPasatori(@Param("leagueId") Long leagueId, @Param("season") Integer season,
                                        Pageable pageable);

    /** Toate liniile de statistici ale unui jucator (toate echipele/ligile/sezoanele) — pentru profil. */
    List<PlayerSeasonStats> findByPlayerId(Long playerId);
}
