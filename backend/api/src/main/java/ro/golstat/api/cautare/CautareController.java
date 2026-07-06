package ro.golstat.api.cautare;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Cautare globala din navbar: echipe, campionate, jucatori (min 2 caractere, max 5 per tip). */
@RestController
@RequestMapping("/api/v1/cauta")
public class CautareController {

    private final CautareService cautare;

    public CautareController(CautareService cautare) {
        this.cautare = cautare;
    }

    @GetMapping
    public List<RezultatCautareDto> cauta(@RequestParam(required = false) String q) {
        return cautare.cauta(q);
    }
}
