package ro.golstat.collector.live;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Config-ul buclei LIVE (din {@code golstat.live.*}). {@code enabled} o activeaza (implicit oprita —
 * doar pe API real). {@code pollMs} = ritmul de poll; fereastra {@code [before, after]} minute in
 * jurul kickoff-ului in care consideram un meci "live".
 */
@ConfigurationProperties(prefix = "golstat.live")
public record LiveProperties(boolean enabled, long pollMs, int windowBeforeMinutes, int windowAfterMinutes) {

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
    }
}
