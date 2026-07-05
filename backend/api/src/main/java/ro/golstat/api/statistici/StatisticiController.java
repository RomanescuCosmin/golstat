package ro.golstat.api.statistici;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Pagina Statistici: clasamentul de tendinte pe ligi (medii goluri/cornere/faulturi/cartonase). */
@RestController
@RequestMapping("/api/v1/statistici")
public class StatisticiController {

    private final StatisticiService statistici;

    public StatisticiController(StatisticiService statistici) {
        this.statistici = statistici;
    }

    @GetMapping("/ligi")
    public List<StatisticiLigaDto> ligi() {
        return statistici.ligi();
    }
}
