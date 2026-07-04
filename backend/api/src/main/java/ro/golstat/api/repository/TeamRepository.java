package ro.golstat.api.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.golstat.api.entity.Team;

import java.util.List;

public interface TeamRepository extends JpaRepository<Team, Long> {

    /**
     * Cautare pe nume (infix, case-insensitive). Ordonare: cluburi inaintea nationalelor,
     * potriviri prefix inaintea celor infix, apoi alfabetic. {@code q} vine deja lowercased.
     */
    @Query("""
            select t from Team t
            where lower(t.name) like concat('%', :q, '%')
            order by
                case when t.isNational = true then 1 else 0 end asc,
                case when lower(t.name) like concat(:q, '%') then 0 else 1 end asc,
                t.name asc
            """)
    List<Team> search(@Param("q") String q, Pageable pageable);
}
