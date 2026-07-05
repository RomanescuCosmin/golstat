package ro.golstat.api.jucator;

import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Profilul unui jucator: identitate + statistici pe sezoane. */
@RestController
@RequestMapping("/api/v1/jucatori")
@Validated
public class JucatorController {

    private final JucatorService jucatori;

    public JucatorController(JucatorService jucatori) {
        this.jucatori = jucatori;
    }

    /** Detaliul unui jucator dupa id; 404 daca nu exista. */
    @GetMapping("/{playerId}")
    public PaginaJucatorDto jucator(@PathVariable @Min(1) long playerId) {
        return jucatori.pagina(playerId);
    }
}
