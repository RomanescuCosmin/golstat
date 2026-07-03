package ro.golstat.api.prediction;

import ro.golstat.api.entity.Fixture;
import ro.golstat.api.entity.Team;
import ro.golstat.api.prediction.PredictieMeciDto.EchipaDto;
import ro.golstat.api.prediction.PredictieMeciDto.LinieGolDto;
import ro.golstat.api.prediction.PredictieMeciDto.ProcentCota;
import ro.golstat.stats.match.MatchPrediction;
import ro.golstat.stats.odds.Odds;

import java.util.List;

/** Traduce {@link MatchPrediction} (motor) + meciul in DTO-ul de afisare (procent + cota). */
public final class PredictieMeciMapper {

    private PredictieMeciMapper() {
    }

    public static PredictieMeciDto toDto(Fixture f, MatchPrediction p, Team gazde, Team oaspeti) {
        List<LinieGolDto> linii = p.linii().stream()
                .map(ou -> new LinieGolDto(ou.line(), procentCota(ou.overRate()), procentCota(ou.underRate())))
                .toList();

        return new PredictieMeciDto(
                f.getId(), echipa(f.getHomeTeamId(), gazde), echipa(f.getAwayTeamId(), oaspeti), f.getKickoff(),
                round2(p.lambdaGazde()), round2(p.lambdaOaspeti()),
                procentCota(p.sansaGazde()), procentCota(p.sansaEgal()), procentCota(p.sansaOaspeti()),
                linii, procentCota(p.btts()),
                p.esantionGazde(), p.esantionOaspeti());
    }

    private static EchipaDto echipa(long id, Team team) {
        return team == null ? new EchipaDto(id, null, null) : new EchipaDto(id, team.getName(), team.getLogo());
    }

    private static ProcentCota procentCota(double rate) {
        double cota = rate > 0 ? round2(Odds.fromProbability(rate)) : 0.0;
        return new ProcentCota(round1(rate * 100.0), cota);
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
