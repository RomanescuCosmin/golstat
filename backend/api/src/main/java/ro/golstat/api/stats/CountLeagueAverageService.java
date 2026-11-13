package ro.golstat.api.stats;

import org.springframework.stereotype.Service;
import ro.golstat.api.repository.FixtureTeamHalfStatsRepository;
import ro.golstat.api.repository.FixtureTeamStatsRepository;
import ro.golstat.common.GolstatConstants;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Mediile de cornere/faulturi/cartonase ale unei ligi/sezon, din meciurile TERMINALE cu statistici
 * colectate. DB-ul da media PE ECHIPA; modelele cer media TOTALULUI pe meci, deci dublam.
 * Fara date (sau liga/sezon necunoscute pe fixture) → fallback global.
 */
@Service
public class CountLeagueAverageService {

    private static final List<String> TERMINAL = List.of(
            GolstatConstants.FixtureStatus.FINISHED,
            GolstatConstants.FixtureStatus.FINISHED_AET,
            GolstatConstants.FixtureStatus.FINISHED_PEN
    );

    /** Totaluri tipice de fotbal pe meci (ambele echipe). */
    static final double DEFAULT_CORNERE = 10.0;
    static final double DEFAULT_FAULTURI = 24.0;
    static final double DEFAULT_CARTONASE = 4.0;
    static final double DEFAULT_SUTURI = 25.0;
    static final double DEFAULT_SUTURI_POARTA = 9.0;

    private final FixtureTeamStatsRepository teamStats;
    private final FixtureTeamHalfStatsRepository halfStats;

    public CountLeagueAverageService(FixtureTeamStatsRepository teamStats, FixtureTeamHalfStatsRepository halfStats) {
        this.teamStats = teamStats;
        this.halfStats = halfStats;
    }

    /** Mediile din meciurile jucate PANA ACUM (pagini de statistici „la zi"). */
    public CountLeagueAverages averages(Long leagueId, Integer season) {
        return averages(leagueId, season, OffsetDateTime.now());
    }

    /** Mediile din meciurile terminate inainte de {@code before} — la un meci, kickoff-ul lui (fara informatie din viitor). */
    public CountLeagueAverages averages(Long leagueId, Integer season, OffsetDateTime before) {
        if (leagueId == null || season == null) {
            return new CountLeagueAverages(DEFAULT_CORNERE, DEFAULT_FAULTURI, DEFAULT_CARTONASE,
                    DEFAULT_SUTURI, DEFAULT_SUTURI_POARTA);
        }
        CountAverage agg = teamStats.avgCounts(leagueId, season, TERMINAL, before);
        if (agg == null) {
            return averages(null, null, before);
        }
        double cornere = total(agg.getAvgCornere(), DEFAULT_CORNERE);
        double faulturi = total(agg.getAvgFaulturi(), DEFAULT_FAULTURI);
        double cartonase = total(agg.getAvgCartonase(), DEFAULT_CARTONASE);
        CountLeagueAverages.PeLocatie peLocatieCornere =
                peLocatie(agg.getAvgCornereGazde(), agg.getAvgCornereOaspeti(), cornere);
        CountLeagueAverages medii = new CountLeagueAverages(cornere, faulturi, cartonase,
                total(agg.getAvgSuturi(), DEFAULT_SUTURI),
                total(agg.getAvgSuturiPePoarta(), DEFAULT_SUTURI_POARTA),
                peLocatieCornere,
                peLocatie(agg.getAvgFaulturiGazde(), agg.getAvgFaulturiOaspeti(), faulturi),
                peLocatie(agg.getAvgCartonaseGazde(), agg.getAvgCartonaseOaspeti(), cartonase));
        CornereReprizeAverage reprize = halfStats.avgCornereReprize(leagueId, season, TERMINAL, before);
        if (reprize == null || reprize.getRepriza1Gazde() == null || reprize.getRepriza1Oaspeti() == null
                || reprize.getRepriza2Gazde() == null || reprize.getRepriza2Oaspeti() == null) {
            return medii;
        }
        return new CountLeagueAverages(medii.cornerePeMeci(), medii.faulturiPeMeci(), medii.cartonasePeMeci(),
                medii.suturiPeMeci(), medii.suturiPePoartaPeMeci(), medii.cornere(), medii.faulturi(),
                medii.cartonase(),
                new CountLeagueAverages.PeLocatie(reprize.getRepriza1Gazde(), reprize.getRepriza1Oaspeti()),
                new CountLeagueAverages.PeLocatie(reprize.getRepriza2Gazde(), reprize.getRepriza2Oaspeti()));
    }

    private static double total(Double perEchipa, double fallback) {
        return perEchipa != null ? 2 * perEchipa : fallback;
    }

    /** Impartirea gazde/oaspeti; fara una dintre ele (date incomplete) → jumatate din total fiecare. */
    private static CountLeagueAverages.PeLocatie peLocatie(Double gazde, Double oaspeti, double total) {
        return gazde != null && oaspeti != null
                ? new CountLeagueAverages.PeLocatie(gazde, oaspeti)
                : new CountLeagueAverages.PeLocatie(total / 2, total / 2);
    }
}
