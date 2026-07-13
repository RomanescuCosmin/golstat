package ro.golstat.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.golstat.api.entity.FixturePlayerStats;

public interface FixturePlayerStatsRepository extends JpaRepository<FixturePlayerStats, FixturePlayerStats.Pk> {
}
