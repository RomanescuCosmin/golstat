package ro.golstat.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.golstat.api.entity.League;

public interface LeagueRepository extends JpaRepository<League, Long> {
}
