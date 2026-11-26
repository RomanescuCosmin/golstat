package ro.golstat.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Token de unica folosinta trimis pe email. In baza sta doar SHA-256 al tokenului, niciodata tokenul. */
@Entity
@Table(name = "token_utilizator", indexes = @Index(name = "ix_token_utilizator_tip", columnList = "utilizator_id, tip"))
@Getter
@Setter
@NoArgsConstructor
public class TokenUtilizator {

    public enum Tip { CONFIRMARE_EMAIL, RESETARE_PAROLA }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long utilizatorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Tip tip;

    @Column(nullable = false, unique = true, length = 64)
    private String hash;

    @Column(nullable = false)
    private Instant expiraLa;

    private Instant folositLa;

    @Column(nullable = false)
    private Instant creatLa;
}
