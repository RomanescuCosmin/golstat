package ro.golstat.stats.model;

import java.time.LocalDate;

/**
 * Un meci trecut vazut din perspectiva unei echipe, redus la ce au nevoie ferestrele si
 * filtrele: data (ordonare), gazda/oaspete (split acasa/deplasare), rangul adversarului
 * (filtru pe calibrul adversarului). Implementat de {@code MatchSample} (goluri) si
 * {@code EventCountSample} (faulturi/cornere/cartonase), ca sa refolosim {@code MatchWindow}
 * si {@code ScheduleFilter} pe orice piata.
 */
public interface TeamMatch {
    LocalDate date();

    boolean home();

    Integer opponentRank();
}
