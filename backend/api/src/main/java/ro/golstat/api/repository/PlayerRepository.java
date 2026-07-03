package ro.golstat.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.golstat.api.entity.Player;

public interface PlayerRepository extends JpaRepository<Player, Long> {
}
