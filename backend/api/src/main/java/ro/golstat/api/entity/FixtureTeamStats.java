package ro.golstat.api.entity;

import java.io.Serializable;
import java.math.BigDecimal;

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
@Table(name = "fixture_team_stats")
@IdClass(FixtureTeamStats.Pk.class)
@Getter
@Setter
@NoArgsConstructor
public class FixtureTeamStats {

    @Id
    private Long fixtureId;
    @Id
    private Long teamId;

    private Integer shotsOnGoal;
    private Integer shotsOffGoal;
    private Integer shotsTotal;
    private Integer shotsBlocked;
    private Integer shotsInsidebox;
    private Integer shotsOutsidebox;
    private Integer fouls;
    private Integer cornerKicks;
    private Integer offsides;
    private BigDecimal ballPossession;
    private Integer yellowCards;
    private Integer redCards;
    private Integer goalkeeperSaves;
    private Integer passesTotal;
    private Integer passesAccurate;
    private BigDecimal passesPercentage;
    private BigDecimal expectedGoals;

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
