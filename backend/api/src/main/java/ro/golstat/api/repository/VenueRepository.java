package ro.golstat.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.golstat.api.entity.Venue;

public interface VenueRepository extends JpaRepository<Venue, Long> {
}
