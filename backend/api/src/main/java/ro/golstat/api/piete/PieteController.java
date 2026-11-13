package ro.golstat.api.piete;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Lista de piete pe zilele urmatoare. Filtrarea pe piata, pragul si sortarea se fac in interfata —
 * aici se intoarce tot ce are eșantion, o singura data.
 */
@RestController
@RequestMapping("/api/v1/piete")
@Validated
public class PieteController {

    private final PieteZileService piete;

    public PieteController(PieteZileService piete) {
        this.piete = piete;
    }

    @GetMapping("/zile")
    /**
     * Implicit urmatoarele {@code zile} zile; cu {@code de} + {@code pana} (ISO, ex. 2026-11-14) perioada aleasa
     * de utilizator, inclusiv capetele.
     */
    public PieteZileDto zile(@RequestParam(defaultValue = "3") @Min(1) @Max(7) int zile,
                            @RequestParam(defaultValue = "toate") String competitie,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate pana) {
        boolean doarCompetitia = "curenta".equalsIgnoreCase(competitie);
        if (de != null && pana != null) {
            return piete.piete(de, pana, doarCompetitia);
        }
        return piete.piete(zile, doarCompetitia);
    }
}
