package ro.golstat.collector.live;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Config-ul buclei LIVE (din {@code golstat.live.*}). {@code enabled} o activeaza (implicit oprita —
 * doar pe API real). {@code pollMs} = ritmul de poll; fereastra {@code [before, after]} minute in
 * jurul kickoff-ului in care consideram un meci "live". {@code statsEveryMs} = ritmul (throttled) de
 * refresh al statisticilor live; {@code statsLeagues} = allowlist de ligi pentru statistici (gol = toate
 * cele urmarite), ca sa tinem cota sub control cand sunt multe meciuri simultan.
 * {@code lineupsBeforeMinutes} = cu cat inainte de kickoff incepem sa cerem formatiile anuntate
 * (API-Football le publica de regula cu ~20-60 min inainte).
 */
@ConfigurationProperties(prefix = "golstat.live")
public record LiveProperties(boolean enabled, long pollMs, int windowBeforeMinutes, int windowAfterMinutes,
                             long statsEveryMs, List<Long> statsLeagues, int lineupsBeforeMinutes) {

    public LiveProperties {
        if (pollMs <= 0) {
            pollMs = 15000;
        }
        if (windowBeforeMinutes <= 0) {
            windowBeforeMinutes = 180;   // acopera un meci lung (repriza + prelungiri)
        }
        if (windowAfterMinutes <= 0) {
            windowAfterMinutes = 15;     // incepe polul putin inainte de fluier
        }
        if (statsEveryMs <= 0) {
            statsEveryMs = 120000;       // statisticile live la 2 min → cota controlata
        }
        if (statsLeagues == null) {
            statsLeagues = List.of();
        }
        if (lineupsBeforeMinutes <= 0) {
            lineupsBeforeMinutes = 70;   // anuntul vine de regula la T-60..T-20
        }
    }
}
