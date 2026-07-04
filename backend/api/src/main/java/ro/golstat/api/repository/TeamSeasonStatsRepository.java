package ro.golstat.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.golstat.api.entity.TeamSeasonStats;

import java.util.List;
import java.util.Optional;

public interface TeamSeasonStatsRepository extends JpaRepository<TeamSeasonStats, TeamSeasonStats.Pk> {

    List<TeamSeasonStats> findByTeamId(Long teamId);

    Optional<TeamSeasonStats> findByTeamIdAndLeagueIdAndSeasonYear(Long teamId, Long leagueId, Integer seasonYear);
}
