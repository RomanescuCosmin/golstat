package ro.golstat.api.web;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ro.golstat.api.competitie.ClasamentDetaliatDto;
import ro.golstat.api.competitie.ClasamentDetaliatService;
import ro.golstat.api.competitie.CompetitieController;
import ro.golstat.api.competitie.CompetitieService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@WebMvcTest(CompetitieController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "golstat.securitate.cookie-secure=false")
class SecurityConfigCookieNesecurizatTest {

    @Autowired MockMvc mvc;
    @MockitoBean CompetitieService competitii;
    @MockitoBean ClasamentDetaliatService clasamente;

    @Test
    void cookieSecureFalse_xsrfFaraSecure() throws Exception {
        when(clasamente.clasament(5L, 2026)).thenReturn(new ClasamentDetaliatDto(List.of()));

        Cookie xsrf = mvc.perform(get("/api/v1/competitii/5/clasament").param("sezon", "2026"))
                .andReturn().getResponse().getCookie("XSRF-TOKEN");

        assertThat(xsrf).isNotNull();
        assertThat(xsrf.getSecure()).isFalse();
    }
}
