package ro.golstat.api.cautare;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.golstat.api.repository.LeagueRepository;
import ro.golstat.api.repository.PlayerRepository;
import ro.golstat.api.repository.TeamRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Cautare globala pentru navbar: echipe + campionate + jucatori. Termen sub {@value #MIN_CARACTERE}
 * caractere → lista goala (fara hit DB). Rezultatele vin plate, ordonate ECHIPE → CAMPIONATE → JUCATORI;
 * frontend-ul le grupeaza dupa {@code tip}.
 */
@Service
@Transactional(readOnly = true)
public class CautareService {

    private static final int MIN_CARACTERE = 2;
    private static final int LIMITA_PER_TIP = 5;

    private final TeamRepository teams;
    private final LeagueRepository leagues;
    private final PlayerRepository players;

    public CautareService(TeamRepository teams, LeagueRepository leagues, PlayerRepository players) {
        this.teams = teams;
        this.leagues = leagues;
        this.players = players;
    }

    public List<RezultatCautareDto> cauta(String q) {
        String termen = q != null ? q.strip().toLowerCase() : "";
        if (termen.length() < MIN_CARACTERE) {
            return List.of();
        }
        PageRequest limita = PageRequest.of(0, LIMITA_PER_TIP);
        List<RezultatCautareDto> rezultate = new ArrayList<>();

        teams.search(termen, limita).forEach(t -> rezultate.add(new RezultatCautareDto(
                TipRezultat.ECHIPA, t.getId(), t.getName(), t.getLogo(), t.getCountryName(),
                Boolean.TRUE.equals(t.getIsNational()))));
        leagues.search(termen, limita).forEach(l -> rezultate.add(new RezultatCautareDto(
                TipRezultat.LIGA, l.getId(), l.getName(), l.getLogo(), l.getCountryName(), false)));
        players.search(termen, limita).forEach(p -> rezultate.add(new RezultatCautareDto(
                TipRezultat.JUCATOR, p.getId(), p.getName(), p.getPhoto(), p.getNationality(), false)));

        return rezultate;
    }
}
