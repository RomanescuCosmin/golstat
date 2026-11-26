package ro.golstat.api.auth;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import ro.golstat.api.entity.Utilizator;
import ro.golstat.api.repository.UtilizatorRepository;
import ro.golstat.api.web.ClockConfig;
import ro.golstat.api.web.SecurityConfig;
import ro.golstat.api.web.XsrfTestSuport;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, AutentificareConfig.class, ParoleConfig.class, ClockConfig.class,
        AutentificareService.class, UtilizatorDetailsService.class, LimitariAuth.class, AuditAuth.class})
class AuthControllerTest {

    private static final String PAROLA = "cal verde sare gardul";
    private static final AtomicInteger IP = new AtomicInteger(1);

    @Autowired MockMvc mvc;
    @Autowired PasswordEncoder encoder;
    @MockitoBean UtilizatorRepository utilizatori;
    @MockitoBean ContService cont;

    /** Limitele sunt in memorie si contextul e partajat: fiecare test are IP-ul lui. */
    private String ip;

    @BeforeEach
    void setUp() {
        ip = "10.0.0." + IP.getAndIncrement();
    }

    @Test
    void autentificare_corecta_200_sesiuneNoua_cuIdRotit_siCsrfNou() throws Exception {
        utilizatorExistent("ana@ex.ro", true);
        var inainte = new MockHttpSession();
        String idInainte = inainte.getId();

        MvcResult r = mvc.perform(autentificare("Ana@Ex.ro", PAROLA).session(inainte))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.email").value("ana@ex.ro"))
                .andExpect(jsonPath("$.nume").value("Ana"))
                .andExpect(jsonPath("$.parolaHash").doesNotExist())
                .andReturn();

        assertThat(r.getRequest().getSession(false).getId()).isNotEqualTo(idInainte);
        Cookie[] xsrf = java.util.Arrays.stream(r.getResponse().getCookies())
                .filter(c -> c.getName().equals("XSRF-TOKEN")).toArray(Cookie[]::new);
        assertThat(xsrf).isNotEmpty();
        assertThat(xsrf[xsrf.length - 1].getValue()).isNotEmpty().isNotEqualTo("token-test-csrf");
        verify(utilizatori).marcheazaAutentificare(org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.any(Instant.class));
    }

    @Test
    void autentificare_apoiEu_returneazaUtilizatorul() throws Exception {
        utilizatorExistent("ana@ex.ro", true);
        MockHttpSession sesiune = login("ana@ex.ro");

        mvc.perform(get("/api/v1/auth/eu").session(sesiune))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ana@ex.ro"));
    }

    @Test
    void autentificare_parolaGresita_si_emailInexistent_raspunsIdentic401() throws Exception {
        utilizatorExistent("ana@ex.ro", true);
        when(utilizatori.findByEmail("nimeni@ex.ro")).thenReturn(Optional.empty());

        String gresita = mvc.perform(autentificare("ana@ex.ro", "parola gresita aici"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Email sau parolă incorecte."))
                .andReturn().getResponse().getContentAsString();
        String inexistent = mvc.perform(autentificare("nimeni@ex.ro", PAROLA))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(faraTimestamp(gresita)).isEqualTo(faraTimestamp(inexistent));
    }

    @Test
    void autentificare_neconfirmat_parolaCorecta_403CuCod() throws Exception {
        utilizatorExistent("neconf@ex.ro", false);

        mvc.perform(autentificare("neconf@ex.ro", PAROLA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.cod").value("email-neconfirmat"));
    }

    @Test
    void autentificare_neconfirmat_parolaGresita_401() throws Exception {
        utilizatorExistent("neconf2@ex.ro", false);

        mvc.perform(autentificare("neconf2@ex.ro", "parola gresita aici"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void autentificare_faraCsrf_403() throws Exception {
        mvc.perform(post("/api/v1/auth/autentificare").with(dinIp())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("ana@ex.ro", PAROLA)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.cod").value("csrf"));
    }

    @Test
    void autentificare_pesteLimitaPeEmail_429CuRetryAfter() throws Exception {
        utilizatorExistent("tinta@ex.ro", true);
        for (int i = 0; i < 5; i++) {
            mvc.perform(autentificare("tinta@ex.ro", "parola gresita aici")).andExpect(status().isUnauthorized());
        }

        mvc.perform(autentificare("tinta@ex.ro", PAROLA))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.retryAfter").isNumber());
    }

    @Test
    void autentificare_victimaBlocataDeAtacator_nuEBlocataDePeAltIp() throws Exception {
        utilizatorExistent("victima@ex.ro", true);
        for (int i = 0; i < 6; i++) {
            mvc.perform(autentificare("victima@ex.ro", "parola gresita aici"));
        }

        ip = "10.9.9." + IP.getAndIncrement();
        mvc.perform(autentificare("victima@ex.ro", PAROLA)).andExpect(status().isOk());
    }

    @Test
    void eu_utilizatorSters_204_siSesiuneaInvalidata() throws Exception {
        utilizatorExistent("ana@ex.ro", true);
        MockHttpSession sesiune = login("ana@ex.ro");
        when(utilizatori.findById(7L)).thenReturn(Optional.empty());

        mvc.perform(get("/api/v1/auth/eu").session(sesiune)).andExpect(status().isNoContent());

        assertThat(sesiune.isInvalid()).isTrue();
    }

    @Test
    void deconectare_stergeCookieXsrf() throws Exception {
        utilizatorExistent("ana@ex.ro", true);
        MockHttpSession sesiune = login("ana@ex.ro");

        MvcResult r = mvc.perform(post("/api/v1/auth/deconectare").session(sesiune).with(XsrfTestSuport.xsrf()))
                .andExpect(status().isNoContent()).andReturn();

        assertThat(java.util.Arrays.stream(r.getResponse().getCookies())
                .anyMatch(c -> c.getName().equals("XSRF-TOKEN") && c.getMaxAge() == 0)).isTrue();
    }

    @Test
    void confirmareEmail_parolaGresita_400CuCod() throws Exception {
        doThrow(new ParolaCurentaGresitaException()).when(cont).confirmaEmail(anyString(), anyString());

        mvc.perform(post("/api/v1/auth/confirmare-email").with(XsrfTestSuport.xsrf()).with(dinIp())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"abc\",\"parola\":\"gresita\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.cod").value("parola-gresita"));
    }

    @Test
    void inregistrare_pesteLimitaPeIp_429() throws Exception {
        for (int i = 0; i < 5; i++) {
            mvc.perform(inregistrare()).andExpect(status().isAccepted());
        }
        mvc.perform(inregistrare()).andExpect(status().isTooManyRequests());
    }

    private MockHttpServletRequestBuilder inregistrare() {
        return post("/api/v1/auth/inregistrare").with(XsrfTestSuport.xsrf()).with(dinIp())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"ana@ex.ro\",\"parola\":\"" + PAROLA + "\",\"nume\":\"Ana\"}");
    }

    @Test
    void autentificare_reusita_reseteazaContorulEmailului() throws Exception {
        utilizatorExistent("reset@ex.ro", true);
        for (int i = 0; i < 4; i++) {
            mvc.perform(autentificare("reset@ex.ro", "parola gresita aici")).andExpect(status().isUnauthorized());
        }
        mvc.perform(autentificare("reset@ex.ro", PAROLA)).andExpect(status().isOk());

        for (int i = 0; i < 4; i++) {
            mvc.perform(autentificare("reset@ex.ro", "parola gresita aici")).andExpect(status().isUnauthorized());
        }
    }

    @Test
    void eu_anonim_204_siCookieXsrf() throws Exception {
        mvc.perform(get("/api/v1/auth/eu"))
                .andExpect(status().isNoContent())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie().exists("XSRF-TOKEN"));
    }

    @Test
    void deconectare_invalideazaSesiunea_204() throws Exception {
        utilizatorExistent("ana@ex.ro", true);
        MockHttpSession sesiune = login("ana@ex.ro");

        mvc.perform(post("/api/v1/auth/deconectare").session(sesiune).with(XsrfTestSuport.xsrf()))
                .andExpect(status().isNoContent());

        assertThat(sesiune.isInvalid()).isTrue();
    }

    @Test
    void deconectare_faraCsrf_403() throws Exception {
        mvc.perform(post("/api/v1/auth/deconectare")).andExpect(status().isForbidden());
    }

    @Test
    void schimbaParola_anonim_401() throws Exception {
        mvc.perform(post("/api/v1/auth/schimba-parola").with(XsrfTestSuport.xsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parolaCurenta\":\"a\",\"parolaNoua\":\"b\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(cont);
    }

    @Test
    void schimbaParola_autentificat_pastreazaSesiuneaCurenta_siRotesteId() throws Exception {
        utilizatorExistent("ana@ex.ro", true);
        MockHttpSession sesiune = login("ana@ex.ro");
        String idInainte = sesiune.getId();

        mvc.perform(post("/api/v1/auth/schimba-parola").session(sesiune).with(XsrfTestSuport.xsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parolaCurenta\":\"" + PAROLA + "\",\"parolaNoua\":\"minge rosie pe stadion\"}"))
                .andExpect(status().isNoContent());

        verify(cont).schimbaParola(7L, PAROLA, "minge rosie pe stadion", idInainte);
        assertThat(sesiune.getId()).isNotEqualTo(idInainte);
    }

    @Test
    void inregistrare_validareEsuata_400CuErori_faraValoriInRaspuns() throws Exception {
        String raspuns = mvc.perform(post("/api/v1/auth/inregistrare").with(XsrfTestSuport.xsrf()).with(dinIp())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"josé@ex.ro\",\"parola\":\"parolaSecretaX\",\"nume\":\"<script>\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erori.email").exists())
                .andExpect(jsonPath("$.erori.nume").exists())
                .andReturn().getResponse().getContentAsString();

        assertThat(raspuns).doesNotContain("parolaSecretaX").doesNotContain("<script>");
        verifyNoInteractions(cont);
    }

    @Test
    void inregistrare_valida_202FaraCorp() throws Exception {
        mvc.perform(post("/api/v1/auth/inregistrare").with(XsrfTestSuport.xsrf()).with(dinIp())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@ex.ro\",\"parola\":\"" + PAROLA + "\",\"nume\":\"Ana-Maria O'Neil\"}"))
                .andExpect(status().isAccepted())
                .andExpect(content().string(""));
        verify(cont).inregistreaza("ana@ex.ro", PAROLA, "Ana-Maria O'Neil");
    }

    @Test
    void inregistrare_concurentaPeAcelasiEmail_tot202() throws Exception {
        doThrow(new DataIntegrityViolationException("dublura")).when(cont).inregistreaza(anyString(), anyString(), anyString());

        mvc.perform(post("/api/v1/auth/inregistrare").with(XsrfTestSuport.xsrf()).with(dinIp())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@ex.ro\",\"parola\":\"" + PAROLA + "\",\"nume\":\"Ana\"}"))
                .andExpect(status().isAccepted());
    }

    @Test
    void inregistrare_parolaSlaba_400CuCod() throws Exception {
        doThrow(new ParolaSlabaException("Parola e prea des folosită. Alege alta."))
                .when(cont).inregistreaza(anyString(), anyString(), anyString());

        mvc.perform(post("/api/v1/auth/inregistrare").with(XsrfTestSuport.xsrf()).with(dinIp())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@ex.ro\",\"parola\":\"x\",\"nume\":\"Ana\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.cod").value("parola-slaba"));
    }

    @Test
    void parolaUitata_mereu202() throws Exception {
        mvc.perform(post("/api/v1/auth/parola-uitata").with(XsrfTestSuport.xsrf()).with(dinIp())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"oricine@ex.ro\"}"))
                .andExpect(status().isAccepted())
                .andExpect(content().string(""));
        verify(cont).parolaUitata("oricine@ex.ro");
    }

    @Test
    void parolaUitata_pesteLimitaPeIp_429() throws Exception {
        for (int i = 0; i < 5; i++) {
            mvc.perform(parolaUitata()).andExpect(status().isAccepted());
        }
        mvc.perform(parolaUitata()).andExpect(status().isTooManyRequests());
    }

    @Test
    void confirmareEmail_tokenInvalid_400CuCod() throws Exception {
        doThrow(new TokenInvalidException()).when(cont).confirmaEmail(anyString(), anyString());

        mvc.perform(post("/api/v1/auth/confirmare-email").with(XsrfTestSuport.xsrf()).with(dinIp())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"abc\",\"parola\":\"" + PAROLA + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.cod").value("token-invalid"));
    }

    @Test
    void resetareParola_valida_204() throws Exception {
        mvc.perform(post("/api/v1/auth/resetare-parola").with(XsrfTestSuport.xsrf()).with(dinIp())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"abc\",\"parolaNoua\":\"" + PAROLA + "\"}"))
                .andExpect(status().isNoContent());
        verify(cont).reseteazaParola("abc", PAROLA);
    }

    private MockHttpSession login(String email) throws Exception {
        MvcResult r = mvc.perform(autentificare(email, PAROLA)).andExpect(status().isOk()).andReturn();
        return (MockHttpSession) r.getRequest().getSession(false);
    }

    private MockHttpServletRequestBuilder autentificare(String email, String parola) {
        return post("/api/v1/auth/autentificare").with(XsrfTestSuport.xsrf()).with(dinIp())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(email, parola));
    }

    private MockHttpServletRequestBuilder parolaUitata() {
        return post("/api/v1/auth/parola-uitata").with(XsrfTestSuport.xsrf()).with(dinIp())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"oricine@ex.ro\"}");
    }

    private RequestPostProcessor dinIp() {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }

    private void utilizatorExistent(String email, boolean confirmat) {
        var u = new Utilizator();
        u.setId(7L);
        u.setEmail(email);
        u.setNume("Ana");
        u.setParolaHash(encoder.encode(PAROLA));
        u.setEmailConfirmatLa(confirmat ? Instant.EPOCH : null);
        when(utilizatori.findByEmail(email)).thenReturn(Optional.of(u));
        when(utilizatori.findById(7L)).thenReturn(Optional.of(u));
    }

    private static String json(String email, String parola) {
        return "{\"email\":\"" + email + "\",\"parola\":\"" + parola + "\"}";
    }

    private static String faraTimestamp(String json) {
        return json.replaceAll("\"timestamp\":\"[^\"]*\"", "");
    }
}
