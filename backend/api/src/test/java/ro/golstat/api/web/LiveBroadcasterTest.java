package ro.golstat.api.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import ro.golstat.common.GolstatConstants;
import ro.golstat.common.dto.FixtureDto;

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class LiveBroadcasterTest {

    @Mock SimpMessagingTemplate messaging;
    @InjectMocks LiveBroadcaster broadcaster;

    private static FixtureDto fixture(String statusShort) {
        return new FixtureDto(100L, null, "UTC", OffsetDateTime.parse("2026-07-03T18:00:00Z"),
                39L, 2025, "R", null, "long", statusShort, 55,
                1L, 2L, 1, 0, 0, 0, null, null, null, null, null, null);
    }

    @Test
    void inPlay_broadcastsToFixtureTopic() {
        broadcaster.broadcast(fixture(GolstatConstants.FixtureStatus.SECOND_HALF));
        verify(messaging).convertAndSend(eq("/topic/live/100"), any(FixtureDto.class));
    }

    @Test
    void notStarted_doesNotBroadcast() {
        broadcaster.broadcast(fixture(GolstatConstants.FixtureStatus.NOT_STARTED));
        verifyNoInteractions(messaging);
    }

    @Test
    void finished_doesNotBroadcast() {
        broadcaster.broadcast(fixture(GolstatConstants.FixtureStatus.FINISHED));
        verifyNoInteractions(messaging);
    }
}
