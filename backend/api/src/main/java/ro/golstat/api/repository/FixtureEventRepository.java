package ro.golstat.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.golstat.api.entity.FixtureEvent;

public interface FixtureEventRepository extends JpaRepository<FixtureEvent, Long> {

    /** Sterge toate evenimentele unui meci — folosit la re-colectare (replace idempotent). */
    long deleteByFixtureId(Long fixtureId);
}
