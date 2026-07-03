package ro.golstat.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.golstat.api.entity.Season;

public interface SeasonRepository extends JpaRepository<Season, Season.Pk> {
}
