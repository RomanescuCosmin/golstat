package ro.golstat.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.golstat.api.entity.Fixture;

public interface FixtureRepository extends JpaRepository<Fixture, Long> {
}
