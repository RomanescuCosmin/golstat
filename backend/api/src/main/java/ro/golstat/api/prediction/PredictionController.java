package ro.golstat.api.prediction;

import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Expune predictiile pentru meciurile viitoare. Erorile sunt tratate central in {@code GlobalExceptionHandler}. */
@RestController
@RequestMapping("/api/v1/predictii/meciuri")
@Validated
public class PredictionController {

    private final PredictionService predictions;

    public PredictionController(PredictionService predictions) {
        this.predictions = predictions;
    }

    /** Predictia unui meci anume; 404 daca nu exista sau nu e viitor. */
    @GetMapping("/{fixtureId}")
    public PredictieMeciDto meci(@PathVariable @Min(1) long fixtureId) {
        return predictions.predict(fixtureId)
                .orElseThrow(() -> new PredictionNotFoundException(fixtureId));
    }

    /** Predictiile meciurilor viitoare ale unei ligi intr-o zi ({@code data=YYYY-MM-DD}). */
    @GetMapping
    public List<PredictieMeciDto> meciuri(
            @RequestParam @Min(1) long leagueId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return predictions.upcoming(leagueId, data);
    }
}
