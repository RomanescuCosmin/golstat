package ro.golstat.api.dev;

import jakarta.validation.constraints.Min;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ro.golstat.api.entity.Fixture;
import ro.golstat.api.matchcenter.MeciNotFoundException;
import ro.golstat.api.repository.FixtureRepository;
import ro.golstat.api.web.LiveBroadcaster;
import ro.golstat.common.GolstatConstants.FixtureStatus;
import ro.golstat.common.dto.FixtureDto;

/**
 * Simulator LIVE pentru dezvoltare/verificare: muta un meci prin statusuri in-play si difuzeaza
 * pe acelasi canal WebSocket ({@code /topic/live/{id}}) ca un meci real, ca sa se poata vedea
 * actualizarile in timp real fara o sursa de date live. Activat DOAR cand {@code golstat.dev.enabled=true}.
 */
@RestController
@RequestMapping("/api/v1/dev/live")
@Validated
@ConditionalOnProperty(name = "golstat.dev.enabled", havingValue = "true")
public class LiveSimController {

    private final FixtureRepository fixtures;
    private final LiveBroadcaster broadcaster;

    public LiveSimController(FixtureRepository fixtures, LiveBroadcaster broadcaster) {
        this.fixtures = fixtures;
        this.broadcaster = broadcaster;
    }

    /** Porneste "meciul": repriza 1, minutul 1, scor 0-0 daca nu era setat. */
    @PostMapping("/{fixtureId}/start")
    public FixtureDto start(@PathVariable @Min(1) long fixtureId) {
        Fixture f = load(fixtureId);
        f.setStatusShort(FixtureStatus.FIRST_HALF);
        f.setStatusLong("First Half");
        f.setStatusElapsed(1);
        if (f.getGoalsHome() == null) f.setGoalsHome(0);
        if (f.getGoalsAway() == null) f.setGoalsAway(0);
        return persistAndBroadcast(f);
    }

    /**
     * Avanseaza meciul. Fara parametri: creste minutul cu 1. Se pot suprascrie minutul, scorul si
     * statusul (ex. {@code status=HT} pentru pauza, {@code status=2H} pentru repriza 2).
     */
    @PostMapping("/{fixtureId}/tick")
    public FixtureDto tick(@PathVariable @Min(1) long fixtureId,
                           @RequestParam(required = false) Integer minut,
                           @RequestParam(required = false) Integer golGazde,
                           @RequestParam(required = false) Integer golOaspeti,
                           @RequestParam(required = false) String status) {
        Fixture f = load(fixtureId);
        f.setStatusElapsed(minut != null ? minut : (f.getStatusElapsed() == null ? 1 : f.getStatusElapsed() + 1));
        if (golGazde != null) f.setGoalsHome(golGazde);
        if (golOaspeti != null) f.setGoalsAway(golOaspeti);
        if (status != null) f.setStatusShort(status);
        return persistAndBroadcast(f);
    }

    /** Incheie meciul (FT); ultimul scor devine scorul final. */
    @PostMapping("/{fixtureId}/stop")
    public FixtureDto stop(@PathVariable @Min(1) long fixtureId) {
        Fixture f = load(fixtureId);
        f.setStatusShort(FixtureStatus.FINISHED);
        f.setStatusLong("Match Finished");
        return persistAndBroadcast(f);
    }

    private Fixture load(long fixtureId) {
        return fixtures.findById(fixtureId).orElseThrow(() -> new MeciNotFoundException(fixtureId));
    }

    private FixtureDto persistAndBroadcast(Fixture f) {
        fixtures.save(f);
        FixtureDto dto = toDto(f);
        broadcaster.broadcast(dto);
        return dto;
    }

    private static FixtureDto toDto(Fixture f) {
        return new FixtureDto(
                f.getId(), f.getReferee(), f.getTimezone(), f.getKickoff(), f.getLeagueId(), f.getSeasonYear(),
                f.getRound(), f.getVenueId(), f.getStatusLong(), f.getStatusShort(), f.getStatusElapsed(),
                f.getHomeTeamId(), f.getAwayTeamId(), f.getGoalsHome(), f.getGoalsAway(),
                f.getScoreHtHome(), f.getScoreHtAway(), f.getScoreFtHome(), f.getScoreFtAway(),
                f.getScoreEtHome(), f.getScoreEtAway(), f.getScorePenHome(), f.getScorePenAway());
    }
}
