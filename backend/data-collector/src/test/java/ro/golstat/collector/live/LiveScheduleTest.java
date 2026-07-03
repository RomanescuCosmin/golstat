package ro.golstat.collector.live;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LiveScheduleTest {

    private static final Instant NOW = Instant.parse("2026-07-03T18:00:00Z");
    private static final Duration BEFORE = Duration.ofHours(3);
    private static final Duration AFTER = Duration.ofMinutes(15);

    private static LiveSchedule with(String kickoff) {
        LiveSchedule s = new LiveSchedule();
        s.replaceForLeague(39L, List.of(OffsetDateTime.parse(kickoff)));
        return s;
    }

    @Test
    void kickoffRecentlyPast_isInWindow() {
        assertTrue(with("2026-07-03T17:30:00Z").anyKickoffWithin(NOW, BEFORE, AFTER)); // acum 30 min
    }

    @Test
    void kickoffAboutToStart_isInWindow() {
        assertTrue(with("2026-07-03T18:10:00Z").anyKickoffWithin(NOW, BEFORE, AFTER)); // peste 10 min
    }

    @Test
    void kickoffLongPast_isOutsideWindow() {
        assertFalse(with("2026-07-03T13:00:00Z").anyKickoffWithin(NOW, BEFORE, AFTER)); // acum 5h
    }

    @Test
    void kickoffFarFuture_isOutsideWindow() {
        assertFalse(with("2026-07-04T18:00:00Z").anyKickoffWithin(NOW, BEFORE, AFTER)); // maine
    }

    @Test
    void empty_isOutsideWindow() {
        assertFalse(new LiveSchedule().anyKickoffWithin(NOW, BEFORE, AFTER));
    }

    @Test
    void replaceForLeague_overwritesPrevious() {
        LiveSchedule s = with("2026-07-03T17:30:00Z");          // in fereastra
        s.replaceForLeague(39L, List.of(OffsetDateTime.parse("2026-07-04T18:00:00Z"))); // inlocuit cu maine
        assertFalse(s.anyKickoffWithin(NOW, BEFORE, AFTER));
    }
}
