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
@Table(name = "player_season_stats")
@IdClass(PlayerSeasonStats.Pk.class)
@Getter
@Setter
@NoArgsConstructor
public class PlayerSeasonStats {

    @Id
    private Long playerId;
    @Id
    private Long teamId;
    @Id
    private Long leagueId;
    @Id
    private Integer seasonYear;

    private String position;
    private Integer appearances;
    private Integer lineups;
    private Integer minutes;
    private BigDecimal rating;
    private Boolean captain;

    private Integer goalsTotal;
    private Integer goalsConceded;
    private Integer goalsAssists;
    private Integer goalsSaves;

    private Integer shotsTotal;
    private Integer shotsOn;
    private Integer passesTotal;
    private Integer passesKey;
    private Integer passesAccuracy;

    private Integer tacklesTotal;
    private Integer tacklesBlocks;
    private Integer tacklesIntercep;
    private Integer duelsTotal;
    private Integer duelsWon;
    private Integer dribblesAttempts;
    private Integer dribblesSuccess;

    private Integer foulsDrawn;
    private Integer foulsCommitted;
    private Integer cardsYellow;
    private Integer cardsYellowred;
    private Integer cardsRed;

    private Integer penaltyWon;
    private Integer penaltyCommitted;
    private Integer penaltyScored;
    private Integer penaltyMissed;
    private Integer penaltySaved;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Pk implements Serializable {
        private Long playerId;
        private Long teamId;
        private Long leagueId;
        private Integer seasonYear;
    }
}
