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
@Table(name = "team_season_stats")
@IdClass(TeamSeasonStats.Pk.class)
@Getter
@Setter
@NoArgsConstructor
public class TeamSeasonStats {

    @Id
    private Long teamId;
    @Id
    private Long leagueId;
    @Id
    private Integer seasonYear;

    private String form;

    private Integer playedHome;
    private Integer playedAway;
    private Integer playedTotal;
    private Integer winsHome;
    private Integer winsAway;
    private Integer winsTotal;
    private Integer drawsHome;
    private Integer drawsAway;
    private Integer drawsTotal;
    private Integer losesHome;
    private Integer losesAway;
    private Integer losesTotal;

    private Integer goalsForHome;
    private Integer goalsForAway;
    private Integer goalsForTotal;
    private java.math.BigDecimal goalsForAvgHome;
    private java.math.BigDecimal goalsForAvgAway;
    private java.math.BigDecimal goalsForAvgTotal;
    private Integer goalsAgainstHome;
    private Integer goalsAgainstAway;
    private Integer goalsAgainstTotal;
    private java.math.BigDecimal goalsAgainstAvgHome;
    private java.math.BigDecimal goalsAgainstAvgAway;
    private java.math.BigDecimal goalsAgainstAvgTotal;

    private Integer cleanSheetHome;
    private Integer cleanSheetAway;
    private Integer cleanSheetTotal;
    private Integer failedToScoreHome;
    private Integer failedToScoreAway;
    private Integer failedToScoreTotal;

    private Integer yellowCardsTotal;
    private Integer redCardsTotal;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Pk implements Serializable {
        private Long teamId;
        private Long leagueId;
        private Integer seasonYear;
    }
}
