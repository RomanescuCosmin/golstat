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
@Table(name = "fixture_player_stats")
@IdClass(FixturePlayerStats.Pk.class)
@Getter
@Setter
@NoArgsConstructor
public class FixturePlayerStats {

    @Id
    private Long fixtureId;
    @Id
    private Long playerId;

    private Long teamId;
    private Integer minutes;
    private BigDecimal rating;
    private Boolean captain;
    private Boolean substitute;
    private String position;

    private Integer shotsTotal;
    private Integer shotsOn;
    private Integer goalsTotal;
    private Integer goalsConceded;
    private Integer goalsAssists;
    private Integer goalsSaves;

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
        private Long fixtureId;
        private Long playerId;
    }
}
