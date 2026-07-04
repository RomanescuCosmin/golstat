package ro.golstat.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.golstat.api.entity.Standing;

import java.util.List;
import java.util.Optional;

public interface StandingRepository extends JpaRepository<Standing, Standing.Pk> {

    Optional<Standing> findByLeagueIdAndSeasonYearAndTeamId(Long leagueId, Integer seasonYear, Long teamId);

    List<Standing> findByLeagueIdAndSeasonYearOrderByRankAsc(Long leagueId, Integer seasonYear);
}
