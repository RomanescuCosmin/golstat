package ro.golstat.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.golstat.api.entity.PlayerSeasonStats;

import java.util.List;

public interface PlayerSeasonStatsRepository extends JpaRepository<PlayerSeasonStats, PlayerSeasonStats.Pk> {

    List<PlayerSeasonStats> findByTeamIdAndLeagueIdAndSeasonYear(Long teamId, Long leagueId, Integer seasonYear);
}
