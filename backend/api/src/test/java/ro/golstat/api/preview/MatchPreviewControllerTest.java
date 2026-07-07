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
import ro.golstat.api.preview.FormaEchipaDto.FereastraFormaDto;
import ro.golstat.api.preview.StatisticiAvansateDto.EgaluriDto;
import ro.golstat.api.preview.StatisticiAvansateDto.FrecventaDto;
import ro.golstat.api.preview.StatisticiAvansateDto.GgDto;
import ro.golstat.api.preview.StatisticiAvansateDto.LinieDto;
import ro.golstat.api.preview.StatisticiAvansateDto.MediiEchipaDto;
import ro.golstat.api.preview.StatisticiAvansateDto.PiataDto;
import ro.golstat.api.preview.StatisticiAvansateDto.ReprizeDto;
import ro.golstat.api.preview.StatisticiCheieDto.StatisticiEchipaDto;

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
                List.of(), new ProcentCota(52.0, 1.92), 8, 6, null);
        FormaEchipaDto formaGazde = new FormaEchipaDto(
                new FereastraFormaDto(
                        List.of(new FormaMeciDto(LocalDate.of(2025, 5, 6), true, 2, 1, "V")), 1.33, 1.17),
                new FereastraFormaDto(List.of(), 1.1, 1.0));
        FormaEchipaDto formaOaspeti = new FormaEchipaDto(
                new FereastraFormaDto(
                        List.of(new FormaMeciDto(LocalDate.of(2025, 5, 5), false, 0, 0, "E")), 0.8, 1.4),
                new FereastraFormaDto(List.of(), 0.9, 1.2));
        IntalnireDirectaDto directa = new IntalnireDirectaDto(55L,
                OffsetDateTime.parse("2024-11-01T18:00:00Z"), oaspeti, gazde, 3, 0);
        StatisticiAvansateDto statistici = new StatisticiAvansateDto(
                new PiataDto(List.of(new LinieDto(2.5, 0.55,
                        new FrecventaDto(5, 7), new FrecventaDto(4, 7),
                        new FrecventaDto(3, 7), new FrecventaDto(3, 7))),
                        new MediiEchipaDto(1.8, 3.1, 1.6, 2.9), new MediiEchipaDto(1.1, 2.4, 1.3, 2.6)),
                new GgDto(0.61, new FrecventaDto(6, 7), new FrecventaDto(4, 7),
                        new FrecventaDto(5, 7), new FrecventaDto(3, 7)),
                new PiataDto(List.of(new LinieDto(9.5, 0.62,
                        new FrecventaDto(4, 7), new FrecventaDto(4, 7),
                        new FrecventaDto(2, 7), new FrecventaDto(3, 7))),
                        new MediiEchipaDto(6.2, 10.4, 5.8, 9.9), new MediiEchipaDto(4.1, 9.2, 4.4, 9.5)),
                new PiataDto(List.of(), new MediiEchipaDto(null, null, null, null),
                        new MediiEchipaDto(null, null, null, null)),
                new PiataDto(List.of(new LinieDto(4.5, 0.41,
                        new FrecventaDto(3, 7), new FrecventaDto(3, 7),
                        new FrecventaDto(4, 7), new FrecventaDto(4, 7))),
                        new MediiEchipaDto(2.1, 4.3, 2.0, 4.1), new MediiEchipaDto(2.4, 4.6, 2.2, 4.4)),
                new PiataDto(List.of(new LinieDto(24.5, 0.52,
                        new FrecventaDto(4, 7), new FrecventaDto(4, 7),
                        new FrecventaDto(3, 7), new FrecventaDto(3, 7))),
                        new MediiEchipaDto(13.2, 24.8, 12.9, 24.1), new MediiEchipaDto(11.4, 23.6, 11.8, 23.9)),
                new PiataDto(List.of(new LinieDto(8.5, 0.57,
                        new FrecventaDto(5, 7), new FrecventaDto(4, 7),
                        new FrecventaDto(3, 7), new FrecventaDto(3, 7))),
                        new MediiEchipaDto(4.9, 8.8, 4.7, 8.6), new MediiEchipaDto(3.9, 8.1, 4.0, 8.3)),
                new EgaluriDto(0.38, 0.27,
                        new FrecventaDto(3, 7), new FrecventaDto(2, 7),
                        new FrecventaDto(2, 7), new FrecventaDto(1, 7)),
                new ReprizeDto(0.72, 0.81,
                        new FrecventaDto(5, 7), new FrecventaDto(4, 7),
                        new FrecventaDto(6, 7), new FrecventaDto(5, 7)),
                null);
        StatisticiCheieDto statisticiCheie = new StatisticiCheieDto(
                new StatisticiEchipaDto(55.0, 12.4, 5.1, 6.2, 2.1),
                new StatisticiEchipaDto(null, null, null, null, null));
        EchipaDeStartDto echipeDeStart = new EchipaDeStartDto(
                new EchipaDeStartDto.EchipaLineupDto("4-3-3",
                        List.of(new EchipaDeStartDto.JucatorDto(617L, "Ederson", 31, "G", "1:1",
                                "http://foto/617.png")),
                        List.of(new EchipaDeStartDto.JucatorDto(619L, "Rezerva", 13, "G", null, null)),
                        List.of(new EchipaDeStartDto.IndisponibilDto(801L, "A. Accidentat", "ACCIDENTAT", "Knee Injury"))),
                new EchipaDeStartDto.EchipaLineupDto("4-4-2", List.of(), List.of(), List.of()),
                "M. Oliver", false);
        return new PrevizualizareMeciDto(predictie, formaGazde, formaOaspeti, List.of(directa),
                statistici, statisticiCheie, echipeDeStart);
    }

    @Test
    void previzualizare_returnsFullJson() throws Exception {
        when(preview.previzualizare(100L)).thenReturn(sampleDto());

        mvc.perform(get("/api/v1/predictii/meciuri/100/previzualizare"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.predictie.fixtureId").value(100))
                .andExpect(jsonPath("$.predictie.echipaGazde.nume").value("FC Gazde"))
                .andExpect(jsonPath("$.predictie.gazde.procent").value(45.0))
                .andExpect(jsonPath("$.formaGazde.locatie.meciuri[0].rezultat").value("V"))
                .andExpect(jsonPath("$.formaGazde.locatie.meciuri[0].acasa").value(true))
                .andExpect(jsonPath("$.formaGazde.locatie.goluriMarcatePeMeci").value(1.33))
                .andExpect(jsonPath("$.formaGazde.general.goluriMarcatePeMeci").value(1.1))
                .andExpect(jsonPath("$.formaOaspeti.locatie.meciuri[0].rezultat").value("E"))
                .andExpect(jsonPath("$.intalniriDirecte[0].fixtureId").value(55))
                .andExpect(jsonPath("$.intalniriDirecte[0].gazde.nume").value("FC Oaspeti"))
                .andExpect(jsonPath("$.intalniriDirecte[0].golGazde").value(3))
                .andExpect(jsonPath("$.intalniriDirecte[0].golOaspeti").value(0))
                .andExpect(jsonPath("$.statistici.goluri.linii[0].linie").value(2.5))
                .andExpect(jsonPath("$.statistici.goluri.linii[0].probabilitate").value(0.55))
                .andExpect(jsonPath("$.statistici.goluri.linii[0].gazdeLocatie.reusite").value(5))
                .andExpect(jsonPath("$.statistici.goluri.linii[0].gazdeLocatie.total").value(7))
                .andExpect(jsonPath("$.statistici.goluri.gazde.proprieLocatie").value(1.8))
                .andExpect(jsonPath("$.statistici.gg.probabilitate").value(0.61))
                .andExpect(jsonPath("$.statistici.gg.gazdeMarcat.reusite").value(6))
                .andExpect(jsonPath("$.statistici.cornere.linii[0].linie").value(9.5))
                .andExpect(jsonPath("$.statistici.cornere.gazde.totalLocatie").value(10.4))
                .andExpect(jsonPath("$.statistici.faulturi.gazde.proprieLocatie").isEmpty())
                .andExpect(jsonPath("$.statistici.cartonase.linii[0].probabilitate").value(0.41))
                .andExpect(jsonPath("$.statistici.suturi.linii[0].linie").value(24.5))
                .andExpect(jsonPath("$.statistici.suturi.gazde.proprieLocatie").value(13.2))
                .andExpect(jsonPath("$.statistici.suturiPePoarta.linii[0].probabilitate").value(0.57))
                .andExpect(jsonPath("$.statistici.egaluri.egalPauza").value(0.38))
                .andExpect(jsonPath("$.statistici.egaluri.egalFinal").value(0.27))
                .andExpect(jsonPath("$.statistici.reprize.golRepriza1").value(0.72))
                .andExpect(jsonPath("$.statistici.reprize.repriza2Gazde.reusite").value(6))
                .andExpect(jsonPath("$.statisticiCheie.gazde.posesieMedie").value(55.0))
                .andExpect(jsonPath("$.statisticiCheie.oaspeti.posesieMedie").isEmpty())
                .andExpect(jsonPath("$.echipeDeStart.arbitru").value("M. Oliver"))
                .andExpect(jsonPath("$.echipeDeStart.gazde.formatie").value("4-3-3"))
                .andExpect(jsonPath("$.echipeDeStart.gazde.titulari[0].nume").value("Ederson"))
                .andExpect(jsonPath("$.echipeDeStart.gazde.titulari[0].grid").value("1:1"))
                .andExpect(jsonPath("$.echipeDeStart.gazde.titulari[0].foto").value("http://foto/617.png"))
                .andExpect(jsonPath("$.echipeDeStart.probabila").value(false))
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
