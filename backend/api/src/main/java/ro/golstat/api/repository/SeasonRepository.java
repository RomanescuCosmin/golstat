package ro.golstat.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.golstat.api.entity.Season;

import java.util.List;

public interface SeasonRepository extends JpaRepository<Season, Season.Pk> {

    /** Sezoanele unei ligi, cele mai recente primele — pentru selectorul de sezon al competitiei. */
    List<Season> findByLeagueIdOrderByYearDesc(Long leagueId);
}
