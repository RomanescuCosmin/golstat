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
@Table(name = "standing")
@IdClass(Standing.Pk.class)
@Getter
@Setter
@NoArgsConstructor
public class Standing {

    @Id
    private Long leagueId;
    @Id
    private Integer seasonYear;
    @Id
    private Long teamId;

    private Integer rank;
    private String groupName;
    private Integer points;
    private Integer goalsDiff;
    private String form;
    private String status;
    private String description;

    private Integer playedAll;
    private Integer winAll;
    private Integer drawAll;
    private Integer loseAll;
    private Integer goalsForAll;
    private Integer goalsAgainstAll;

    private Integer playedHome;
    private Integer winHome;
    private Integer drawHome;
    private Integer loseHome;
    private Integer goalsForHome;
    private Integer goalsAgainstHome;

    private Integer playedAway;
    private Integer winAway;
    private Integer drawAway;
    private Integer loseAway;
    private Integer goalsForAway;
    private Integer goalsAgainstAway;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Pk implements Serializable {
        private Long leagueId;
        private Integer seasonYear;
        private Long teamId;
    }
}
