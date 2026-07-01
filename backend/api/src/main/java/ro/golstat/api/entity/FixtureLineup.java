package ro.golstat.api.entity;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "fixture_lineup")
@IdClass(FixtureLineup.Pk.class)
@Getter
@Setter
@NoArgsConstructor
public class FixtureLineup {

    @Id
    private Long fixtureId;
    @Id
    private Long teamId;

    private String formation;
    private Long coachId;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Pk implements Serializable {
        private Long fixtureId;
        private Long teamId;
    }
}
