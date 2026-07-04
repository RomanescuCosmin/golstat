package ro.golstat.api.live;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.golstat.api.entity.Fixture;
import ro.golstat.api.entity.League;
import ro.golstat.api.entity.Team;
import ro.golstat.api.prediction.PredictieMeciDto.EchipaDto;
import ro.golstat.api.repository.FixtureRepository;
import ro.golstat.api.repository.LeagueRepository;
import ro.golstat.api.repository.TeamRepository;
import ro.golstat.common.GolstatConstants.FixtureStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Compune programul meciurilor viitoare cross-competitii: meciuri {@code NS} intr-o fereastra de zile,
 * grupate pe zi (UTC) apoi pe competitie, pastrand ordinea cronologica.
 */
@Service
@Transactional(readOnly = true)
public class ProgramService {

    private final FixtureRepository fixtures;
    private final TeamRepository teams;
    private final LeagueRepository leagues;

    public ProgramService(FixtureRepository fixtures, TeamRepository teams, LeagueRepository leagues) {
        this.fixtures = fixtures;
        this.teams = teams;
        this.leagues = leagues;
    }

    public ProgramDto program(int zile) {
        OffsetDateTime from = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime to = from.toLocalDate().plusDays(zile).atStartOfDay().atOffset(ZoneOffset.UTC);
        List<Fixture> found = fixtures.findUpcomingAll(FixtureStatus.NOT_STARTED, from, to);

        Map<Long, Team> echipe = teamsById(found);
        Map<Long, League> ligiById = leaguesById(found);

        // ziua (UTC) → liga → meciuri, in ordinea de kickoff deja sortata din query
        Map<LocalDate, Map<Long, List<Fixture>>> peZi = new LinkedHashMap<>();
        for (Fixture f : found) {
            LocalDate zi = f.getKickoff().atZoneSameInstant(ZoneOffset.UTC).toLocalDate();
            peZi.computeIfAbsent(zi, z -> new LinkedHashMap<>())
                    .computeIfAbsent(f.getLeagueId(), l -> new ArrayList<>())
                    .add(f);
        }

        List<ProgramDto.Zi> ziduri = peZi.entrySet().stream()
                .map(zi -> new ProgramDto.Zi(zi.getKey(), ligi(zi.getValue(), ligiById, echipe)))
                .toList();
        return new ProgramDto(ziduri);
    }

    /**
     * Meciurile unei zile (orice status) din TOATE competitiile colectate, grupate pe competitie.
     * Fara filtru de liga: colectorul e singura sursa de scope (doar Europa ajunge in DB), iar ligile
     * fara fixtures in zi (ex. placeholder-e din ingest) nu apar. Ligile cu meci in desfasurare urca primele.
     */
    public ProgramZiDto programZi(LocalDate zi) {
        OffsetDateTime from = zi.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime to = from.plusDays(1);
        List<Fixture> found = fixtures.findByDayAllLeagues(from, to);

        Map<Long, Team> echipe = teamsById(found);
        Map<Long, League> ligiById = leaguesById(found);

        // liga → meciuri, in ordinea de kickoff deja sortata din query
        Map<Long, List<Fixture>> peLiga = new LinkedHashMap<>();
        for (Fixture f : found) {
            peLiga.computeIfAbsent(f.getLeagueId(), l -> new ArrayList<>()).add(f);
        }

        List<ProgramZiDto.Liga> ligi = peLiga.entrySet().stream()
                .map(e -> {
                    League lg = e.getKey() != null ? ligiById.get(e.getKey()) : null;
                    List<ProgramZiDto.Meci> meciuri = e.getValue().stream()
                            .map(f -> meciZi(f, echipe)).toList();
                    return new ProgramZiDto.Liga(
                            e.getKey() != null ? e.getKey() : 0,
                            lg != null ? lg.getName() : null,
                            lg != null ? lg.getCountryName() : null,
                            lg != null ? lg.getLogo() : null,
                            meciuri);
                })
                // ligile cu cel putin un meci live urca in capul listei; restul isi pastreaza ordinea
                .sorted(Comparator.comparing((ProgramZiDto.Liga l) ->
                        l.meciuri().stream().noneMatch(ProgramZiDto.Meci::inDesfasurare)))
                .toList();

        return new ProgramZiDto(zi, ligi);
    }

    private ProgramZiDto.Meci meciZi(Fixture f, Map<Long, Team> echipe) {
        return new ProgramZiDto.Meci(
                f.getId(),
                f.getKickoff(),
                echipa(f.getHomeTeamId(), echipe),
                echipa(f.getAwayTeamId(), echipe),
                f.getGoalsHome(), f.getGoalsAway(),
                f.getStatusShort(),
                FixtureStatus.IN_PLAY.contains(f.getStatusShort()),
                FixtureStatus.TERMINAL.contains(f.getStatusShort()),
                f.getStatusElapsed());
    }

    private List<ProgramDto.Liga> ligi(Map<Long, List<Fixture>> peLiga, Map<Long, League> ligiById,
                                       Map<Long, Team> echipe) {
        return peLiga.entrySet().stream().map(e -> {
            League lg = e.getKey() != null ? ligiById.get(e.getKey()) : null;
            List<ProgramDto.Meci> meciuri = e.getValue().stream()
                    .map(f -> new ProgramDto.Meci(
                            f.getId(), f.getKickoff(),
                            echipa(f.getHomeTeamId(), echipe), echipa(f.getAwayTeamId(), echipe)))
                    .toList();
            return new ProgramDto.Liga(
                    e.getKey() != null ? e.getKey() : 0,
                    lg != null ? lg.getName() : null,
                    lg != null ? lg.getCountryName() : null,
                    lg != null ? lg.getLogo() : null,
                    meciuri);
        }).toList();
    }

    private Map<Long, Team> teamsById(List<Fixture> found) {
        List<Long> ids = found.stream()
                .flatMap(f -> Stream.of(f.getHomeTeamId(), f.getAwayTeamId()))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return teams.findAllById(ids).stream()
                .collect(Collectors.toMap(Team::getId, Function.identity()));
    }

    private Map<Long, League> leaguesById(List<Fixture> found) {
        List<Long> ids = found.stream()
                .map(Fixture::getLeagueId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return leagues.findAllById(ids).stream()
                .collect(Collectors.toMap(League::getId, Function.identity()));
    }

    private static EchipaDto echipa(Long id, Map<Long, Team> echipe) {
        Team t = echipe.get(id);
        return new EchipaDto(id != null ? id : 0, t != null ? t.getName() : null, t != null ? t.getLogo() : null);
    }
}
