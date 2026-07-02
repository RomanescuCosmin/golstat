package ro.golstat.stats.form;

import org.junit.jupiter.api.Test;
import ro.golstat.stats.model.MatchSample;
import ro.golstat.stats.model.Venue;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MatchWindowTest {

    private static MatchSample match(LocalDate date, boolean home) {
        return new MatchSample(date, home, 0, 0, 0, 0, null);
    }

    @Test
    void lastN_returnsMostRecentFirst() {
        List<MatchSample> matches = List.of(
                match(LocalDate.of(2024, 5, 1), true),
                match(LocalDate.of(2024, 5, 15), false),
                match(LocalDate.of(2024, 5, 8), true),
                match(LocalDate.of(2024, 4, 20), false)
        );

        List<MatchSample> result = MatchWindow.lastN(matches, 2);

        assertEquals(2, result.size());
        assertEquals(LocalDate.of(2024, 5, 15), result.get(0).date());
        assertEquals(LocalDate.of(2024, 5, 8), result.get(1).date());
    }

    @Test
    void lastN_fewerThanN_returnsAll() {
        List<MatchSample> matches = List.of(
                match(LocalDate.of(2024, 5, 1), true),
                match(LocalDate.of(2024, 5, 8), true)
        );

        List<MatchSample> result = MatchWindow.lastN(matches, 7);

        assertEquals(2, result.size());
    }

    @Test
    void lastN_byVenue_filtersThenTakesRecent() {
        List<MatchSample> matches = List.of(
                match(LocalDate.of(2024, 5, 1), true),
                match(LocalDate.of(2024, 5, 15), false),
                match(LocalDate.of(2024, 5, 8), true),
                match(LocalDate.of(2024, 5, 20), true)
        );

        List<MatchSample> result = MatchWindow.lastN(matches, 2, Venue.HOME);

        assertEquals(2, result.size());
        assertEquals(LocalDate.of(2024, 5, 20), result.get(0).date());
        assertEquals(LocalDate.of(2024, 5, 8), result.get(1).date());
    }
}
