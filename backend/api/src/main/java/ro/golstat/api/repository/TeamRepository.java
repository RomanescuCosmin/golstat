package ro.golstat.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.golstat.api.entity.Team;

public interface TeamRepository extends JpaRepository<Team, Long> {
}
