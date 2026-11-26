package ro.golstat.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.golstat.api.entity.TokenUtilizator;
import ro.golstat.api.entity.TokenUtilizator.Tip;

import java.time.Instant;
import java.util.Optional;

public interface TokenUtilizatorRepository extends JpaRepository<TokenUtilizator, Long> {

    Optional<TokenUtilizator> findByHashAndTip(String hash, Tip tip);

    /** Marcheaza tokenul folosit; 1 = valid si consumat acum, 0 = inexistent, expirat sau deja folosit. */
    @Modifying(flushAutomatically = true)
    @Query("""
            update TokenUtilizator t set t.folositLa = :acum
            where t.hash = :hash and t.tip = :tip and t.folositLa is null and t.expiraLa > :acum""")
    int consuma(@Param("hash") String hash, @Param("tip") Tip tip, @Param("acum") Instant acum);

    @Modifying(flushAutomatically = true)
    @Query("""
            update TokenUtilizator t set t.folositLa = :acum
            where t.utilizatorId = :utilizatorId and t.tip = :tip and t.folositLa is null""")
    int invalideazaActive(@Param("utilizatorId") long utilizatorId, @Param("tip") Tip tip, @Param("acum") Instant acum);

    @Modifying(clearAutomatically = true)
    @Query("delete from TokenUtilizator t where t.expiraLa < :prag")
    int stergeExpirateInainteDe(@Param("prag") Instant prag);

    /** Inainte de {@code UtilizatorRepository.stergeNeconfirmateInainteDe} (nu exista FK cu cascada). */
    @Modifying(clearAutomatically = true)
    @Query("""
            delete from TokenUtilizator t where t.utilizatorId in
            (select u.id from Utilizator u where u.emailConfirmatLa is null and u.creatLa < :prag)""")
    int stergeAleConturilorNeconfirmateInainteDe(@Param("prag") Instant prag);
}
