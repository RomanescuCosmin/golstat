package ro.golstat.api.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "fixture")
@Getter
@Setter
@NoArgsConstructor
public class Fixture {

    @Id
    private Long id;
    private String referee;
    private String timezone;
    private OffsetDateTime kickoff;
    private Long leagueId;
    private Integer seasonYear;
    private String round;
    private Long venueId;

    private String statusLong;
    private String statusShort;
    private Integer statusElapsed;

    private Long homeTeamId;
    private Long awayTeamId;

    private Integer goalsHome;
    private Integer goalsAway;
    private Integer scoreHtHome;
    private Integer scoreHtAway;
    private Integer scoreFtHome;
    private Integer scoreFtAway;
    private Integer scoreEtHome;
    private Integer scoreEtAway;
    private Integer scorePenHome;
    private Integer scorePenAway;
}
