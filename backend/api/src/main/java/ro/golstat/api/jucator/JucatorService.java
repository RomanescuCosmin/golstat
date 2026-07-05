package ro.golstat.api.jucator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.golstat.api.entity.League;
import ro.golstat.api.entity.Player;
import ro.golstat.api.entity.PlayerSeasonStats;
import ro.golstat.api.entity.Team;
import ro.golstat.api.prediction.PredictieMeciDto.EchipaDto;
import ro.golstat.api.repository.LeagueRepository;
import ro.golstat.api.repository.PlayerRepository;
import ro.golstat.api.repository.PlayerSeasonStatsRepository;
import ro.golstat.api.repository.TeamRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Compune {@link PaginaJucatorDto}: identitate + o linie de statistici per (liga, sezon, echipa). */
@Service
@Transactional(readOnly = true)
public class JucatorService {

    private final PlayerRepository players;
    private final PlayerSeasonStatsRepository playerSeasonStats;
    private final LeagueRepository leagues;
    private final TeamRepository teams;

    public JucatorService(PlayerRepository players, PlayerSeasonStatsRepository playerSeasonStats,
                          LeagueRepository leagues, TeamRepository teams) {
        this.players = players;
        this.playerSeasonStats = playerSeasonStats;
        this.leagues = leagues;
        this.teams = teams;
    }

    public PaginaJucatorDto pagina(long playerId) {
        Player p = players.findById(playerId).orElseThrow(() -> new JucatorNotFoundException(playerId));

        List<PlayerSeasonStats> stats = playerSeasonStats.findByPlayerId(playerId).stream()
                .sorted(Comparator.comparing(PlayerSeasonStats::getSeasonYear,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        Map<Long, League> ligi = leagues.findAllById(
                        stats.stream().map(PlayerSeasonStats::getLeagueId).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(League::getId, Function.identity()));
        Map<Long, Team> echipe = teams.findAllById(
                        stats.stream().map(PlayerSeasonStats::getTeamId).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(Team::getId, Function.identity()));

        List<PaginaJucatorDto.Sezon> sezoane = stats.stream().map(s -> {
            League lg = s.getLeagueId() != null ? ligi.get(s.getLeagueId()) : null;
            Team t = s.getTeamId() != null ? echipe.get(s.getTeamId()) : null;
            return new PaginaJucatorDto.Sezon(
                    s.getLeagueId() != null ? s.getLeagueId() : 0,
                    lg != null ? lg.getName() : null, lg != null ? lg.getLogo() : null,
                    s.getSeasonYear(), echipa(s.getTeamId(), t),
                    s.getAppearances(), s.getMinutes(), s.getGoalsTotal(), s.getGoalsAssists(),
                    s.getCardsYellow(), s.getCardsRed(),
                    s.getRating() != null ? s.getRating().doubleValue() : null);
        }).toList();

        PlayerSeasonStats recent = stats.stream().findFirst().orElse(null);
        Team echipaCurenta = recent != null && recent.getTeamId() != null ? echipe.get(recent.getTeamId()) : null;

        return new PaginaJucatorDto(
                p.getId(), p.getName(), p.getPhoto(), p.getNationality(), p.getAge(),
                recent != null ? recent.getPosition() : null,
                recent != null ? echipa(recent.getTeamId(), echipaCurenta) : null,
                sezoane);
    }

    private static EchipaDto echipa(Long id, Team t) {
        if (id == null) {
            return null;
        }
        return new EchipaDto(id, t != null ? t.getName() : null, t != null ? t.getLogo() : null);
    }
}
