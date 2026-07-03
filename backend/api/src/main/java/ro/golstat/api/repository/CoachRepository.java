package ro.golstat.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.golstat.api.entity.Coach;

public interface CoachRepository extends JpaRepository<Coach, Long> {
}
