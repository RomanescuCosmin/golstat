package ro.golstat.api.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ro.golstat.api.repository.TokenUtilizatorRepository;
import ro.golstat.api.repository.UtilizatorRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/** Sterge conturile neconfirmate vechi (adresa ramane libera daca cineva a pre-inregistrat-o) si tokenurile expirate. */
@Component
class CuratareConturiJob {

    private static final Logger log = LoggerFactory.getLogger(CuratareConturiJob.class);
    static final Duration PASTRARE = Duration.ofDays(7);

    private final UtilizatorRepository utilizatori;
    private final TokenUtilizatorRepository tokenuri;
    private final Clock clock;

    CuratareConturiJob(UtilizatorRepository utilizatori, TokenUtilizatorRepository tokenuri, Clock clock) {
        this.utilizatori = utilizatori;
        this.tokenuri = tokenuri;
        this.clock = clock;
    }

    @Scheduled(cron = "0 30 3 * * *", zone = "UTC")
    @Transactional
    public void curata() {
        Instant prag = clock.instant().minus(PASTRARE);
        int tokenuriConturi = tokenuri.stergeAleConturilorNeconfirmateInainteDe(prag);
        int conturi = utilizatori.stergeNeconfirmateInainteDe(prag);
        int expirate = tokenuri.stergeExpirateInainteDe(prag);
        log.info("Curatare conturi: {} conturi neconfirmate, {} tokenuri", conturi, tokenuriConturi + expirate);
    }
}
