package ro.golstat.api.live;

import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ro.golstat.api.entity.Fixture;
import ro.golstat.api.entity.Team;
import ro.golstat.api.matchcenter.MatchCenterService;
import ro.golstat.api.matchcenter.MeciCentralDto;
import ro.golstat.api.prediction.PredictieMeciDto.EchipaDto;
import ro.golstat.api.repository.FixtureRepository;
import ro.golstat.api.repository.TeamRepository;
import ro.golstat.common.GolstatConstants.FixtureStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Meciurile in desfasurare acum + detaliul unui meci (Match Center), din DB. */
@RestController
@RequestMapping("/api/v1/meciuri")
@Validated
public class MeciuriLiveController {

    private final FixtureRepository fixtures;
    private final TeamRepository teams;
    private final MatchCenterService matchCenter;

    public MeciuriLiveController(FixtureRepository fixtures, TeamRepository teams,
                                 MatchCenterService matchCenter) {
        this.fixtures = fixtures;
        this.teams = teams;
        this.matchCenter = matchCenter;
    }

    @GetMapping("/live")
    public List<MeciLiveDto> live() {
        List<Fixture> found = fixtures.findLive(FixtureStatus.IN_PLAY);
        Map<Long, Team> echipe = teamsById(found);

        return found.stream()
                .map(f -> new MeciLiveDto(
                        f.getId(), f.getLeagueId(),
                        echipa(f.getHomeTeamId(), echipe), echipa(f.getAwayTeamId(), echipe),
                        f.getGoalsHome(), f.getGoalsAway(), f.getStatusShort(), f.getStatusElapsed()))
                .toList();
    }

    /** Toate meciurile unei ligi intr-o zi ({@code data=YYYY-MM-DD}), orice status, cronologic. */
    @GetMapping
    public List<MeciZiDto> meciuriZi(
            @RequestParam @Min(1) long leagueId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        OffsetDateTime from = data.atStartOfDay().atOffset(ZoneOffset.UTC);
        List<Fixture> found = fixtures.findByDay(leagueId, from, from.plusDays(1));
        Map<Long, Team> echipe = teamsById(found);

        return found.stream()
                .map(f -> new MeciZiDto(
                        f.getId(), f.getLeagueId(),
                        echipa(f.getHomeTeamId(), echipe), echipa(f.getAwayTeamId(), echipe),
                        f.getGoalsHome(), f.getGoalsAway(),
                        f.getStatusShort(), f.getStatusElapsed(),
                        FixtureStatus.IN_PLAY.contains(f.getStatusShort()),
                        FixtureStatus.TERMINAL.contains(f.getStatusShort()),
                        f.getKickoff()))
                .toList();
    }

    /** Detaliul unui meci (scor, statistici, cronologie); 404 daca nu exista. */
    @GetMapping("/{fixtureId}")
    public MeciCentralDto matchCenter(@PathVariable @Min(1) long fixtureId) {
        return matchCenter.matchCenter(fixtureId);
    }

    private Map<Long, Team> teamsById(List<Fixture> found) {
        List<Long> ids = found.stream()
                .flatMap(f -> Stream.of(f.getHomeTeamId(), f.getAwayTeamId()))
                .distinct()
                .toList();
        return teams.findAllById(ids).stream()
                .collect(Collectors.toMap(Team::getId, Function.identity()));
    }

    private static EchipaDto echipa(Long id, Map<Long, Team> echipe) {
        Team t = echipe.get(id);
        return new EchipaDto(id != null ? id : 0, t != null ? t.getName() : null, t != null ? t.getLogo() : null);
    }
}
