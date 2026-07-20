package ro.golstat.api.piete;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ro.golstat.api.piete.PieteZileDto.CotaPiataDto;
import ro.golstat.api.piete.PieteZileDto.LigaDto;
import ro.golstat.api.piete.PieteZileDto.MeciPieteDto;
import ro.golstat.api.piete.PieteZileDto.ZiDto;
import ro.golstat.api.prediction.PredictieMeciDto.EchipaDto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PieteController.class)
class PieteControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean PieteZileService piete;

    private static PieteZileDto exemplu() {
        return new PieteZileDto(List.of(new ZiDto(LocalDate.of(2026, 7, 21), List.of(
                new MeciPieteDto(500L, OffsetDateTime.parse("2026-07-21T18:00:00Z"),
                        new LigaDto(39L, "Premier League", "http://logo/39.png"),
                        new EchipaDto(10L, "FC Gazde", "http://logo/10.png"),
                        new EchipaDto(20L, "FC Oaspeti", null),
                        List.of(new CotaPiataDto(CodPiata.GOLURI_PESTE, 2.5, 0.62, 1.61, 14),
                                new CotaPiataDto(CodPiata.GG, null, 0.55, 1.82, 14)))))));
    }

    @Test
    void zile_returneazaListaGrupataPeZi() throws Exception {
        when(piete.piete(anyInt())).thenReturn(exemplu());

        mvc.perform(get("/api/v1/piete/zile?zile=3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zile[0].data").value("2026-07-21"))
                .andExpect(jsonPath("$.zile[0].meciuri[0].fixtureId").value(500))
                .andExpect(jsonPath("$.zile[0].meciuri[0].liga.nume").value("Premier League"))
                .andExpect(jsonPath("$.zile[0].meciuri[0].gazde.nume").value("FC Gazde"))
                .andExpect(jsonPath("$.zile[0].meciuri[0].oaspeti.logo").isEmpty())
                .andExpect(jsonPath("$.zile[0].meciuri[0].piete[0].piata").value("GOLURI_PESTE"))
                .andExpect(jsonPath("$.zile[0].meciuri[0].piete[0].linie").value(2.5))
                .andExpect(jsonPath("$.zile[0].meciuri[0].piete[0].probabilitate").value(0.62))
                .andExpect(jsonPath("$.zile[0].meciuri[0].piete[0].cota").value(1.61))
                .andExpect(jsonPath("$.zile[0].meciuri[0].piete[0].esantion").value(14))
                .andExpect(jsonPath("$.zile[0].meciuri[0].piete[1].piata").value("GG"))
                .andExpect(jsonPath("$.zile[0].meciuri[0].piete[1].linie").isEmpty());
    }

    @Test
    void zile_fataParametruFoloseste3() throws Exception {
        when(piete.piete(anyInt())).thenReturn(exemplu());

        mvc.perform(get("/api/v1/piete/zile")).andExpect(status().isOk());

        verify(piete).piete(3);
    }

    @Test
    void zile_inAfaraIntervaluluiDa400() throws Exception {
        mvc.perform(get("/api/v1/piete/zile?zile=0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/piete/zile?zile=8")).andExpect(status().isBadRequest());
    }
}
