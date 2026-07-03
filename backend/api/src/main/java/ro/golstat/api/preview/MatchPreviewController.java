package ro.golstat.api.preview;

import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Expune previzualizarea unui meci. Erorile sunt tratate central in {@code GlobalExceptionHandler}. */
@RestController
@RequestMapping("/api/v1/predictii/meciuri")
@Validated
public class MatchPreviewController {

    private final MatchPreviewService preview;

    public MatchPreviewController(MatchPreviewService preview) {
        this.preview = preview;
    }

    /** Previzualizarea unui meci anume; 404 daca nu exista sau nu e viitor. */
    @GetMapping("/{fixtureId}/previzualizare")
    public PrevizualizareMeciDto previzualizare(@PathVariable @Min(1) long fixtureId) {
        return preview.previzualizare(fixtureId);
    }
}
