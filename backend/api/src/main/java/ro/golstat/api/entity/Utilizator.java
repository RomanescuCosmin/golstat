package ro.golstat.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "utilizator")
@Getter
@Setter
@NoArgsConstructor
public class Utilizator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Normalizat (trim + lowercase), vezi {@code NormalizatorEmail}. */
    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(nullable = false, length = 50)
    private String nume;

    @Column(nullable = false)
    private String parolaHash;

    private Instant emailConfirmatLa;

    @Column(nullable = false)
    private Instant creatLa;

    private Instant parolaSchimbataLa;
    private Instant ultimaAutentificareLa;

    @Version
    private long versiune;

    public boolean isEmailConfirmat() {
        return emailConfirmatLa != null;
    }
}
