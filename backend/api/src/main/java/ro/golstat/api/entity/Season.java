package ro.golstat.api.entity;

import java.io.Serializable;
import java.time.LocalDate;

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
@Table(name = "season")
@IdClass(Season.Pk.class)
@Getter
@Setter
@NoArgsConstructor
public class Season {

    @Id
    private Long leagueId;
    @Id
    private Integer year;

    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isCurrent;
    private Boolean hasEvents;
    private Boolean hasLineups;
    private Boolean hasStatisticsFixtures;
    private Boolean hasStatisticsPlayers;
    private Boolean hasStandings;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Pk implements Serializable {
        private Long leagueId;
        private Integer year;
    }
}
