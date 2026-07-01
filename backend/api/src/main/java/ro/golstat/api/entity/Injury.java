package ro.golstat.api.entity;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "injury")
@Getter
@Setter
@NoArgsConstructor
public class Injury {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long playerId;
    private Long teamId;
    private Long fixtureId;
    private Long leagueId;
    private Integer seasonYear;
    private String type;
    private String reason;
    private LocalDate reportedAt;
}
