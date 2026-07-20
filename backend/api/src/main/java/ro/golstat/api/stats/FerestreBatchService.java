package ro.golstat.api.stats;

import org.springframework.stereotype.Service;
import ro.golstat.api.entity.Fixture;
import ro.golstat.api.entity.FixtureTeamStats;
import ro.golstat.api.entity.Standing;
import ro.golstat.api.preview.StatisticiAvansateBuilder;
import ro.golstat.api.repository.FixtureRepository;
import ro.golstat.api.repository.FixtureTeamStatsRepository;
import ro.golstat.api.repository.StandingRepository;
import ro.golstat.common.GolstatConstants;
import ro.golstat.stats.model.MatchLocation;
import ro.golstat.stats.model.MatchSample;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Ferestrele de analiza ale AMBELOR echipe pentru o lista intreaga de meciuri, cu un numar de
 * query-uri CONSTANT (nu creste cu numarul de meciuri).
 *
 * <p>Calea per-meci ({@link MatchHistoryService} + {@link StatsHistoryService}) face ~6 query-uri
 * per meci plus un N+1 pe clasament — acceptabil la un meci, dar ~2000 de query-uri pe o fereastra
 * de 3 zile. Aici incarcam o singura data istoricul tuturor echipelor, statisticile lor si intreg
 * clasamentul, apoi taiem ferestrele in memorie. Rezultatul e IDENTIC cu al caii per-meci
 * (inclusiv rangul adversarului) — vezi testul de echivalenta.
 */
@Service
public class FerestreBatchService {

    /** Peste atatea id-uri intr-un {@code in (...)} unele drivere se sufoca; spargem in bucati. */
    private static final int BUCATA = 1000;

    private static final List<String> TERMINAL = List.of(
            GolstatConstants.FixtureStatus.FINISHED,
            GolstatConstants.FixtureStatus.FINISHED_AET,
            GolstatConstants.FixtureStatus.FINISHED_PEN
    );

    private final FixtureRepository fixtures;
    private final FixtureTeamStatsRepository teamStats;
    private final StandingRepository standings;

    public FerestreBatchService(FixtureRepository fixtures, FixtureTeamStatsRepository teamStats,
                                StandingRepository standings) {
        this.fixtures = fixtures;
        this.teamStats = teamStats;
        this.standings = standings;
    }

    /**
     * @param istoricFetch cate meciuri din istoric se iau per echipa inainte de filtrul de locatie
     * @param fereastra    marimea ferestrei finale (ultimele N pe locatie, respectiv generale)
     * @return {@code fixtureId → ferestrele celor doua echipe}; meciurile fara echipe sau fara
     * kickoff lipsesc din harta
     */
    public Map<Long, FerestreMeci> ferestre(List<Fixture> meciuri, int fereastra, int istoricFetch) {
        List<Fixture> valide = meciuri.stream()
                .filter(f -> f.getHomeTeamId() != null && f.getAwayTeamId() != null && f.getKickoff() != null)
                .toList();
        if (valide.isEmpty()) {
            return Map.of();
        }

        Set<Long> echipe = new HashSet<>();
        OffsetDateTime celMaiTarziu = valide.get(0).getKickoff();
        for (Fixture f : valide) {
            echipe.add(f.getHomeTeamId());
            echipe.add(f.getAwayTeamId());
            if (f.getKickoff().isAfter(celMaiTarziu)) {
                celMaiTarziu = f.getKickoff();
            }
        }

        List<Fixture> istoric = fixtures.findTerminalForTeams(echipe, TERMINAL, celMaiTarziu);
        Map<Long, List<Fixture>> istoricPerEchipa = grupeazaPerEchipa(istoric, echipe);
        Map<Long, Map<Long, FixtureTeamStats>> statsPerMeci = statistici(istoric);
        Map<String, Integer> ranguri = ranguri();

        Map<Long, FerestreMeci> out = new HashMap<>();
        for (Fixture f : valide) {
            IstoricEchipa gazde = istoricEchipa(f.getHomeTeamId(), f.getKickoff(),
                    istoricPerEchipa, statsPerMeci, ranguri, istoricFetch);
            IstoricEchipa oaspeti = istoricEchipa(f.getAwayTeamId(), f.getKickoff(),
                    istoricPerEchipa, statsPerMeci, ranguri, istoricFetch);
            out.put(f.getId(), new FerestreMeci(
                    StatisticiAvansateBuilder.ferestre(gazde.mostre(), gazde.counturi(),
                            MatchLocation.HOME, fereastra),
                    StatisticiAvansateBuilder.ferestre(oaspeti.mostre(), oaspeti.counturi(),
                            MatchLocation.AWAY, fereastra),
                    gazde.mostre(), oaspeti.mostre()));
        }
        return out;
    }

    /** Istoricul brut al unei echipe dinaintea unui kickoff, in ambele forme de care are nevoie motorul. */
    private record IstoricEchipa(List<MatchSample> mostre, IstoricCounturi counturi) {
    }

    private IstoricEchipa istoricEchipa(long teamId, OffsetDateTime kickoff,
                                        Map<Long, List<Fixture>> istoricPerEchipa,
                                        Map<Long, Map<Long, FixtureTeamStats>> statsPerMeci,
                                        Map<String, Integer> ranguri, int istoricFetch) {
        List<Fixture> aleEchipei = istoricPerEchipa.getOrDefault(teamId, List.of()).stream()
                .filter(f -> f.getKickoff() != null && f.getKickoff().isBefore(kickoff))
                .limit(istoricFetch)
                .toList();
        List<MatchSample> mostre = aleEchipei.stream()
                .map(f -> MatchSampleMapper.toSample(f, teamId, rangAdversar(f, teamId, ranguri)))
                .toList();
        return new IstoricEchipa(mostre, CountSampleMapper.istoric(teamId, aleEchipei, statsPerMeci));
    }

    /** Istoricul deja ordonat descrescator; fiecare meci intra la echipa lui (sau la ambele). */
    private static Map<Long, List<Fixture>> grupeazaPerEchipa(List<Fixture> istoric, Set<Long> echipe) {
        Map<Long, List<Fixture>> out = new HashMap<>();
        for (Fixture f : istoric) {
            adauga(out, f.getHomeTeamId(), f, echipe);
            adauga(out, f.getAwayTeamId(), f, echipe);
        }
        return out;
    }

    private static void adauga(Map<Long, List<Fixture>> out, Long teamId, Fixture f, Set<Long> echipe) {
        if (teamId != null && echipe.contains(teamId)) {
            out.computeIfAbsent(teamId, k -> new ArrayList<>()).add(f);
        }
    }

    private Map<Long, Map<Long, FixtureTeamStats>> statistici(List<Fixture> istoric) {
        List<Long> ids = istoric.stream().map(Fixture::getId).distinct().toList();
        Map<Long, Map<Long, FixtureTeamStats>> out = new HashMap<>();
        for (int i = 0; i < ids.size(); i += BUCATA) {
            List<Long> bucata = ids.subList(i, Math.min(i + BUCATA, ids.size()));
            teamStats.findByFixtureIdIn(bucata).stream()
                    .filter(s -> s.getFixtureId() != null && s.getTeamId() != null)
                    .forEach(s -> out.computeIfAbsent(s.getFixtureId(), k -> new HashMap<>())
                            .putIfAbsent(s.getTeamId(), s));
        }
        return out;
    }

    /** Tot clasamentul intr-o harta — inlocuieste N+1-ul per meci din {@link MatchHistoryService}. */
    private Map<String, Integer> ranguri() {
        return standings.findAll().stream()
                .filter(s -> s.getLeagueId() != null && s.getSeasonYear() != null
                        && s.getTeamId() != null && s.getRank() != null)
                .collect(Collectors.toMap(
                        s -> cheieRang(s.getLeagueId(), s.getSeasonYear(), s.getTeamId()),
                        Standing::getRank, (a, b) -> a));
    }

    private static Integer rangAdversar(Fixture fixture, long teamId, Map<String, Integer> ranguri) {
        boolean home = fixture.getHomeTeamId() != null && fixture.getHomeTeamId() == teamId;
        Long adversarId = home ? fixture.getAwayTeamId() : fixture.getHomeTeamId();
        if (adversarId == null || fixture.getLeagueId() == null || fixture.getSeasonYear() == null) {
            return null;
        }
        return ranguri.get(cheieRang(fixture.getLeagueId(), fixture.getSeasonYear(), adversarId));
    }

    private static String cheieRang(long leagueId, int season, long teamId) {
        return leagueId + ":" + season + ":" + teamId;
    }
}
