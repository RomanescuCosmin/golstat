package ro.golstat.api.piete;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    public PieteZileDto zile(@RequestParam(defaultValue = "3") @Min(1) @Max(7) int zile) {
        return piete.piete(zile);
    }
}
