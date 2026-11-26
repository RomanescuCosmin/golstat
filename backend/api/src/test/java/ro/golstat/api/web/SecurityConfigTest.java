package ro.golstat.api.web;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ro.golstat.api.competitie.ClasamentDetaliatDto;
import ro.golstat.api.competitie.ClasamentDetaliatService;
import ro.golstat.api.competitie.CompetitieController;
import ro.golstat.api.competitie.CompetitieService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CompetitieController.class)
@Import(SecurityConfig.class)
class SecurityConfigTest {

    private static final String CALE = "/api/v1/competitii/5/clasament";

    @Autowired MockMvc mvc;
    @MockitoBean CompetitieService competitii;
    @MockitoBean ClasamentDetaliatService clasamente;

    @Test
    void get_anonim_200_faraSesiune() throws Exception {
        when(clasamente.clasament(5L, 2026)).thenReturn(new ClasamentDetaliatDto(List.of()));

        MvcResult rezultat = mvc.perform(get(CALE).param("sezon", "2026"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(rezultat.getRequest().getSession(false)).isNull();
    }

    @Test
    void post_faraCsrf_403ProblemDetail() throws Exception {
        mvc.perform(post(CALE))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.cod").value("csrf"))
                .andExpect(jsonPath("$.instance").value(CALE))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void get_seteazaCookieXsrf_citibilDinJs_sameSiteStrict() throws Exception {
        when(clasamente.clasament(5L, 2026)).thenReturn(new ClasamentDetaliatDto(List.of()));

        Cookie xsrf = mvc.perform(get(CALE).param("sezon", "2026")).andReturn().getResponse().getCookie("XSRF-TOKEN");

        assertThat(xsrf).isNotNull();
        assertThat(xsrf.isHttpOnly()).isFalse();
        assertThat(xsrf.getAttribute("SameSite")).isEqualTo("Strict");
        assertThat(xsrf.getSecure()).isTrue();
    }

    @Test
    void post_cuCookieSiHeaderXsrf_treceDeFiltru() throws Exception {
        when(clasamente.clasament(5L, 2026)).thenReturn(new ClasamentDetaliatDto(List.of()));
        Cookie xsrf = mvc.perform(get(CALE).param("sezon", "2026")).andReturn().getResponse().getCookie("XSRF-TOKEN");

        mvc.perform(post(CALE).cookie(xsrf).header("X-XSRF-TOKEN", xsrf.getValue()))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void post_headerXsrfGresit_403() throws Exception {
        when(clasamente.clasament(5L, 2026)).thenReturn(new ClasamentDetaliatDto(List.of()));
        Cookie xsrf = mvc.perform(get(CALE).param("sezon", "2026")).andReturn().getResponse().getCookie("XSRF-TOKEN");

        mvc.perform(post(CALE).cookie(xsrf).header("X-XSRF-TOKEN", "altceva"))
                .andExpect(status().isForbidden());
    }

    @Test
    void schimbaParola_anonim_401ProblemDetail() throws Exception {
        mvc.perform(post("/api/v1/auth/schimba-parola").with(XsrfTestSuport.xsrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void raspuns_areHeadereSecuritate() throws Exception {
        when(clasamente.clasament(5L, 2026)).thenReturn(new ClasamentDetaliatDto(List.of()));

        mvc.perform(get(CALE).param("sezon", "2026"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().string("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'"));
    }

    // WebSocketConfig nu e in slice: 404 dovedeste ca filtrul CSRF a lasat cererea sa treaca
    @Test
    void ws_nuCereCsrf() throws Exception {
        mvc.perform(post("/ws/123/abc/xhr_send")).andExpect(status().isNotFound());
    }

    @Test
    void dev_nuCereCsrf() throws Exception {
        mvc.perform(post("/api/v1/dev/live/start")).andExpect(status().isNotFound());
    }

    @Test
    void get_anonim_nuSeteazaCookieDeSesiune() throws Exception {
        when(clasamente.clasament(5L, 2026)).thenReturn(new ClasamentDetaliatDto(List.of()));

        mvc.perform(get(CALE).param("sezon", "2026"))
                .andExpect(cookie().doesNotExist("JSESSIONID"))
                .andExpect(cookie().doesNotExist("GOLSTAT_SID"));
    }
}
