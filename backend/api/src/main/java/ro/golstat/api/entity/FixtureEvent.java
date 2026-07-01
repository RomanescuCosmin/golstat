package ro.golstat.api.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "fixture_event")
@Getter
@Setter
@NoArgsConstructor
public class FixtureEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long fixtureId;
    private Long teamId;
    private Long playerId;
    private Long assistId;
    private Integer timeElapsed;
    private Integer timeExtra;
    private String type;
    private String detail;
    private String comments;
}
