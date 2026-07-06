package ro.golstat.api.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.golstat.api.entity.Player;

import java.util.List;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    /**
     * Cautare pe nume (infix, case-insensitive). Ordonare: potriviri prefix inaintea celor infix, apoi
     * alfabetic. {@code Pageable} obligatoriu (tabel mare). {@code q} vine deja lowercased.
     */
    @Query("""
            select p from Player p
            where lower(p.name) like concat('%', :q, '%')
            order by
                case when lower(p.name) like concat(:q, '%') then 0 else 1 end asc,
                p.name asc
            """)
    List<Player> search(@Param("q") String q, Pageable pageable);
}
