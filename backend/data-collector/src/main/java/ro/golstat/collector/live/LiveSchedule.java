package ro.golstat.collector.live;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Orarul in memorie al meciurilor urmarite (kickoff-urile per liga), populat de colectare. Serveste
 * bucla LIVE ca sa poleze DOAR cand exista un meci in fereastra de joc — altfel am arde request-uri
 * 24/7 degeaba. Colectorul n-are DB, deci starea asta e efemera (se reface la fiecare ciclu).
 */
@Component
public class LiveSchedule {

    private final Map<Long, List<Instant>> kickoffsByLeague = new ConcurrentHashMap<>();

    /** Inlocuieste kickoff-urile cunoscute pentru o liga (ultima colectare castiga). */
    public void replaceForLeague(long leagueId, List<OffsetDateTime> kickoffs) {
        kickoffsByLeague.put(leagueId, kickoffs.stream().map(OffsetDateTime::toInstant).toList());
    }

    /** Exista vreun meci cu kickoff in {@code [now - before, now + after]}? (adica in/langa fereastra de joc). */
    public boolean anyKickoffWithin(Instant now, Duration before, Duration after) {
        Instant lower = now.minus(before);
        Instant upper = now.plus(after);
        return kickoffsByLeague.values().stream()
                .flatMap(List::stream)
                .anyMatch(k -> !k.isBefore(lower) && !k.isAfter(upper));
    }
}
