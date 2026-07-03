package ro.golstat.api.web;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import ro.golstat.common.GolstatConstants;
import ro.golstat.common.dto.FixtureDto;

import java.util.Set;

/**
 * Impinge actualizarile LIVE catre clientii WebSocket. Difuzeaza pe {@code /topic/live/{fixtureId}}
 * DOAR meciurile in desfasurare (repriza 1/pauza/repriza 2/prelungiri/penalty-uri) — un meci NS sau
 * terminat nu are de ce sa emita un push.
 */
@Component
public class LiveBroadcaster {

    private static final Set<String> IN_PLAY = Set.of(
            GolstatConstants.FixtureStatus.FIRST_HALF,
            GolstatConstants.FixtureStatus.HALF_TIME,
            GolstatConstants.FixtureStatus.SECOND_HALF,
            GolstatConstants.FixtureStatus.EXTRA_TIME,
            GolstatConstants.FixtureStatus.PENALTY
    );

    private final SimpMessagingTemplate messaging;

    public LiveBroadcaster(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    public void broadcast(FixtureDto fixture) {
        if (fixture.id() == null || !IN_PLAY.contains(fixture.statusShort())) {
            return;
        }
        messaging.convertAndSend("/topic/live/" + fixture.id(), fixture);
    }
}
