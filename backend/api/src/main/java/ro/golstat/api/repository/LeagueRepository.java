package ro.golstat.api.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.golstat.api.entity.League;

import java.util.List;

public interface LeagueRepository extends JpaRepository<League, Long> {

    /**
     * Cautare pe nume (infix, case-insensitive), doar ligile care AU meciuri colectate (catalogul complet
     * are mii de ligi goale — le sarim). Ordonare: potriviri prefix inaintea celor infix, apoi alfabetic.
     * {@code q} vine deja lowercased.
     */
    @Query("""
            select l from League l
            where lower(l.name) like concat('%', :q, '%')
              and exists (select 1 from Fixture f where f.leagueId = l.id)
            order by
                case when lower(l.name) like concat(:q, '%') then 0 else 1 end asc,
                l.name asc
            """)
    List<League> search(@Param("q") String q, Pageable pageable);
}
