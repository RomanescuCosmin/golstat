package ro.golstat.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.golstat.api.entity.Utilizator;

import java.time.Instant;
import java.util.Optional;

public interface UtilizatorRepository extends JpaRepository<Utilizator, Long> {

    Optional<Utilizator> findByEmail(String email);

    @Modifying
    @Query("update Utilizator u set u.ultimaAutentificareLa = :la where u.id = :id")
    void marcheazaAutentificare(@Param("id") long id, @Param("la") Instant la);

    @Modifying(clearAutomatically = true)
    @Query("delete from Utilizator u where u.emailConfirmatLa is null and u.creatLa < :prag")
    int stergeNeconfirmateInainteDe(@Param("prag") Instant prag);
}
