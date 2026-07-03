package ro.golstat.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.golstat.api.entity.Country;

public interface CountryRepository extends JpaRepository<Country, String> {
}
