package ro.golstat.api.competitie;

import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Pagina unei competitii: clasament, golgheteri/pasatori, rezultate si program. */
@RestController
@RequestMapping("/api/v1/competitii")
@Validated
public class CompetitieController {

    private final CompetitieService competitii;

    public CompetitieController(CompetitieService competitii) {
        this.competitii = competitii;
    }

    /** Detaliul unei competitii; sezonul e optional (default = cel mai recent sezon jucat); 404 daca nu exista. */
    @GetMapping("/{leagueId}")
    public PaginaCompetitieDto competitie(@PathVariable @Min(1) long leagueId,
                                          @RequestParam(required = false) Integer sezon) {
        return competitii.pagina(leagueId, sezon);
    }
}
