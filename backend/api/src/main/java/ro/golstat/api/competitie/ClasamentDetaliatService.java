package ro.golstat.api.competitie;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ro.golstat.api.competitie.ClasamentDetaliatDto.Grupa;
import ro.golstat.api.competitie.ClasamentDetaliatDto.MeciForma;
import ro.golstat.api.competitie.ClasamentDetaliatDto.Rand;
import ro.golstat.api.entity.Fixture;
import ro.golstat.api.entity.Standing;
import ro.golstat.api.entity.Team;
import ro.golstat.api.repository.FixtureRepository;
import ro.golstat.api.repository.StandingRepository;
import ro.golstat.api.repository.TeamRepository;
import ro.golstat.common.GolstatConstants;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Clasamentul oficial (exact cum il da furnizorul: ordine, puncte, zone, grupe) + forma pe ultimele
 * meciuri ale sezonului, cu adversar si scor, din meciurile terminate ale competitiei.
 */
@Service
public class ClasamentDetaliatService {

    static final int MECIURI_FORMA = 5;
    /** Forma din clasament = doar faza de clasament (la cupe fara calificari si eliminatorii). */
    private static final Pattern FAZA_ELIMINATORIE = Pattern.compile(
            "(?i)qualif|playoff|play-off|round of|final|knockout|preliminary");

    private final FixtureRepository fixtures;
    private final StandingRepository standings;
    private final TeamRepository teams;

    public ClasamentDetaliatService(FixtureRepository fixtures, StandingRepository standings, TeamRepository teams) {
        this.fixtures = fixtures;
        this.standings = standings;
        this.teams = teams;
    }

    public ClasamentDetaliatDto clasament(long leagueId, int sezon) {
        List<Standing> oficial = standings.findByLeagueIdAndSeasonYearOrderByRankAsc(leagueId, sezon);
        if (oficial.isEmpty()) {
            return new ClasamentDetaliatDto(List.of());
        }
        List<Fixture> meciuri = fixtures.findByLeagueIdAndSeasonYearAndStatusShortInOrderByKickoffDesc(
                leagueId, sezon, GolstatConstants.FixtureStatus.TERMINAL, Pageable.unpaged()).stream()
                .filter(f -> f.getRound() == null || !FAZA_ELIMINATORIE.matcher(f.getRound()).find())
                .toList();
        Map<Long, Team> echipe = teams.findAllById(Stream.concat(
                        oficial.stream().map(Standing::getTeamId),
                        meciuri.stream().flatMap(f -> Stream.of(f.getHomeTeamId(), f.getAwayTeamId())))
                        .filter(Objects::nonNull).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Team::getId, Function.identity()));
        Map<Long, List<MeciForma>> forma = forma(meciuri, echipe);

        Map<String, List<Rand>> grupe = new LinkedHashMap<>();
        oficial.stream()
                .filter(s -> s.getTeamId() != null)
                .sorted(Comparator.comparing((Standing s) -> s.getGroupName() == null ? "" : s.getGroupName())
                        .thenComparing(Standing::getRank, Comparator.nullsLast(Comparator.naturalOrder())))
                .forEach(s -> grupe.computeIfAbsent(s.getGroupName() == null ? "" : s.getGroupName(),
                        k -> new ArrayList<>()).add(rand(s, echipe, forma)));
        return new ClasamentDetaliatDto(grupe.entrySet().stream()
                .map(g -> new Grupa(g.getKey().isEmpty() ? null : g.getKey(), g.getValue()))
                .toList());
    }

    private static Rand rand(Standing s, Map<Long, Team> echipe, Map<Long, List<MeciForma>> forma) {
        Team t = echipe.get(s.getTeamId());
        return new Rand(s.getRank() != null ? s.getRank() : 0, s.getTeamId(),
                t != null ? t.getName() : null, t != null ? t.getLogo() : null,
                s.getPlayedAll(), s.getWinAll(), s.getDrawAll(), s.getLoseAll(),
                s.getGoalsForAll(), s.getGoalsAgainstAll(), s.getGoalsDiff(), s.getPoints(),
                s.getDescription(), forma.getOrDefault(s.getTeamId(), List.of()));
    }

    /** {@code meciuri} = cele mai recente primele; pastreaza primele {@link #MECIURI_FORMA} per echipa. */
    static Map<Long, List<MeciForma>> forma(List<Fixture> meciuri, Map<Long, Team> echipe) {
        Map<Long, List<MeciForma>> out = new HashMap<>();
        for (Fixture f : meciuri) {
            if (f.getGoalsHome() == null || f.getGoalsAway() == null
                    || f.getHomeTeamId() == null || f.getAwayTeamId() == null) {
                continue;
            }
            adauga(out, f, true, echipe);
            adauga(out, f, false, echipe);
        }
        return out;
    }

    private static void adauga(Map<Long, List<MeciForma>> out, Fixture f, boolean acasa, Map<Long, Team> echipe) {
        List<MeciForma> lista = out.computeIfAbsent(acasa ? f.getHomeTeamId() : f.getAwayTeamId(), k -> new ArrayList<>());
        if (lista.size() >= MECIURI_FORMA) {
            return;
        }
        int pentru = acasa ? f.getGoalsHome() : f.getGoalsAway();
        int contra = acasa ? f.getGoalsAway() : f.getGoalsHome();
        Team adversar = echipe.get(acasa ? f.getAwayTeamId() : f.getHomeTeamId());
        lista.add(new MeciForma(f.getId(), f.getKickoff() != null ? f.getKickoff().toLocalDate() : null,
                pentru > contra ? "V" : pentru == contra ? "E" : "I", acasa,
                adversar != null ? adversar.getName() : null, f.getGoalsHome(), f.getGoalsAway()));
    }
}
