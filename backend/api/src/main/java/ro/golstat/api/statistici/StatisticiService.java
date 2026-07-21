package ro.golstat.api.statistici;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.golstat.api.entity.League;
import ro.golstat.api.repository.FixtureRepository;
import ro.golstat.api.repository.FixtureTeamStatsRepository;
import ro.golstat.api.repository.LeagueRepository;
import ro.golstat.api.stats.CountAverage;
import ro.golstat.api.stats.GoalAverage;
import ro.golstat.api.stats.LeagueAverageService;
import ro.golstat.api.stats.LeagueAverages;
import ro.golstat.common.GolstatConstants.FixtureStatus;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Clasamentul de tendinte pe ligi al paginii Statistici: pentru fiecare mare competitie europeana,
 * mediile pe meci (goluri, cornere, faulturi, cartonase) pe cel mai recent sezon cu date reale.
 */
@Service
@Transactional(readOnly = true)
public class StatisticiService {

    /**
     * Amicalele n-au clasament si aduna mii de meciuri intre echipe din ligi diferite: mediile lor
     * nu spun nimic despre o "tendinta de competitie", deci le tinem in afara clasamentului.
     */
    private static final Set<Long> EXCLUSE = Set.of(667L, 10L, 666L);

    private final LeagueRepository leagues;
    private final FixtureRepository fixtures;
    private final FixtureTeamStatsRepository teamStats;
    private final LeagueAverageService goalAverages;

    public StatisticiService(LeagueRepository leagues, FixtureRepository fixtures,
                             FixtureTeamStatsRepository teamStats, LeagueAverageService goalAverages) {
        this.leagues = leagues;
        this.fixtures = fixtures;
        this.teamStats = teamStats;
        this.goalAverages = goalAverages;
    }

    /**
     * Ligile cu date reale, sortate descrescator dupa media de goluri pe meci.
     *
     * <p>Lista vine din DATE, nu dintr-o lista alba: orice competitie colectata apare aici automat.
     * Inainte era hardcodata, deci ligile aduse ulterior prin backfill (Serie B, 2. Bundesliga,
     * Segunda, Championship...) nu apareau niciodata, oricate date am fi avut.
     */
    public List<StatisticiLigaDto> ligi() {
        return fixtures.ligiCuMeciuriJucate(FixtureStatus.TERMINAL).stream()
                .filter(ls -> ls.getLeagueId() != null && ls.getSeasonYear() != null)
                .filter(ls -> !EXCLUSE.contains(ls.getLeagueId()))
                .map(ls -> liga(ls.getLeagueId(), ls.getSeasonYear()))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(StatisticiLigaDto::medieGoluri,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private StatisticiLigaDto liga(long leagueId, int sezon) {
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

    private static Double round1(Double v) {
        return v != null ? Math.round(v * 10.0) / 10.0 : null;
    }
}
