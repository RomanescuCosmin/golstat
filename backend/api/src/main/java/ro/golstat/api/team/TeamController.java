package ro.golstat.api.team;

import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Expune pagina unei echipe. Erorile sunt tratate central in {@code GlobalExceptionHandler}. */
@RestController
@RequestMapping("/api/v1/echipe")
@Validated
public class TeamController {

    private final TeamService teams;

    public TeamController(TeamService teams) {
        this.teams = teams;
    }

    /** Pagina echipei; 404 doar daca echipa nu exista. Liga/sezonul sunt optionale (altfel derivate). */
    @GetMapping("/{teamId}")
    public PaginaEchipaDto echipa(@PathVariable @Min(1) long teamId,
                                  @RequestParam(required = false) Long leagueId,
                                  @RequestParam(required = false) Integer sezon) {
        return teams.pagina(teamId, leagueId, sezon);
    }
}
