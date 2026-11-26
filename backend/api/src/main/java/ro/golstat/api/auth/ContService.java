package ro.golstat.api.auth;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.Nullable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.golstat.api.auth.GeneratorToken.TokenNou;
import ro.golstat.api.auth.email.EmailDeTrimis;
import ro.golstat.api.auth.email.EmailDeTrimis.TipEmail;
import ro.golstat.api.entity.TokenUtilizator;
import ro.golstat.api.entity.TokenUtilizator.Tip;
import ro.golstat.api.entity.Utilizator;
import ro.golstat.api.repository.TokenUtilizatorRepository;
import ro.golstat.api.repository.UtilizatorRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Ciclul de viata al contului. Inregistrarea spune explicit ca emailul e deja folosit (decizie de produs,
 * limitata de rata per IP); parola uitata si retrimiterea raspund identic in ambele cazuri.
 */
@Service
public class ContService {

    static final Duration VALABILITATE_CONFIRMARE = Duration.ofHours(24);
    static final Duration VALABILITATE_RESETARE = Duration.ofMinutes(30);

    private final UtilizatorRepository utilizatori;
    private final TokenUtilizatorRepository tokenuri;
    private final PasswordEncoder encoder;
    private final PoliticaParola politica;
    private final GeneratorToken generator;
    private final SesiuniUtilizator sesiuni;
    private final LimitariAuth limitari;
    private final ApplicationEventPublisher evenimente;
    private final AuditAuth audit;
    private final Clock clock;

    public ContService(UtilizatorRepository utilizatori, TokenUtilizatorRepository tokenuri, PasswordEncoder encoder,
                       PoliticaParola politica, GeneratorToken generator, SesiuniUtilizator sesiuni,
                       LimitariAuth limitari, ApplicationEventPublisher evenimente, AuditAuth audit, Clock clock) {
        this.utilizatori = utilizatori;
        this.tokenuri = tokenuri;
        this.encoder = encoder;
        this.politica = politica;
        this.generator = generator;
        this.sesiuni = sesiuni;
        this.limitari = limitari;
        this.evenimente = evenimente;
        this.audit = audit;
        this.clock = clock;
    }

    /** Un email existent nu se suprascrie si nu primeste niciun email: {@link EmailExistentException}. */
    @Transactional
    public void inregistreaza(String email, String parola, String nume) {
        String e = NormalizatorEmail.normalizeaza(email);
        String n = nume.trim();
        var existent = utilizatori.findByEmail(e);
        if (existent.isPresent()) {
            audit.eveniment("inregistrare-email-existent", existent.get().getId());
            throw new EmailExistentException();
        }
        politica.verifica(parola, e, n);

        var u = new Utilizator();
        u.setEmail(e);
        u.setNume(n);
        u.setParolaHash(encoder.encode(parola));
        u.setCreatLa(clock.instant());
        utilizatori.saveAndFlush(u);
        audit.eveniment("inregistrare", u.getId());
        permiteEmail(u);
        trimiteCuToken(u, Tip.CONFIRMARE_EMAIL, TipEmail.CONFIRMARE);
    }

    /**
     * Cere si parola contului: altfel un atacator care a inregistrat adresa victimei cu parola lui
     * ar obtine un cont confirmat cand victima apasa linkul. Victima care nu stie parola foloseste
     * „parola uitata”, care inlocuieste parola atacatorului.
     *
     * <p>Fiindca linkul singur nu ajunge, toate linkurile de confirmare trimise raman valide pana la
     * expirare (utilizatorul poate apasa oricare email din conversatie); la confirmare se anuleaza toate.
     */
    @Transactional
    public void confirmaEmail(String tokenBrut, String parola) {
        Utilizator u = utilizatorCuTokenValid(tokenBrut, Tip.CONFIRMARE_EMAIL);
        if (!encoder.matches(parola, u.getParolaHash())) {
            audit.eveniment("confirmare-parola-gresita", u.getId());
            throw new ParolaCurentaGresitaException();
        }
        u = consumaToken(tokenBrut, Tip.CONFIRMARE_EMAIL);
        if (!u.isEmailConfirmat()) {
            u.setEmailConfirmatLa(clock.instant());
        }
        tokenuri.invalideazaActive(u.getId(), Tip.CONFIRMARE_EMAIL, clock.instant());
        audit.eveniment("email-confirmat", u.getId());
    }

    @Transactional
    public void retrimiteConfirmare(String email) {
        String e = NormalizatorEmail.normalizeaza(email);
        utilizatori.findByEmail(e)
                .filter(u -> !u.isEmailConfirmat())
                .filter(this::permiteEmail)
                .ifPresent(u -> trimiteCuToken(u, Tip.CONFIRMARE_EMAIL, TipEmail.CONFIRMARE));
    }

    @Transactional
    public void parolaUitata(String email) {
        String e = NormalizatorEmail.normalizeaza(email);
        utilizatori.findByEmail(e)
                .filter(this::permiteEmail)
                .ifPresent(u -> {
                    tokenuri.invalideazaActive(u.getId(), Tip.RESETARE_PAROLA, clock.instant());
                    trimiteCuToken(u, Tip.RESETARE_PAROLA, TipEmail.RESETARE);
                    audit.eveniment("parola-uitata", u.getId());
                });
    }

    /**
     * Tokenul se valideaza inainte de politica de parola, dar se consuma abia dupa: o parola slaba
     * nu arde linkul. Dupa resetare, toate sesiunile sunt deconectate.
     */
    @Transactional
    public void reseteazaParola(String tokenBrut, String parolaNoua) {
        Utilizator u = utilizatorCuTokenValid(tokenBrut, Tip.RESETARE_PAROLA);
        politica.verifica(parolaNoua, u.getEmail(), u.getNume());

        u = consumaToken(tokenBrut, Tip.RESETARE_PAROLA);
        Instant acum = clock.instant();
        if (!u.isEmailConfirmat()) {
            u.setEmailConfirmatLa(acum);
        }
        seteazaParola(u, parolaNoua, null);
        tokenuri.invalideazaActive(u.getId(), Tip.CONFIRMARE_EMAIL, acum);
        audit.eveniment("parola-resetata", u.getId());
    }

    /** Pastreaza sesiunea curenta (id-ul ei se roteste in stratul HTTP) si le deconecteaza pe celelalte. */
    @Transactional
    public void schimbaParola(long utilizatorId, String parolaCurenta, String parolaNoua, @Nullable String sesiuneCurenta) {
        limitari.schimbarePeUtilizator().consuma(String.valueOf(utilizatorId));
        Utilizator u = utilizatori.findById(utilizatorId).orElseThrow(ParolaCurentaGresitaException::new);
        if (!encoder.matches(parolaCurenta, u.getParolaHash())) {
            audit.eveniment("schimbare-parola-esuata", utilizatorId);
            throw new ParolaCurentaGresitaException();
        }
        politica.verifica(parolaNoua, u.getEmail(), u.getNume());
        seteazaParola(u, parolaNoua, sesiuneCurenta);
        audit.eveniment("parola-schimbata", utilizatorId);
    }

    private void seteazaParola(Utilizator u, String parolaNoua, @Nullable String sesiunePastrata) {
        u.setParolaHash(encoder.encode(parolaNoua));
        u.setParolaSchimbataLa(clock.instant());
        utilizatori.saveAndFlush(u);
        tokenuri.invalideazaActive(u.getId(), Tip.RESETARE_PAROLA, clock.instant());
        sesiuni.invalideazaToate(u.getId(), sesiunePastrata);
        evenimente.publishEvent(new EmailDeTrimis(u.getEmail(), TipEmail.PAROLA_SCHIMBATA, null));
    }

    /** Limita pe cont (nu pe textul introdus), ca variante ale aceleiasi adrese sa nu ocoleasca limita. */
    private boolean permiteEmail(Utilizator u) {
        return limitari.emailuriPeAdresa().incearca("u:" + u.getId());
    }

    private Utilizator utilizatorCuTokenValid(String tokenBrut, Tip tip) {
        Instant acum = clock.instant();
        return tokenuri.findByHashAndTip(GeneratorToken.hash(tokenBrut), tip)
                .filter(t -> t.getFolositLa() == null && t.getExpiraLa().isAfter(acum))
                .flatMap(t -> utilizatori.findById(t.getUtilizatorId()))
                .orElseThrow(TokenInvalidException::new);
    }

    private Utilizator consumaToken(String tokenBrut, Tip tip) {
        String hash = GeneratorToken.hash(tokenBrut);
        if (tokenuri.consuma(hash, tip, clock.instant()) != 1) {
            throw new TokenInvalidException();
        }
        return tokenuri.findByHashAndTip(hash, tip)
                .flatMap(t -> utilizatori.findById(t.getUtilizatorId()))
                .orElseThrow(TokenInvalidException::new);
    }

    private void trimiteCuToken(Utilizator u, Tip tip, TipEmail tipEmail) {
        TokenNou nou = generator.genereaza();
        Instant acum = clock.instant();
        var token = new TokenUtilizator();
        token.setUtilizatorId(u.getId());
        token.setTip(tip);
        token.setHash(nou.hash());
        token.setCreatLa(acum);
        token.setExpiraLa(acum.plus(tip == Tip.CONFIRMARE_EMAIL ? VALABILITATE_CONFIRMARE : VALABILITATE_RESETARE));
        tokenuri.save(token);
        evenimente.publishEvent(new EmailDeTrimis(u.getEmail(), tipEmail, nou.brut()));
    }
}
