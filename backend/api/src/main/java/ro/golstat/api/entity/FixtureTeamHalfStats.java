package ro.golstat.api.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Entity
@Table(name = "fixture_team_half_stats")
@IdClass(FixtureTeamHalfStats.Pk.class)
@Getter
@Setter
@NoArgsConstructor
public class FixtureTeamHalfStats {

    @Id
    private Long fixtureId;
    @Id
    private Long teamId;
    @Id
    private Integer half;

    private Integer cornerKicks;
    private Integer fouls;
    private Integer yellowCards;
    private Integer redCards;
    private Integer shotsTotal;
    private Integer shotsOnGoal;
    private Integer offsides;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Pk implements Serializable {
        private Long fixtureId;
        private Long teamId;
        private Integer half;
    }
}
