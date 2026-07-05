package ro.golstat.api.statistici;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.golstat.api.entity.League;
import ro.golstat.api.entity.Season;
import ro.golstat.api.repository.FixtureRepository;
import ro.golstat.api.repository.FixtureTeamStatsRepository;
import ro.golstat.api.repository.LeagueRepository;
import ro.golstat.api.repository.SeasonRepository;
import ro.golstat.api.stats.CountAverage;
import ro.golstat.api.stats.GoalAverage;
import ro.golstat.api.stats.LeagueAverageService;
import ro.golstat.api.stats.LeagueAverages;
import ro.golstat.common.GolstatConstants.FixtureStatus;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Clasamentul de tendinte pe ligi al paginii Statistici: pentru fiecare mare competitie europeana,
 * mediile pe meci (goluri, cornere, faulturi, cartonase) pe cel mai recent sezon cu date reale.
 */
@Service
@Transactional(readOnly = true)
public class StatisticiService {

    /** Marile competitii afisate (mari campionate + cupe europene); ordinea initiala inainte de sortare. */
    private static final List<Long> LIGI = List.of(
            39L, 140L, 135L, 78L, 61L, 88L, 94L, 144L, 203L, 283L, 197L, 113L, 103L, 119L,
            2L, 3L, 848L);

    private final LeagueRepository leagues;
    private final SeasonRepository seasons;
    private final FixtureRepository fixtures;
    private final FixtureTeamStatsRepository teamStats;
    private final LeagueAverageService goalAverages;

    public StatisticiService(LeagueRepository leagues, SeasonRepository seasons, FixtureRepository fixtures,
                             FixtureTeamStatsRepository teamStats, LeagueAverageService goalAverages) {
        this.leagues = leagues;
        this.seasons = seasons;
        this.fixtures = fixtures;
        this.teamStats = teamStats;
        this.goalAverages = goalAverages;
    }

    /** Ligile cu date reale, sortate descrescator dupa media de goluri pe meci. */
    public List<StatisticiLigaDto> ligi() {
        return LIGI.stream()
                .map(this::liga)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(StatisticiLigaDto::medieGoluri,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private StatisticiLigaDto liga(long leagueId) {
        Integer sezon = sezonCuDate(leagueId);
        if (sezon == null) {
            return null;
        }
        // doar ligi cu meciuri TERMINALE reale (altfel mediile sunt doar default-uri)
        GoalAverage brut = fixtures.avgGoals(leagueId, sezon, FixtureStatus.TERMINAL);
        if (brut == null || brut.getAvgGazde() == null) {
            return null;
        }
        League lg = leagues.findById(leagueId).orElse(null);
        LeagueAverages g = goalAverages.averages(leagueId, sezon);
        // Cornere/faulturi/cartonase DOAR din statistici reale colectate (fara fallback la default-uri):
        // daca liga nu are fixture_team_stats, coloanele raman null si UI-ul arata "—", nu cifre inventate.
        CountAverage cnt = teamStats.avgCounts(leagueId, sezon, FixtureStatus.TERMINAL);
        return new StatisticiLigaDto(
                leagueId, lg != null ? lg.getName() : null, lg != null ? lg.getCountryName() : null,
                lg != null ? lg.getLogo() : null, sezon,
                round1(g.mediaLigaGazde() + g.mediaLigaOaspeti()),
                perMeci(cnt != null ? cnt.getAvgCornere() : null),
                perMeci(cnt != null ? cnt.getAvgFaulturi() : null),
                perMeci(cnt != null ? cnt.getAvgCartonase() : null));
    }

    /** DB da media PE ECHIPA; totalul pe meci = dublul. Null → nu exista statistici reale. */
    private static Double perMeci(Double perEchipa) {
        return perEchipa != null ? round1(2 * perEchipa) : null;
    }

    /** Cel mai recent sezon al ligii cu meciuri jucate (dupa lista de sezoane). */
    private Integer sezonCuDate(long leagueId) {
        List<Integer> ani = seasons.findByLeagueIdOrderByYearDesc(leagueId).stream()
                .map(Season::getYear).filter(Objects::nonNull).toList();
        for (Integer an : ani) {
            GoalAverage brut = fixtures.avgGoals(leagueId, an, FixtureStatus.TERMINAL);
            if (brut != null && brut.getAvgGazde() != null) {
                return an;
            }
        }
        return null;
    }

    private static Double round1(Double v) {
        return v != null ? Math.round(v * 10.0) / 10.0 : null;
    }
}
