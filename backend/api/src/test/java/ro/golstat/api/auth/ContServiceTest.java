package ro.golstat.api.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import ro.golstat.api.auth.email.EmailDeTrimis;
import ro.golstat.api.auth.email.EmailDeTrimis.TipEmail;
import ro.golstat.api.entity.TokenUtilizator;
import ro.golstat.api.entity.TokenUtilizator.Tip;
import ro.golstat.api.entity.Utilizator;
import ro.golstat.api.repository.TokenUtilizatorRepository;
import ro.golstat.api.repository.UtilizatorRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ContServiceTest {

    private static final Instant ACUM = Instant.parse("2026-11-26T10:00:00Z");
    private static final String PAROLA = "cal verde sare gardul";
    private static final String PAROLA_NOUA = "minge rosie pe stadion";

    @Mock UtilizatorRepository utilizatori;
    @Mock TokenUtilizatorRepository tokenuri;
    @Mock SesiuniUtilizator sesiuni;
    @Mock ApplicationEventPublisher evenimente;
    @Mock AuditAuth audit;

    private final Clock clock = Clock.fixed(ACUM, ZoneOffset.UTC);
    private final PasswordEncoder encoder = new ParoleConfig().passwordEncoder();
    private ContService cont;

    @BeforeEach
    void setUp() {
        cont = new ContService(utilizatori, tokenuri, encoder, new PoliticaParola(), new GeneratorToken(), sesiuni,
                new LimitariAuth(clock), evenimente, audit, clock);
        when(utilizatori.saveAndFlush(any())).thenAnswer(inv -> {
            Utilizator u = inv.getArgument(0);
            if (u.getId() == null) {
                u.setId(1L);
            }
            return u;
        });
    }

    @Test
    void inregistreaza_emailNou_salveazaHashArgon2_siPublicaConfirmare() {
        when(utilizatori.findByEmail("ana@ex.ro")).thenReturn(Optional.empty());

        cont.inregistreaza("  Ana@Ex.ro ", PAROLA, " Ana ");

        var salvat = ArgumentCaptor.forClass(Utilizator.class);
        verify(utilizatori).saveAndFlush(salvat.capture());
        assertThat(salvat.getValue().getEmail()).isEqualTo("ana@ex.ro");
        assertThat(salvat.getValue().getNume()).isEqualTo("Ana");
        assertThat(salvat.getValue().getParolaHash()).startsWith("{argon2}");
        assertThat(salvat.getValue().isEmailConfirmat()).isFalse();

        var token = ArgumentCaptor.forClass(TokenUtilizator.class);
        verify(tokenuri).save(token.capture());
        assertThat(token.getValue().getTip()).isEqualTo(Tip.CONFIRMARE_EMAIL);
        assertThat(token.getValue().getExpiraLa()).isEqualTo(ACUM.plus(ContService.VALABILITATE_CONFIRMARE));

        EmailDeTrimis email = emailPublicat();
        assertThat(email.catre()).isEqualTo("ana@ex.ro");
        assertThat(email.tip()).isEqualTo(TipEmail.CONFIRMARE);
        assertThat(GeneratorToken.hash(email.tokenBrut())).isEqualTo(token.getValue().getHash());
    }

    @Test
    void inregistreaza_emailExistent_nuSuprascrie_publicaContExistent() {
        Utilizator existent = utilizator(true);
        String hashVechi = existent.getParolaHash();
        when(utilizatori.findByEmail("ana@ex.ro")).thenReturn(Optional.of(existent));

        cont.inregistreaza("ana@ex.ro", PAROLA, "Atacator");

        verify(utilizatori, never()).saveAndFlush(any());
        assertThat(existent.getParolaHash()).isEqualTo(hashVechi);
        assertThat(existent.getNume()).isEqualTo("Ana");
        assertThat(emailPublicat().tip()).isEqualTo(TipEmail.CONT_EXISTENT);
        var token = ArgumentCaptor.forClass(TokenUtilizator.class);
        verify(tokenuri).save(token.capture());
        assertThat(token.getValue().getTip()).isEqualTo(Tip.RESETARE_PAROLA);
    }

    @Test
    void inregistreaza_emailNeconfirmatExistent_nuAnuleazaLinkurileDeConfirmare() {
        when(utilizatori.findByEmail("ana@ex.ro")).thenReturn(Optional.of(utilizator(false)));

        cont.inregistreaza("ana@ex.ro", PAROLA, "Ana");

        verify(tokenuri, never()).invalideazaActive(7L, Tip.CONFIRMARE_EMAIL, ACUM);
    }

    @Test
    void inregistreaza_parolaSlaba_respinsaInainteDeOriceScriere() {
        assertThatThrownBy(() -> cont.inregistreaza("ana@ex.ro", "scurta", "Ana"))
                .isInstanceOf(ParolaSlabaException.class);

        verifyNoInteractions(utilizatori, tokenuri, evenimente);
    }

    @Test
    void inregistreaza_pesteLimitaPeEmail_nuMaiTrimite() {
        when(utilizatori.findByEmail("ana@ex.ro")).thenReturn(Optional.of(utilizator(true)));

        for (int i = 0; i < 5; i++) {
            cont.inregistreaza("ana@ex.ro", PAROLA, "Ana");
        }

        verify(evenimente, times(3)).publishEvent(any(EmailDeTrimis.class));
    }

    @Test
    void confirmaEmail_tokenValid_confirma() {
        Utilizator u = utilizator(false);
        tokenValid("brut", Tip.CONFIRMARE_EMAIL, u);

        cont.confirmaEmail("brut", PAROLA);

        assertThat(u.getEmailConfirmatLa()).isEqualTo(ACUM);
        verify(tokenuri).invalideazaActive(7L, Tip.CONFIRMARE_EMAIL, ACUM);
    }

    @Test
    void confirmaEmail_parolaGresita_preHijack_nuConfirma_nuConsuma() {
        Utilizator u = utilizator(false);
        tokenValid("brut", Tip.CONFIRMARE_EMAIL, u);

        assertThatThrownBy(() -> cont.confirmaEmail("brut", "nu stiu parola atacatorului"))
                .isInstanceOf(ParolaCurentaGresitaException.class);

        assertThat(u.isEmailConfirmat()).isFalse();
        verify(tokenuri, never()).consuma(anyString(), any(), any());
    }

    @Test
    void inregistreaza_emailExistent_invalideazaSiResetarileVechi() {
        when(utilizatori.findByEmail("ana@ex.ro")).thenReturn(Optional.of(utilizator(true)));

        cont.inregistreaza("ana@ex.ro", PAROLA, "Ana");

        verify(tokenuri).invalideazaActive(7L, Tip.RESETARE_PAROLA, ACUM);
    }

    @Test
    void schimbaParola_invalideazaLinkurileDeResetareActive() {
        when(utilizatori.findById(7L)).thenReturn(Optional.of(utilizator(true)));

        cont.schimbaParola(7L, PAROLA, PAROLA_NOUA, "s1");

        verify(tokenuri).invalideazaActive(7L, Tip.RESETARE_PAROLA, ACUM);
    }

    @Test
    void confirmaEmail_tokenExpiratSauFolosit_TokenInvalid() {
        when(tokenuri.consuma(GeneratorToken.hash("brut"), Tip.CONFIRMARE_EMAIL, ACUM)).thenReturn(0);

        assertThatThrownBy(() -> cont.confirmaEmail("brut", PAROLA)).isInstanceOf(TokenInvalidException.class);
    }

    @Test
    void retrimiteConfirmare_contConfirmat_nuTrimite() {
        when(utilizatori.findByEmail("ana@ex.ro")).thenReturn(Optional.of(utilizator(true)));

        cont.retrimiteConfirmare("ana@ex.ro");

        verifyNoInteractions(evenimente);
    }

    /** Gmail grupeaza emailurile identice: utilizatorul apasa adesea linkul din primul email. */
    @Test
    void retrimiteConfirmare_linkulVechiRamaneValid_siSeTrimiteUnulNou() {
        Utilizator u = utilizator(false);
        when(utilizatori.findByEmail("ana@ex.ro")).thenReturn(Optional.of(u));

        cont.retrimiteConfirmare("ana@ex.ro");
        assertThat(emailPublicat().tip()).isEqualTo(TipEmail.CONFIRMARE);
        verify(tokenuri, never()).invalideazaActive(7L, Tip.CONFIRMARE_EMAIL, ACUM);

        tokenValid("primul-link", Tip.CONFIRMARE_EMAIL, u);
        cont.confirmaEmail("primul-link", PAROLA);

        assertThat(u.isEmailConfirmat()).isTrue();
    }

    @Test
    void parolaUitata_emailInexistent_nuPublicaNimic_faraExceptie() {
        when(utilizatori.findByEmail("nimeni@ex.ro")).thenReturn(Optional.empty());

        cont.parolaUitata("nimeni@ex.ro");

        verifyNoInteractions(evenimente);
        verify(tokenuri, never()).save(any());
    }

    @Test
    void parolaUitata_existent_tokenDe30Minute() {
        when(utilizatori.findByEmail("ana@ex.ro")).thenReturn(Optional.of(utilizator(true)));

        cont.parolaUitata("ana@ex.ro");

        var token = ArgumentCaptor.forClass(TokenUtilizator.class);
        verify(tokenuri).save(token.capture());
        assertThat(token.getValue().getExpiraLa()).isEqualTo(ACUM.plus(ContService.VALABILITATE_RESETARE));
        assertThat(emailPublicat().tip()).isEqualTo(TipEmail.RESETARE);
    }

    @Test
    void parolaUitata_pesteLimitaPeEmail_nuTrimite() {
        when(utilizatori.findByEmail("ana@ex.ro")).thenReturn(Optional.of(utilizator(true)));

        for (int i = 0; i < 4; i++) {
            cont.parolaUitata("ana@ex.ro");
        }

        verify(evenimente, times(3)).publishEvent(any(EmailDeTrimis.class));
    }

    @Test
    void reseteazaParola_invalideazaSesiunile_siTokenurile_siConfirmaEmailul() {
        Utilizator u = utilizator(false);
        tokenValid("brut", Tip.RESETARE_PAROLA, u);

        cont.reseteazaParola("brut", PAROLA_NOUA);

        assertThat(encoder.matches(PAROLA_NOUA, u.getParolaHash())).isTrue();
        assertThat(u.getParolaSchimbataLa()).isEqualTo(ACUM);
        assertThat(u.getEmailConfirmatLa()).isEqualTo(ACUM);
        verify(sesiuni).invalideazaToate(7L, null);
        verify(tokenuri).invalideazaActive(7L, Tip.RESETARE_PAROLA, ACUM);
        verify(tokenuri).invalideazaActive(7L, Tip.CONFIRMARE_EMAIL, ACUM);
        assertThat(emailPublicat().tip()).isEqualTo(TipEmail.PAROLA_SCHIMBATA);
    }

    @Test
    void reseteazaParola_parolaSlaba_nuConsumaTokenul() {
        tokenValid("brut", Tip.RESETARE_PAROLA, utilizator(true));

        assertThatThrownBy(() -> cont.reseteazaParola("brut", "scurta")).isInstanceOf(ParolaSlabaException.class);

        verify(tokenuri, never()).consuma(anyString(), any(), any());
    }

    @Test
    void reseteazaParola_tokenExpirat_TokenInvalid() {
        Utilizator u = utilizator(true);
        tokenValid("brut", Tip.RESETARE_PAROLA, u).setExpiraLa(ACUM.minusSeconds(1));

        assertThatThrownBy(() -> cont.reseteazaParola("brut", PAROLA_NOUA)).isInstanceOf(TokenInvalidException.class);
        verify(sesiuni, never()).invalideazaToate(anyLong(), any());
    }

    @Test
    void reseteazaParola_folositInParalel_TokenInvalid() {
        tokenValid("brut", Tip.RESETARE_PAROLA, utilizator(true));
        when(tokenuri.consuma(GeneratorToken.hash("brut"), Tip.RESETARE_PAROLA, ACUM)).thenReturn(0);

        assertThatThrownBy(() -> cont.reseteazaParola("brut", PAROLA_NOUA)).isInstanceOf(TokenInvalidException.class);
        verify(sesiuni, never()).invalideazaToate(anyLong(), any());
    }

    @Test
    void schimbaParola_parolaCurentaGresita_exceptie_nimicSchimbat() {
        Utilizator u = utilizator(true);
        String hashVechi = u.getParolaHash();
        when(utilizatori.findById(7L)).thenReturn(Optional.of(u));

        assertThatThrownBy(() -> cont.schimbaParola(7L, "gresita gresita", PAROLA_NOUA, "s1"))
                .isInstanceOf(ParolaCurentaGresitaException.class);
        assertThat(u.getParolaHash()).isEqualTo(hashVechi);
        verifyNoInteractions(sesiuni);
    }

    @Test
    void schimbaParola_corect_pastreazaSesiuneaCurenta() {
        Utilizator u = utilizator(true);
        when(utilizatori.findById(7L)).thenReturn(Optional.of(u));

        cont.schimbaParola(7L, PAROLA, PAROLA_NOUA, "s1");

        assertThat(encoder.matches(PAROLA_NOUA, u.getParolaHash())).isTrue();
        verify(sesiuni).invalideazaToate(7L, "s1");
        assertThat(emailPublicat().tip()).isEqualTo(TipEmail.PAROLA_SCHIMBATA);
    }

    @Test
    void schimbaParola_pesteLimita_429() {
        Utilizator u = utilizator(true);
        when(utilizatori.findById(7L)).thenReturn(Optional.of(u));
        for (int i = 0; i < 10; i++) {
            try {
                cont.schimbaParola(7L, "gresita gresita", PAROLA_NOUA, "s1");
            } catch (ParolaCurentaGresitaException ignored) {
                // asteptat
            }
        }

        assertThatThrownBy(() -> cont.schimbaParola(7L, PAROLA, PAROLA_NOUA, "s1"))
                .isInstanceOf(PreaMulteIncercariException.class);
    }

    private Utilizator utilizator(boolean confirmat) {
        var u = new Utilizator();
        u.setId(7L);
        u.setEmail("ana@ex.ro");
        u.setNume("Ana");
        u.setParolaHash(encoder.encode(PAROLA));
        u.setCreatLa(ACUM.minusSeconds(3600));
        u.setEmailConfirmatLa(confirmat ? ACUM.minusSeconds(1800) : null);
        return u;
    }

    private TokenUtilizator tokenValid(String brut, Tip tip, Utilizator u) {
        var t = new TokenUtilizator();
        t.setUtilizatorId(u.getId());
        t.setTip(tip);
        t.setHash(GeneratorToken.hash(brut));
        t.setExpiraLa(ACUM.plusSeconds(600));
        when(tokenuri.findByHashAndTip(GeneratorToken.hash(brut), tip)).thenReturn(Optional.of(t));
        when(tokenuri.consuma(eq(GeneratorToken.hash(brut)), eq(tip), eq(ACUM))).thenReturn(1);
        when(utilizatori.findById(u.getId())).thenReturn(Optional.of(u));
        return t;
    }

    private EmailDeTrimis emailPublicat() {
        var captor = ArgumentCaptor.forClass(EmailDeTrimis.class);
        verify(evenimente).publishEvent(captor.capture());
        return captor.getValue();
    }
}
