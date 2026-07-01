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
@Table(name = "fixture_lineup_player")
@IdClass(FixtureLineupPlayer.Pk.class)
@Getter
@Setter
@NoArgsConstructor
public class FixtureLineupPlayer {

    @Id
    private Long fixtureId;
    @Id
    private Long playerId;

    private Long teamId;
    private String playerName;
    private Integer number;
    private String position;
    private String grid;
    private Boolean isSubstitute;

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
