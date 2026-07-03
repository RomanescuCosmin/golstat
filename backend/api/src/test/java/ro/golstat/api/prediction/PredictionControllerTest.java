package ro.golstat.api.prediction;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ro.golstat.api.prediction.PredictieMeciDto.EchipaDto;
import ro.golstat.api.prediction.PredictieMeciDto.LinieGolDto;
import ro.golstat.api.prediction.PredictieMeciDto.ProcentCota;
import ro.golstat.api.web.CorsConfig;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PredictionController.class)
@Import(CorsConfig.class)
class PredictionControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean PredictionService predictions;

    private static PredictieMeciDto sampleDto() {
        LinieGolDto linie = new LinieGolDto(2.5, new ProcentCota(55.0, 1.82), new ProcentCota(45.0, 2.22));
        return new PredictieMeciDto(
                100L,
                new EchipaDto(10L, "FC Gazde", "http://logo/10.png"),
                new EchipaDto(20L, "FC Oaspeti", "http://logo/20.png"),
                OffsetDateTime.parse("2025-08-16T14:00:00Z"),
                1.6, 1.2,
                new ProcentCota(45.0, 2.22), new ProcentCota(27.0, 3.70), new ProcentCota(28.0, 3.57),
                List.of(linie), new ProcentCota(52.0, 1.92), 8, 6);
    }

    @Test
    void meci_returnsPredictionJson() throws Exception {
        when(predictions.predict(100L)).thenReturn(Optional.of(sampleDto()));

        mvc.perform(get("/api/v1/predictii/meciuri/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fixtureId").value(100))
                .andExpect(jsonPath("$.echipaGazde.id").value(10))
                .andExpect(jsonPath("$.echipaGazde.nume").value("FC Gazde"))
                .andExpect(jsonPath("$.echipaGazde.logo").value("http://logo/10.png"))
                .andExpect(jsonPath("$.echipaOaspeti.nume").value("FC Oaspeti"))
                .andExpect(jsonPath("$.gazde.procent").value(45.0))
                .andExpect(jsonPath("$.gazde.cota").value(2.22))
                .andExpect(jsonPath("$.linii[0].linie").value(2.5))
                .andExpect(jsonPath("$.esantionGazde").value(8));
    }

    @Test
    void corsRequest_allowedOrigin_returnsAllowOriginHeader() throws Exception {
        when(predictions.predict(100L)).thenReturn(Optional.of(sampleDto()));

        mvc.perform(get("/api/v1/predictii/meciuri/100").header("Origin", "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void corsRequest_unknownOrigin_isRejected() throws Exception {
        mvc.perform(get("/api/v1/predictii/meciuri/100").header("Origin", "http://evil.example"))
                .andExpect(status().isForbidden());
    }

    @Test
    void meciuri_returnsArray() throws Exception {
        when(predictions.upcoming(39L, LocalDate.of(2025, 8, 16))).thenReturn(List.of(sampleDto()));

        mvc.perform(get("/api/v1/predictii/meciuri?leagueId=39&data=2025-08-16"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fixtureId").value(100))
                .andExpect(jsonPath("$[0].oaspeti.procent").value(28.0));
    }

    @Test
    void meci_missing_returns404ProblemDetail() throws Exception {
        when(predictions.predict(999L)).thenReturn(Optional.empty());

        mvc.perform(get("/api/v1/predictii/meciuri/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Predictie indisponibila"))
                .andExpect(jsonPath("$.detail").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void meci_nonNumericId_returns400() throws Exception {
        mvc.perform(get("/api/v1/predictii/meciuri/abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void meci_idBelowMinimum_returns400() throws Exception {
        mvc.perform(get("/api/v1/predictii/meciuri/0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void meciuri_missingLeagueId_returns400() throws Exception {
        mvc.perform(get("/api/v1/predictii/meciuri?data=2025-08-16"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void meciuri_badDate_returns400() throws Exception {
        mvc.perform(get("/api/v1/predictii/meciuri?leagueId=39&data=notadate"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unexpectedError_returns500Generic_withoutLeakingCause() throws Exception {
        when(predictions.predict(77L)).thenThrow(new RuntimeException("boom cu detalii interne"));

        mvc.perform(get("/api/v1/predictii/meciuri/77"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail", not(containsString("boom"))));
    }
}
