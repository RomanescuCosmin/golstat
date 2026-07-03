package ro.golstat.api.preview;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ro.golstat.api.prediction.PredictieMeciDto;
import ro.golstat.api.prediction.PredictieMeciDto.EchipaDto;
import ro.golstat.api.prediction.PredictieMeciDto.ProcentCota;
import ro.golstat.api.prediction.PredictionNotFoundException;
import ro.golstat.api.preview.StatisticiCheieDto.StatisticiEchipaDto;
import ro.golstat.stats.market.OverUnder;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MatchPreviewController.class)
class MatchPreviewControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean MatchPreviewService preview;

    private static PrevizualizareMeciDto sampleDto() {
        EchipaDto gazde = new EchipaDto(10L, "FC Gazde", "http://logo/10.png");
        EchipaDto oaspeti = new EchipaDto(20L, "FC Oaspeti", "http://logo/20.png");
        PredictieMeciDto predictie = new PredictieMeciDto(100L, gazde, oaspeti,
                OffsetDateTime.parse("2025-08-16T14:00:00Z"), 1.6, 1.2,
                new ProcentCota(45.0, 2.22), new ProcentCota(27.0, 3.70), new ProcentCota(28.0, 3.57),
                List.of(), new ProcentCota(52.0, 1.92), 8, 6);
        FormaEchipaDto formaGazde = new FormaEchipaDto(
                List.of(new FormaMeciDto(LocalDate.of(2025, 5, 6), true, 2, 1, "V")), 1.33, 1.17);
        FormaEchipaDto formaOaspeti = new FormaEchipaDto(
                List.of(new FormaMeciDto(LocalDate.of(2025, 5, 5), false, 0, 0, "E")), 0.8, 1.4);
        IntalnireDirectaDto directa = new IntalnireDirectaDto(55L,
                OffsetDateTime.parse("2024-11-01T18:00:00Z"), oaspeti, gazde, 3, 0);
        StatisticiCheieDto statistici = new StatisticiCheieDto(
                new StatisticiEchipaDto(55.0, 12.4, 5.1, 6.2, 2.1),
                new StatisticiEchipaDto(null, null, null, null, null));
        EchipaDeStartDto echipeDeStart = new EchipaDeStartDto(
                new EchipaDeStartDto.EchipaLineupDto("4-3-3",
                        List.of(new EchipaDeStartDto.JucatorDto(617L, "Ederson", 31, "G", "1:1")),
                        List.of(new EchipaDeStartDto.JucatorDto(619L, "Rezerva", 13, "G", null)),
                        List.of(new EchipaDeStartDto.IndisponibilDto(801L, "A. Accidentat", "ACCIDENTAT", "Knee Injury"))),
                new EchipaDeStartDto.EchipaLineupDto("4-4-2", List.of(), List.of(), List.of()),
                "M. Oliver");
        return new PrevizualizareMeciDto(predictie, formaGazde, formaOaspeti, List.of(directa),
                List.of(new OverUnder(8.5, 0.62, 0.38)),
                List.of(new OverUnder(24.5, 0.48, 0.52)),
                List.of(new OverUnder(4.5, 0.41, 0.59)),
                statistici,
                echipeDeStart);
    }

    @Test
    void previzualizare_returnsFullJson() throws Exception {
        when(preview.previzualizare(100L)).thenReturn(sampleDto());

        mvc.perform(get("/api/v1/predictii/meciuri/100/previzualizare"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.predictie.fixtureId").value(100))
                .andExpect(jsonPath("$.predictie.echipaGazde.nume").value("FC Gazde"))
                .andExpect(jsonPath("$.predictie.gazde.procent").value(45.0))
                .andExpect(jsonPath("$.formaGazde.meciuri[0].rezultat").value("V"))
                .andExpect(jsonPath("$.formaGazde.meciuri[0].acasa").value(true))
                .andExpect(jsonPath("$.formaGazde.goluriMarcatePeMeci").value(1.33))
                .andExpect(jsonPath("$.formaOaspeti.meciuri[0].rezultat").value("E"))
                .andExpect(jsonPath("$.intalniriDirecte[0].fixtureId").value(55))
                .andExpect(jsonPath("$.intalniriDirecte[0].gazde.nume").value("FC Oaspeti"))
                .andExpect(jsonPath("$.intalniriDirecte[0].golGazde").value(3))
                .andExpect(jsonPath("$.intalniriDirecte[0].golOaspeti").value(0))
                .andExpect(jsonPath("$.cornere[0].line").value(8.5))
                .andExpect(jsonPath("$.cornere[0].overRate").value(0.62))
                .andExpect(jsonPath("$.faulturi[0].line").value(24.5))
                .andExpect(jsonPath("$.cartonase[0].line").value(4.5))
                .andExpect(jsonPath("$.cartonase[0].underRate").value(0.59))
                .andExpect(jsonPath("$.statisticiCheie.gazde.posesieMedie").value(55.0))
                .andExpect(jsonPath("$.statisticiCheie.gazde.suturiPeMeci").value(12.4))
                .andExpect(jsonPath("$.statisticiCheie.oaspeti.posesieMedie").isEmpty())
                .andExpect(jsonPath("$.echipeDeStart.arbitru").value("M. Oliver"))
                .andExpect(jsonPath("$.echipeDeStart.gazde.formatie").value("4-3-3"))
                .andExpect(jsonPath("$.echipeDeStart.gazde.titulari[0].nume").value("Ederson"))
                .andExpect(jsonPath("$.echipeDeStart.gazde.titulari[0].grid").value("1:1"))
                .andExpect(jsonPath("$.echipeDeStart.gazde.rezerve[0].numar").value(13))
                .andExpect(jsonPath("$.echipeDeStart.gazde.indisponibili[0].motiv").value("ACCIDENTAT"))
                .andExpect(jsonPath("$.echipeDeStart.oaspeti.formatie").value("4-4-2"));
    }

    @Test
    void previzualizare_missing_returns404ProblemDetail() throws Exception {
        when(preview.previzualizare(999L)).thenThrow(new PredictionNotFoundException(999L));

        mvc.perform(get("/api/v1/predictii/meciuri/999/previzualizare"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Predictie indisponibila"))
                .andExpect(jsonPath("$.detail").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void previzualizare_idBelowMinimum_returns400() throws Exception {
        mvc.perform(get("/api/v1/predictii/meciuri/0/previzualizare"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void previzualizare_nonNumericId_returns400() throws Exception {
        mvc.perform(get("/api/v1/predictii/meciuri/abc/previzualizare"))
                .andExpect(status().isBadRequest());
    }
}
