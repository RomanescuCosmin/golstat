package ro.golstat.collector.provider.bzzoiro;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import ro.golstat.collector.provider.DataProvider;
import ro.golstat.collector.provider.bzzoiro.BzzoiroMapper.Echipe;
import ro.golstat.common.dto.FixtureDto;
import ro.golstat.common.dto.FixtureEventDto;
import ro.golstat.common.dto.FixtureLineupDto;
import ro.golstat.common.dto.FixtureLiveDto;
import ro.golstat.common.dto.FixtureTeamHalfStatsDto;
import ro.golstat.common.dto.FixtureTeamStatsDto;
import ro.golstat.common.dto.InjuryDto;
import ro.golstat.common.dto.LeagueDto;
import ro.golstat.common.dto.SeasonDto;
import ro.golstat.common.dto.StandingDto;
import ro.golstat.common.dto.TeamDto;
import ro.golstat.common.dto.VenueDto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Furnizor bzzoiro (gratuit, 7500 req/zi). Doar ligile din {@code golstat.bzzoiro.ligi} sunt vizibile,
 * sub id-ul de liga al aplicatiei; echipele/meciurile/jucatorii pastreaza id-urile bzzoiro (de aceea
 * datele lui stau intr-o schema separata de cea cu date API-Football).
 *
 * <p>Neacoperite inca: accidentari, live, stadioane (raman placeholder-ele create la ingest).
 */
@Component
@Profile("bzzoiro")
public class BzzoiroProvider implements DataProvider {

    private static final Logger log = LoggerFactory.getLogger(BzzoiroProvider.class);

    private final BzzoiroClient client;
    private final BzzoiroProperties props;
    private final Map<Long, JsonNode> ligiCache = new ConcurrentHashMap<>();
    private final Map<Long, Echipe> echipePeMeci = new ConcurrentHashMap<>();
    private final Map<Long, JsonNode> statsPeMeci = new ConcurrentHashMap<>();
    private volatile Map<Long, String> arbitri;

    public BzzoiroProvider(BzzoiroClient client, BzzoiroProperties props) {
        this.client = client;
        this.props = props;
    }

    @Override
    public List<FixtureDto> fixtures(long leagueId, int season, LocalDate from, LocalDate to) {
        Optional<Long> bz = idBzzoiro(leagueId);
        Optional<JsonNode> sezon = sezonCurent(leagueId).filter(s -> s.path("year").asInt() == season);
        if (bz.isEmpty() || sezon.isEmpty()) {
            log.warn("bzzoiro: liga {} sezon {} nu e configurata sau nu e sezonul curent la furnizor", leagueId, season);
            return List.of();
        }
        long seasonId = sezon.get().path("id").asLong();
        return client.getAll("/events/", Map.of("league_id", bz.get(),
                        "date_from", from.toString(), "date_to", to.toString())).stream()
                .filter(e -> e.path("season_id").asLong() == seasonId)
                .map(e -> {
                    FixtureDto f = BzzoiroMapper.toFixture(e, leagueId, season,
                            e.path("referee_id").isNumber() ? arbitri().get(e.get("referee_id").asLong()) : null);
                    echipePeMeci.put(f.id(), new Echipe(f.homeTeamId(), f.awayTeamId()));
                    return f;
                })
                .toList();
    }

    @Override
    public List<FixtureEventDto> fixtureEvents(long fixtureId) {
        Echipe echipe = echipe(fixtureId);
        if (echipe == null) {
            return List.of();
        }
        return BzzoiroMapper.toEvents(client.get("/events/" + fixtureId + "/incidents/", Map.of()),
                fixtureId, echipe);
    }

    @Override
    public List<FixtureTeamStatsDto> fixtureStatistics(long fixtureId) {
        Echipe echipe = echipe(fixtureId);
        if (echipe == null) {
            return List.of();
        }
        JsonNode body = client.get("/events/" + fixtureId + "/stats/", Map.of());
        statsPeMeci.put(fixtureId, body);
        return BzzoiroMapper.toTeamStats(body, fixtureId, echipe);
    }

    @Override
    public List<FixtureTeamHalfStatsDto> fixtureHalfStatistics(long fixtureId) {
        Echipe echipe = echipe(fixtureId);
        if (echipe == null) {
            return List.of();
        }
        // reprizele vin in acelasi raspuns ca statisticile de meci → refolosim, fara al doilea request
        JsonNode body = statsPeMeci.remove(fixtureId);
        if (body == null) {
            body = client.get("/events/" + fixtureId + "/stats/", Map.of());
        }
        return BzzoiroMapper.toHalfStats(body, fixtureId, echipe);
    }

    @Override
    public List<FixtureLineupDto> fixtureLineups(long fixtureId) {
        return BzzoiroMapper.toLineups(client.get("/events/" + fixtureId + "/lineups/", Map.of()), fixtureId);
    }

    @Override
    public List<InjuryDto> injuries(long leagueId, int season) {
        return List.of();
    }

    /**
     * Meciurile in desfasurare din ligile configurate: UN singur request ({@code /events/live/}) aduce
     * scorul si minutul tuturor. Lista nu are sezon, runda, stadion, arbitru — doar starea live; cele deja
     * terminate din lista le sarim, ca sa treaca pe ruta „tocmai terminat" (stare finala + completare).
     */
    @Override
    public List<FixtureLiveDto> liveFixtures() {
        Map<Long, Long> aplicatie = ligiInverse();
        List<FixtureLiveDto> out = new ArrayList<>();
        for (JsonNode e : client.get("/events/live/", Map.of()).path("events")) {
            if (!"inprogress".equals(e.path("status").asText())) {
                continue;
            }
            FixtureDto f = dinEveniment(e, aplicatie);
            if (f != null) {
                out.add(new FixtureLiveDto(f, List.of()));
            }
        }
        return out;
    }

    @Override
    public List<FixtureTeamStatsDto> liveFixtureStatistics(long fixtureId) {
        return fixtureStatistics(fixtureId);
    }

    /** Starea curenta a unor meciuri anume (folosit cand un meci iese din live: scor final + FT). */
    @Override
    public List<FixtureDto> fixturesByIds(Collection<Long> fixtureIds) {
        Map<Long, Long> aplicatie = ligiInverse();
        List<FixtureDto> out = new ArrayList<>();
        for (Long id : fixtureIds) {
            FixtureDto f = dinEveniment(client.get("/events/" + id + "/", Map.of()), aplicatie);
            if (f != null) {
                out.add(f);
            }
        }
        return out;
    }

    /** id liga bzzoiro → id liga in aplicatie. */
    private Map<Long, Long> ligiInverse() {
        Map<Long, Long> out = new HashMap<>();
        props.ligi().forEach((aplicatie, bz) -> out.put(bz, aplicatie));
        return out;
    }

    /** Un eveniment bzzoiro ca meci al aplicatiei; null daca liga nu e urmarita sau nu e sezonul curent. */
    private FixtureDto dinEveniment(JsonNode e, Map<Long, Long> aplicatie) {
        Long liga = aplicatie.get(e.path("league_id").asLong());
        if (liga == null) {
            return null;
        }
        Optional<JsonNode> sezon = sezonCurent(liga);
        // lista live nu trimite season_id: acolo ne bazam pe liga + meciul deja existent in DB
        if (sezon.isEmpty() || (e.has("season_id") && e.path("season_id").asLong() != sezon.get().path("id").asLong())) {
            return null;
        }
        FixtureDto f = BzzoiroMapper.toFixture(e, liga, sezon.get().path("year").asInt(),
                e.path("referee_id").isNumber() ? arbitri().get(e.get("referee_id").asLong()) : null);
        if (f.homeTeamId() != null && f.awayTeamId() != null) {
            echipePeMeci.put(f.id(), new Echipe(f.homeTeamId(), f.awayTeamId()));
        }
        return f;
    }

    @Override
    public List<StandingDto> standings(long leagueId, int season) {
        return idBzzoiro(leagueId)
                .map(bz -> BzzoiroMapper.toStandings(
                        client.get("/leagues/" + bz + "/standings/", Map.of()), leagueId, season))
                .orElse(List.of());
    }

    @Override
    public List<TeamDto> teams(long leagueId, int season) {
        return idBzzoiro(leagueId)
                .map(bz -> client.getAll("/teams/", Map.of("league_id", bz)).stream()
                        .map(BzzoiroMapper::toTeam)
                        .toList())
                .orElse(List.of());
    }

    @Override
    public List<LeagueDto> leagues() {
        return props.ligi().keySet().stream()
                .map(id -> liga(id).map(l -> BzzoiroMapper.toLeague(l, id)))
                .flatMap(Optional::stream)
                .toList();
    }

    @Override
    public List<SeasonDto> seasons(long leagueId) {
        return sezonCurent(leagueId).map(s -> List.of(BzzoiroMapper.toSeason(s, leagueId))).orElse(List.of());
    }

    @Override
    public List<VenueDto> venues() {
        return List.of();
    }

    /** Echipele meciului; null (nu se memoreaza) cand furnizorul nu le stie — altfel ar ajunge pe „echipa 0". */
    private Echipe echipe(long fixtureId) {
        Echipe cunoscute = echipePeMeci.get(fixtureId);
        if (cunoscute != null) {
            return cunoscute;
        }
        JsonNode e = client.get("/events/" + fixtureId + "/", Map.of());
        if (!e.path("home_team_id").isNumber() || !e.path("away_team_id").isNumber()) {
            log.warn("bzzoiro: meciul {} nu are echipe cunoscute; il sar", fixtureId);
            return null;
        }
        Echipe gasite = new Echipe(e.get("home_team_id").asLong(), e.get("away_team_id").asLong());
        echipePeMeci.put(fixtureId, gasite);
        return gasite;
    }

    /** Numele arbitrilor dupa id, incarcate o singura data (~10 requesturi pentru toata lista). */
    private Map<Long, String> arbitri() {
        Map<Long, String> a = arbitri;
        if (a == null) {
            a = new ConcurrentHashMap<>();
            for (JsonNode r : client.getAll("/referees/", Map.of())) {
                if (r.path("id").isNumber() && r.hasNonNull("name")) {
                    a.put(r.get("id").asLong(), r.get("name").asText());
                }
            }
            arbitri = a;
        }
        return a;
    }

    private Optional<Long> idBzzoiro(long leagueId) {
        return Optional.ofNullable(props.ligi().get(leagueId));
    }

    private Optional<JsonNode> liga(long leagueId) {
        return idBzzoiro(leagueId).map(bz ->
                ligiCache.computeIfAbsent(leagueId, id -> client.get("/leagues/" + bz + "/", Map.of())));
    }

    private Optional<JsonNode> sezonCurent(long leagueId) {
        return liga(leagueId).map(l -> l.path("current_season")).filter(JsonNode::isObject);
    }
}
