package ro.golstat.api.competitie;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ro.golstat.api.competitie.ClasamentDetaliatDto.Grupa;
import ro.golstat.api.competitie.ClasamentDetaliatDto.MeciForma;
import ro.golstat.api.competitie.ClasamentDetaliatDto.Rand;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CompetitieController.class)
class CompetitieControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean CompetitieService competitii;
    @MockitoBean ClasamentDetaliatService clasamente;

    @Test
    void clasament_grupeRanduriSiForma() throws Exception {
        Rand romania = new Rand(1, 33L, "Romania", "ro.png", 6, 5, 0, 1, 12, 4, 8, 15, "Promotion",
                List.of(new MeciForma(900L, LocalDate.of(2026, 11, 14), "V", false, "Cipru", 0, 2)));
        when(clasamente.clasament(5L, 2026)).thenReturn(new ClasamentDetaliatDto(List.of(
                new Grupa("League C, Group 2", List.of(romania)))));

        mvc.perform(get("/api/v1/competitii/5/clasament").param("sezon", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grupe[0].nume").value("League C, Group 2"))
                .andExpect(jsonPath("$.grupe[0].randuri[0].pozitie").value(1))
                .andExpect(jsonPath("$.grupe[0].randuri[0].teamId").value(33))
                .andExpect(jsonPath("$.grupe[0].randuri[0].nume").value("Romania"))
                .andExpect(jsonPath("$.grupe[0].randuri[0].golaveraj").value(8))
                .andExpect(jsonPath("$.grupe[0].randuri[0].puncte").value(15))
                .andExpect(jsonPath("$.grupe[0].randuri[0].zona").value("Promotion"))
                .andExpect(jsonPath("$.grupe[0].randuri[0].forma[0].fixtureId").value(900))
                .andExpect(jsonPath("$.grupe[0].randuri[0].forma[0].data").value("2026-11-14"))
                .andExpect(jsonPath("$.grupe[0].randuri[0].forma[0].acasa").value(false))
                .andExpect(jsonPath("$.grupe[0].randuri[0].forma[0].golGazde").value(0))
                .andExpect(jsonPath("$.grupe[0].randuri[0].forma[0].golOaspeti").value(2));
    }

    @Test
    void clasament_faraGrupe_listaGoala() throws Exception {
        when(clasamente.clasament(5L, 2026)).thenReturn(new ClasamentDetaliatDto(List.of()));

        mvc.perform(get("/api/v1/competitii/5/clasament").param("sezon", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grupe").isEmpty());
    }

    @Test
    void clasament_faraSezon_400() throws Exception {
        mvc.perform(get("/api/v1/competitii/5/clasament")).andExpect(status().isBadRequest());
        verifyNoInteractions(clasamente);
    }

    @Test
    void clasament_idInvalid_400() throws Exception {
        mvc.perform(get("/api/v1/competitii/0/clasament").param("sezon", "2026"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(clasamente);
    }
}
