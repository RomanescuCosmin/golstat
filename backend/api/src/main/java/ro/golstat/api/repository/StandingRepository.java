package ro.golstat.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.golstat.api.entity.Standing;

public interface StandingRepository extends JpaRepository<Standing, Standing.Pk> {
}
